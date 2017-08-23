(ns aggregator.query.query
  (:require [aggregator.query.cache :as cache]))

;; The main module for queries. All internal calls run through the functions defined here. The other aggregator module call these functions, as well as the utility-handlers.

(defn statement [uri]
  (let [result (cache/retrieve uri)]
    (if (= result :missing)
      (from-db uri)
      result)))

(defn discussion [id]
  id)

(defn statement-tree [id]
  [id id id])

(defn add-statement [statement]
  {:status :okay})

