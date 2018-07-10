(ns aggregator.query.update
  (:require [aggregator.query.db :as db]
            [aggregator.query.cache :as cache]
            [aggregator.broker.publish :as pub]
            [aggregator.config :as config]
            [clojure.set]
            [taoensso.timbre :as log]))

(defn update-statement
  "Update a database-entry for a statement. Typically inserts an entry if not in DB yet."
  [{:keys [identifier] :as statement}]
  (log/debug "[DEBUG] Statement to query: " statement)
  ;; Add aggregator to known list
  (swap! config/app-state update-in [:known-aggregators] conj (:aggregate-id identifier))
  (let [db-result (db/exact-statement (:aggregate-id identifier) (:entity-id identifier)
                                      (:version identifier))]
    (when-not db-result
      (log/debug (format "[UPDATE] Added new statement to db: %s " statement))
      (when (= (:aggregate-id identifier) config/aggregate-name)
        (pub/publish-statement statement))
      (db/insert-statement statement))
    (cache/cache-miss (str (:aggregate-id identifier) "/" (:entity-id identifier) "/"
                           (:version identifier))
                      statement)))

(defn update-link
  "Update a database-entry for a link. Typically inserts a link if not in DB yet."
  [{:keys [source destination identifier] :as link}]
  (let [db-result (db/exact-link (:aggregate-id source) (:entity-id source) (:version source)
                                 (:aggregate-id destination) (:entity-id destination)
                                 (:version destination))
        named-aggregators #{(:aggregate-id identifier)
                            (:aggregate-id destination) (:aggregate-id source)}]
    (swap! config/app-state update-in [:known-aggregators] clojure.set/union named-aggregators)
    (when-not db-result
      (log/debug (format "[UPDATE] Added new link to db: %s" link))
      (when (= (:aggregate-id identifier) config/aggregate-name)
        (pub/publish-link link))
      (cache/cache-miss-link (str (:aggregate-id identifier) "/" (:entity-id identifier) "/"
                                  (:version identifier))
                             link)
      (db/insert-link link))))
