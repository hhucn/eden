(ns aggregator.query.update
  (:require [aggregator.query.db :as db]
            [aggregator.query.cache :as cache]
            [aggregator.broker.publish :as pub]
            [taoensso.timbre :as log]))

(defn update-statement
  "Update a database-entry for a statement. Typically inserts an entry if not in DB yet."
  [statement]
  (log/debug "[DEBUG] Statement to query: " statement)
  (let [db-result (db/exact-statement (:aggregate-id statement) (:entity-id statement)
                                      (:version statement))]
    (when-not db-result
      (log/debug (format "[UPDATE] Added new statement to db: %s " statement))
      (when (= (:aggregate-id statement) (System/getenv "HOSTNAME"))
        (pub/publish-statement statement))
      (cache/cache-miss (str (:aggregate-id statement) "/" (:entity-id statement)) statement)
      (db/insert-statement statement))))

(defn update-link
  "Update a database-entry for a link. Typically inserts a link if not in DB yet."
  [link]
  (let [db-result (if (:to-version link)
                    (db/exact-link (:aggregate-id link) (:entity-id link) (:from-version link)
                                   (:to-aggregate-id link) (:to-entity-id link))
                    (db/exact-link (:aggregate-id link) (:entity-id link) (:from-version link)
                                   (:to-aggregate-id link) (:to-entity-id link)
                                   (:to-version link)))]
    (when-not db-result
      (log/debug (format "[UPDATE] Added new link to db: %s" link))
      (cache/cache-miss-link (str (:aggregate-id link) "/" (:entity-id link)) link)
      (db/insert-link link))))
