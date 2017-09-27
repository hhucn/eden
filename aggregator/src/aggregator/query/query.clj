(ns aggregator.query.query
  (:require [aggregator.query.cache :as cache]
            [aggregator.query.db :as db]
            [aggregator.query.utils :as utils]
            [aggregator.query.update :as up]
            [aggregator.broker.subscriber :as sub]
            [aggregator.config :as config]
            [clj-http.client :as client]
            [clojure.string :as str]
            [taoensso.timbre :as log]))


(defn get-data
  "Get data from a remote aggregator."
  [request-url]
  (-> (client/get request-url  {:accept :json})
      (utils/http-response->map)
      :data))

(defn get-payload
  "Helper to get the payload from a remote query."
  [request-url]
  (:payload (get-data request-url)))

(defn subscribe-to-queue
  "Uses the broker module to subscribe to a queue for updates."
  [queue host]
  (let [queue-name (get-in queue [:data :queue-name])]
    (sub/subscribe sub/to-query queue-name {:host host})))

(defn exact-statement
  "Return the exact statement from cache or db"
  [aggregate-id entity-id version]
  (let [cached-statement (cache/retrieve (str aggregate-id "/" entity-id))]
    (if (and (not= cached-statement :missing)
             (= (:version cached-statement) version))
      cached-statement
      (if-let [maybe-statement (db/exact-statement aggregate-id entity-id version)]
        (do (cache/cache-miss (str aggregate-id "/" entity-id) maybe-statement)
            (log/debug "[query] Return statement from db.")
            maybe-statement)))))

(defn local-undercuts
  "Retrieve all links from the db that undercut the link passed as argument."
  [link]
  (let [aggregate (:aggregate-id link)
        entity-id (:entity-id link)]
    (db/get-undercuts aggregate entity-id)))

(defn links-by-target
  "Retrieve all local links in db pointing to the target statement."
  [target]
  (db/links-by-target (:aggregate-id target)
                      (:entity-id target)
                      (:version  target)))

(defn retrieve-remote
  "Try to retrieve a statement from a remote aggregator. The host part is treated as the webhost."
  [uri]
  (let [split-uri (str/split uri #"/")
        aggregate (first split-uri)
        request-url (str "http://" aggregate "/statements/" uri)
        result-data (get-data request-url)
        results (:payload result-data)
        queue (:queue result-data)]
    (subscribe-to-queue queue aggregate)
    (doall (map up/update-statement results))
    results))

(defn retrieve-exact-statement
  "Retrieves an exact statement from cache / db / a remote aggregator."
  [aggregate entity version]
  (if-let [local-statement (exact-statement aggregate entity version)]
    local-statement
    (let [request-url (str "http://" aggregate "/statement/" aggregate "/" entity "/" version)
          result-data (get-data request-url)
          result (:payload result-data)
          queue (:queue result-data)]
      (subscribe-to-queue queue aggregate)
      (up/update-statement result)
      result)))

(defn remote-link
  "Retrieves a remote link from its aggregator"
  [aggregate entity-id]
  (let [request-url (str "http://" aggregate "/link/" aggregate "/" entity-id)
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
          request-url (str "http://" aggregate "/link/undercuts/" aggregate "/" entity-id)
          results (get-payload request-url)]
      (doall (map up/update-link results))
      results)))

(defn links-to
  "Retrieve all links pointing to provided statement. (From the statements aggregator)"
  [statement]
  (if-let [possible-links (links-by-target statement)]
    possible-links
    (let [aggregate (:aggregate-id statement)
          entity-id (:entity-id statement)
          version (:version statement)
          request-url (str "http://" aggregate "/link/to/" aggregate "/" entity-id "/" version)
          results (get-payload request-url)]
      (doall (map up/update-link results))
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
   (doall (map (fn [aggregator] (remote-starter-set aggregator)) config/whitelist)))
  ([aggregator]
   (let [results (get-payload (str aggregator "/statements/starter-set"))]
     (doall (map (fn [statement] (up/update-statement statement)) results)))))
