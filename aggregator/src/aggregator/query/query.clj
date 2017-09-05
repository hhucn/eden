(ns aggregator.query.query
  (:require [aggregator.query.cache :as cache]
            [aggregator.query.db :as db]
            [aggregator.query.utils :as utils]
            [clj-http.client :as client]
            [clojure.string :as str]))


(defn get-payload
  "Helper to get the payload from a remote query."
  [request-url]
  (-> (client/get request-url  {:accept :json})
      (utils/http-response->map)
      :data
      :payload))

(defn retrieve-remote
  "Try to retrieve an argument from a remote aggregator. The host part is treated as the webhost."
  [uri]
  (let [split-uri (str/split uri #"/")
        aggregate (first split-uri)
        request-url (str "http://" aggregate "/statements/" uri)]
    (get-payload request-url)))

(defn retrieve-exact-statement
  "Retrieves an exact statement from a remote aggregator."
  [aggregate entity version]
  (let [request-url (str "http://" aggregate "/statement/" aggregate "/" entity "/" version)]
    (get-payload request-url)))

(defn remote-link
  "Retrieves a remote link from its aggregator"
  [aggregate entity-id]
  (let [request-url (str "http://" aggregate "/link/" aggregate "/" entity-id)]
    (get-payload request-url)))

(defn remote-undercuts
  "Retrieve a remote list of undercuts. The argument is the link being undercut."
  [link]
  (let [aggregate (:aggregate-id link)
        entity-id (:entity-id link)
        request-url (str " http://" aggregate "/link/undercuts/" aggregate "/" entity-id)]
    (get-payload request-url)))

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
  "Retrieve a link from cache or db. Returns :missing if no such link can be found."
  [uri]
  (let [cached-link (cache/retrieve uri)]
    (if (= cached-link :missing)
      (let [db-result (db/links-by-uri uri)]
        (if (= db-result :missing)
          :not-found
          db-result))
      cached-link)))

(defn exact-statement
  "Return the exact statement from cache or db"
  [aggregate-id entity-id version]
  (let [cached-statement (cache/retrieve (str aggregate-id "/" entity-id))]
    (if (and (not= cached-statement :missing)
             (= (:version cached-statement) version))
      cached-statement
      (if-let [maybe-statement (db/exact-statement aggregate-id entity-id version)]
        (do (cache/cache-miss (str aggregate-id "/" entity-id) maybe-statement)
            maybe-statement)))))

(defn local-undercuts
  "Retrieve all links from the db that undercut the link passed as argument."
  [link-uri]
  (let [split-uri (str/split link-uri #"/")
        aggregate (first split-uri)
        entity-id (second split-uri)]
    (db/get-undercuts aggregate entity-id)))
