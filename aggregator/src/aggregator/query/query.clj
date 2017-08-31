(ns aggregator.query.query
  (:require [aggregator.query.cache :as cache]
            [aggregator.utils.db :as db]
            [aggregator.query.utils :as utils]
            [clj-http.client :as client]
            [clojure.string :as str]
            [clojure.data.json :as json]))

;; The main module for queries. All internal calls run through the functions defined here. The other aggregator module call these functions, as well as the utility-handlers.


;; Non-API helper functions

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
  "Check the database for a statement and update cache after item is found."
  ([uri]
   (check-db uri {}))
  ([uri {:keys [opts]}]
   (let [possible-statement (db/statement-by-uri uri)]
     (if (= possible-statement :missing)
       (if (some #(= % :no-remote) opts)
         :not-found
         (retrieve-remote uri))
       (cache/cache-miss possible-statement)))))

(defn tiered-retrieval
  "Check whether the Cache contains the desired statement. If not delegate to DB and remote acquisition."
  ([uri]
   (tiered-retrieval uri {}))
  ([uri options] 
   (let [cached-statement (cache/retrieve uri)]
     (if (= cached-statement :missing)
       (check-db uri options)
       cached-statement))))


;;
;;;; Call the following functions directly
;;

(defn statement [uri]
  (let [result (tiered-retrieval uri)]
 ))

(defn link [id]
  id)

(defn statement-by-link [link]
  {:statement "hi"})

(defn add-link [link]
   {:status :ok})

(defn add-statement [statement]
  {:status :ok})

(defn statements-by-provider [provider]
  (:a :b :c))

(defn statements-by-author [author]
  (:d :e :f))

(defn linked-statements [uri]
  (:foo :bar))



