(ns aggregator.query.query
  (:require [aggregator.query.cache :as cache]
            [aggregator.query.db :as db]
            [aggregator.query.update :as up]
            [aggregator.query.utils :as utils]
            [aggregator.broker.subscriber :as sub]
            [aggregator.config :as config]
            [clj-http.client :as client]
            [clojure.string :as str]
            [taoensso.timbre :as log]))

(defn get-data
  "Get data from a remote aggregator."
  ([request-url]
   (get-data request-url {}))
  ([request-url query-params]
   (try
     (:body (client/get request-url {:as :json
                                     :query-params query-params}))
     (catch Exception e
       {}))))

(defn- subscribe-to-queue
  "Uses the broker module to subscribe to a queue for updates. Sanitizes the host
  if a port is appended. Example: example.com:8888 is treated as example.com."
  [queue host]
  (let [queue-name (get-in queue [:data :queue-name])
        cleaned-host (first (str/split host #":"))]
    (sub/subscribe queue-name {:host cleaned-host})))

(defn exact-statement
  "Return the exact statement from cache or db"
  [aggregate-id entity-id version]
  (let [cached-statement (cache/retrieve (utils/build-cache-pattern aggregate-id entity-id version))]
    (if (and (not= cached-statement :missing)
             (= (get-in cached-statement [:identifier :version]) version))
      cached-statement
      (if-let [maybe-statement (db/exact-statement aggregate-id entity-id version)]
        (do (cache/cache-miss (utils/build-cache-pattern maybe-statement) maybe-statement)
            (log/debug "[query] Found exact statement in DB")
            maybe-statement)))))

(defn local-undercuts
  "Retrieve all links from the db that undercut the link passed as argument."
  [aggregate-id entity-id]
  (db/get-undercuts aggregate-id entity-id))

(defn links-by-target
  "Retrieve all local links in db pointing to the target statement."
  [aggregate-id entity-id version]
  (db/links-by-target aggregate-id entity-id version))

(defn retrieve-remote
  "Try to retrieve a statement from a remote aggregator. The host part is treated as the webhost."
  ([uri]
   (let [split-uri (str/split uri #"/")]
     (retrieve-remote (first split-uri) (second split-uri))))
  ([aggregate-id entity-id]
   (let [request-url (str config/protocol aggregate-id "/statements/by-id")
         result-data (:statements (get-data request-url {"aggregate-id" aggregate-id
                                                         "entity-id" entity-id}))]
     (subscribe-to-queue "statements" aggregate-id)
     (subscribe-to-queue "links" aggregate-id)
     (doseq [statement result-data] (up/update-statement statement))
     result-data)))

(defn retrieve-exact-statement
  "Retrieves an exact statement from cache / db / a remote aggregator."
  [aggregate entity version]
  (if-let [local-statement (exact-statement aggregate entity version)]
    local-statement
    (let [request-url (str config/protocol aggregate "/statement/" aggregate "/" entity "/" version)
          result-data (get-data request-url)
          result (:statements result-data)]
      (up/update-statement result)
      result)))

(defn remote-link
  "Retrieves a remote link from its aggregator"
  [aggregate entity-id]
  (let [request-url (str config/protocol aggregate "/link/" aggregate "/" entity-id)
        result (get-data request-url)]
    (up/update-link result)
    result))

(defn retrieve-undercuts
  "Retrieve a (possibly remote) list of undercuts. The argument is the link being undercut."
  [aggregate-id entity-id]
  (if-let [possible-undercuts (local-undercuts aggregate-id entity-id)]
    possible-undercuts
    (let [request-url (str config/protocol aggregate-id "/links/undercuts/")
          results (get-data request-url {"aggregate-id" aggregate-id
                                         "entity-id" entity-id})]
      (doseq [link results] (up/update-link link))
      results)))

(defn links-to
  "Retrieve all links pointing to provided statement. (From the statements aggregator)"
  [aggregate-id entity-id version]
  (if-let [possible-links (links-by-target aggregate-id entity-id version)]
    possible-links
    (let [request-url (str config/protocol aggregate-id "/link/to/")
          results (get-data request-url {"aggregate-id" aggregate-id
                                         "entity-id" entity-id
                                         "version" version})]
      (doseq [link results] (up/update-link link))
      results)))

(defn check-db
  "Check the database for an entity and update cache after item is found."
  ([uri]
   (check-db uri {}))
  ([uri {:keys [opts]}]
   (let [possible-entity (db/statements-by-uri uri)]
     (if (= possible-entity :missing)
       (if (some #(= % :no-remote) opts)
         []
         (retrieve-remote uri))
       (cache/cache-miss uri possible-entity)))))

(defn tiered-retrieval
  "Check whether the Cache contains the desired entity. If not delegate to DB and remote acquisition."
  ([aggregate-id entity-id]
   (tiered-retrieval aggregate-id entity-id {}))
  ([aggregate-id entity-id options]
   (let [uri (str aggregate-id "/" entity-id)
         cached-entity (cache/retrieve uri)]
     (if (= cached-entity :missing)
       (check-db uri options)
       cached-entity))))

(defn retrieve-link
  "Retrieve a link from cache or db. Returns :not-found if no such link can be found."
  [uri]
  (let [cached-link (cache/retrieve-link uri)]
    (if (= cached-link :missing)
      (let [db-result (db/links-by-uri uri)]
        (if (= db-result :missing)
          []
          (do (cache/cache-miss-link uri db-result)
            db-result)))
      cached-link)))


(defn starter-set
  "Retrieve a set of starting arguments, which can be used by remote aggregators to bootstrap the connection. This particular implementation just takes a random set of arguments from the cache or database."
  []
  (db/random-statements 10))

(defn remote-starter-set
  "Retrieve remote starter sets and put them into the cache and db."
  ([]
   (doseq [host config/whitelist] (remote-starter-set host)))
  ([aggregator]
   (let [results (:statements (get-data (str config/protocol aggregator "/statements/starter-set")))]
     (doseq [stmt results] (up/update-statement stmt)))))

(defn all-local-statements
  "Retrieve all locally saved statements belonging to the aggregator."
  []
  (db/all-statements))

(defn all-remote-statements
  "Retrieve all statements from a remote aggregator."
  ([]
   (doseq [aggregator config/whitelist]
     (when (not= aggregator config/aggregate-name)
       (all-remote-statements aggregator))))
  ([aggregator]
   (let [results (:statements (get-data (str config/protocol aggregator "/statements")))]
     (doseq [statement results]
       (up/update-statement statement)))))

(defn all-local-links
  "Retrieve all locally saved statements belonging to the aggregator."
  []
  (db/all-links))

(defn all-remote-links
  "Retrieve all links from a remote aggregator."
  ([]
   (doseq [aggregator config/whitelist]
     (when (not= aggregator config/aggregate-name)
       (all-remote-links aggregator))))
  ([aggregator]
   (let [results (get-data (str config/protocol aggregator "/links"))]
     (doseq [link results]
       (up/update-link link)))))
