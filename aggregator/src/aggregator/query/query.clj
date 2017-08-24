(ns aggregator.query.query
  (:require [aggregator.query.cache :as cache]))

;; The main module for queries. All internal calls run through the functions defined here. The other aggregator module call these functions, as well as the utility-handlers.


;; Non-API helper functions

(defn retrieve-remote
  "Try to retrieve an argument from a remote aggregator."
  [uri]
  :returned-statement-or-not-found)

(defn check-db
  "Check the database for a statement and update cache after item is found."
  ([uri]
   (check-db uri {}))
  ([uri options]
   (let [possible-statement :check-db function] ;;TODO
     (if (= possible-statement :missing)
       (if (contains options :no-remote)
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
       (check-db uri)
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



