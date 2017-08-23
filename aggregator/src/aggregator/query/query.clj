(ns aggregator.query.query) ;;TODO

;; The main module for queries. All internal calls run through the functions defined here. The other aggregator module call these functions, as well as the utility-handlers.

(defn statement [id]
  id)

(defn discussion [id]
  id)

(defn statement-tree [id]
  [id id id])

(defn add-statement [statement]
  {:status :okay})

