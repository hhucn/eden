(ns aggregator.query.update
  (:require [aggregator.query.db :as db]
            [aggregator.query.cache :as cache]
            [taoensso.timbre :as log]))

(defn update-statement
  "Update a database-entry for a statement. Typically inserts an entry if not in DB yet."
  [statement]
  (let [db-result (db/exact-statement (:aggregate-id statement) (:entity-id statement)
                                      (:version statement))]
    (when-not db-result
      (log/debug (format "[query] Added new statement to db: %s " statement))
      (cache/cache-miss (str (:aggregate-id statement) "/" (:entity-id statement)) statement)
      (db/insert-statement statement))))

(defn update-link
  "Update a database-entry for a link. Typically inserts a link if not in DB yet."
  [link]
  (let [db-result (db/exact-link (:aggregate-id link) (:entity-id link))]
    (when-not db-result
      (log/debug (format "[query] Added new link to db: %s" link))
      (cache/cache-miss-link (str (:aggregate-id link) "/" (:entity-id link)) link)
      (db/insert-link link))))
