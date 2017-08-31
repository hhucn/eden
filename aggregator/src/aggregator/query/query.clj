(ns aggregator.query.query
  (:require [aggregator.query.cache :as cache]
            [aggregator.utils.db :as db]
            [aggregator.query.utils :as utils]
            [clj-http.client :as client]
            [clojure.string :as str]
            [clojure.data.json :as json]))


(defn retrieve-remote
  "Try to retrieve an argument from a remote aggregator. The host part is treated as the webhost."
  [uri]
  (let [split-uri (str/split uri #"/" 1)
        aggregate (first split-uri)
        entity (last split-uri)
        request-url (str "http://" aggregate "/entity/" entity)]
    (-> (client/get request-url {:accept :json})
        (utils/http-response->map)
        :data
        :payload)))

(defn check-db
  "Check the database for an entity and update cache after item is found."
  ([uri]
   (check-db uri {}))
  ([uri {:keys [opts]}]
   (let [possible-entity (db/statement-by-uri uri)]
     (if (= possible-entity :missing)
       (if (some #(= % :no-remote) opts)
         :not-found
         (retrieve-remote uri))
       (cache/cache-miss possible-entity)))))

(defn tiered-retrieval
  "Check whether the Cache contains the desired entity. If not delegate to DB and remote acquisition."
  ([uri]
   (tiered-retrieval uri {}))
  ([uri options] 
   (let [cached-entity (cache/retrieve uri)]
     (if (= cached-entity :missing)
       (check-db uri options)
       cached-entity))))



