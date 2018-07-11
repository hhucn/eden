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
  [request-url]
  (try
    (:data (:body (client/get request-url {:as :json})))
    (catch Exception e
      {})))

(defn get-payload
  "Helper to get the payload from a remote query."
  [request-url]
  (:payload (get-data request-url)))

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
  [{:keys [aggregate-id entity-id]}]
  (db/get-undercuts aggregate-id entity-id))

(defn links-by-target
  "Retrieve all local links in db pointing to the target statement."
  [target]
  (db/links-by-target (:aggregate-id target)
                      (:entity-id target)
                      (:version target)))

(defn retrieve-remote
  "Try to retrieve a statement from a remote aggregator. The host part is treated as the webhost."
  [uri]
  (let [split-uri (str/split uri #"/")
        aggregate (first split-uri)
        request-url (str config/protocol aggregate "/statements/" uri)
        result-data (get-data request-url)
        results (:payload result-data)]
    (subscribe-to-queue "statements" aggregate)
    (subscribe-to-queue "links" aggregate)
    (doseq [statement results] (up/update-statement statement))
    results))

(defn retrieve-exact-statement
  "Retrieves an exact statement from cache / db / a remote aggregator."
  [aggregate entity version]
  (if-let [local-statement (exact-statement aggregate entity version)]
    local-statement
    (let [request-url (str config/protocol aggregate "/statement/" aggregate "/" entity "/" version)
          result-data (get-data request-url)
          result (:payload result-data)]
      (up/update-statement result)
      result)))

(defn remote-link
  "Retrieves a remote link from its aggregator"
  [aggregate entity-id]
  (let [request-url (str config/protocol aggregate "/link/" aggregate "/" entity-id)
        result (get-payload request-url)]
    (up/update-link result)
    result))

(defn retrieve-undercuts
  "Retrieve a (possibly remote) list of undercuts. The argument is the link being undercut."
  [link]
  (if-let [possible-undercuts (local-undercuts link)]
    possible-undercuts
    (let [aggregate (:aggregate-id link)
          entity-id (:entity-id link)
          request-url (str config/protocol aggregate "/link/undercuts/" aggregate "/" entity-id)
          results (get-payload request-url)]
      (doseq [link results] (up/update-link link))
      results)))

(defn links-to
  "Retrieve all links pointing to provided statement. (From the statements aggregator)"
  [statement]
  (if-let [possible-links (links-by-target statement)]
    possible-links
    (let [aggregate (:aggregate-id statement)
          entity-id (:entity-id statement)
          version (:version statement)
          request-url (str config/protocol aggregate "/link/to/" aggregate "/" entity-id "/" version)
          results (get-payload request-url)]
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
         :not-found
         (retrieve-remote uri))
       (cache/cache-miss uri possible-entity)))))

(defn tiered-retrieval
  "Check whether the Cache contains the desired entity. If not delegate to DB and remote acquisition."
  ([uri]
   (tiered-retrieval uri {}))
  ([uri options]
   (let [cached-entity (cache/retrieve uri)]
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
          :not-found
          (do (cache/cache-miss-link uri db-result)
            db-result)))
      cached-link)))


(defn starter-set
  "Retrieve a set of starting arguments, which can be used by remote aggregators to bootstrap the connection. This particular implementation just takes a random set of arguments from the cache or databse."
  []
  (db/random-statements 10))

(defn remote-starter-set
  "Retrieve remote starter sets and put them into the cache and db."
  ([]
   (doseq [host config/whitelist] (remote-starter-set host)))
  ([aggregator]
   (let [results (get-payload (str config/protocol aggregator "/statements/starter-set"))]
     (when (and results (not= results "not-found"))
       (doseq [stmt results] (up/update-statement stmt))))))

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
   (let [results (get-payload (str config/protocol aggregator "/statements"))]
     (when (not= results "not-found")
       (doseq [statement results]
         (up/update-statement statement))))))

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
   (let [results (get-payload (str config/protocol aggregator "/links"))]
     (when (not= results "not-found")
       (doseq [link results]
         (up/update-link link))))))
