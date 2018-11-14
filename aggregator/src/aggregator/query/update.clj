(ns aggregator.query.update
  (:require [aggregator.query.db :as db]
            [aggregator.query.cache :as cache]
            [aggregator.broker.publish :as pub]
            [aggregator.config :as config]
            [aggregator.specs :as specs]
            [clojure.spec.alpha :as s]
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
    (let [cache-uri (str (:aggregate-id identifier) "/" (:entity-id identifier) "/"
                         (:version identifier))
          cached-entity (cache/retrieve cache-uri)]
      (when (= :missing cached-entity)
        (cache/cache-miss cache-uri statement))))
  statement)

(defn update-link
  "Update a database-entry for a link. Typically inserts a link if not in DB yet."
  [{:keys [source destination identifier] :as link}]
  (let [db-result (db/exact-link (:aggregate-id source) (:entity-id source) (:version source)
                                 (:aggregate-id destination) (:entity-id destination)
                                 (:version destination))]
    (swap! config/app-state update-in [:known-aggregators] conj (:aggregate-id identifier))
    (swap! config/app-state update-in [:known-aggregators] conj (:aggregate-id destination))
    (swap! config/app-state update-in [:known-aggregators] conj (:aggregate-id source))
    (when-not db-result
      (log/debug (format "[UPDATE] Added new link to db: %s" link))
      (when (= (:aggregate-id identifier) config/aggregate-name)
        (pub/publish-link link))
      (cache/cache-miss-link (str (:aggregate-id identifier) "/" (:entity-id identifier) "/"
                                  (:version identifier))
                             link)
      (db/insert-link link))
    link))

(defn update-statement-content
  "Updates the content-text of a statement and bumps the version."
  [statement updated-text]
  (let [updated-statement (-> statement
                              (assoc-in [:content :content-string] (str updated-text))
                              (update-in [:identifier :version] inc))]
    (when (s/valid? ::specs/statement updated-statement)
      (db/insert-statement updated-statement)
      updated-statement)))

(defn fork-statement
  "Forks a statement with a new identifier, content-string and author."
  [statement identifier content-string author]
  (let [updated-statement (-> statement
                              (assoc-in [:content :content-string] (str content-string))
                              (assoc-in [:content :author] (str author))
                              (assoc :identifier identifier)
                              (assoc-in [:identifier :version] 1)
                              (assoc :predecessors [(:identifier statement)]))]
    (when (s/valid? ::specs/statement updated-statement)
      (db/insert-statement updated-statement)
      (pub/publish-statement updated-statement)
      updated-statement)))
