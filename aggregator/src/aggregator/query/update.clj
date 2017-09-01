(ns aggregator.query.update
  (:require [aggregator.utils.db :as db]))

(defn update-statement
  "Update a database-entry for a statement. Typically inserts an entry if not in DB yet."
  [statement]
  (let [db-result (db/exact-statement (:aggregate-id statement) (:entity-id statement)
                                      (:version statement))]
    (when (not db-result)
      (db/insert-statement statement))))


(defn update-link
  "Update a database-entry for a link. Typically inserts a link if not in DB yet."
  [link]
  link)
