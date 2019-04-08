(ns aggregator.query.update
  (:require [aggregator.query.db :as db]
            [aggregator.query.cache :as cache]
            [aggregator.config :as config]
            [aggregator.graphql.dbas-connector :as dbas]
            [aggregator.specs :as specs]
            [aggregator.utils.common :as utils]
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
      (db/insert-statement statement))
    (let [cache-uri (str (:aggregate-id identifier) "/" (:entity-id identifier) "/"
                         (:version identifier))
          cached-entity (cache/retrieve cache-uri)]
      (when (= :missing cached-entity)
        (cache/cache-miss cache-uri statement))))
  statement)

(defn update-link
  "Update a database-entry for a link. Typically inserts a link if not in DB yet."
  [{:keys [source destination identifier created] :as link}]
  (log/debug (format "Checking if link needs to be added to db: %s" link))
  (let [db-result (db/exact-link (:aggregate-id source) (:entity-id source) (:version source)
                                 (:aggregate-id destination) (:entity-id destination)
                                 (:version destination))]
    (swap! config/app-state update-in [:known-aggregators] conj (:aggregate-id identifier))
    (swap! config/app-state update-in [:known-aggregators] conj (:aggregate-id destination))
    (swap! config/app-state update-in [:known-aggregators] conj (:aggregate-id source))
    (let [new-link (if created
                     link
                     (assoc link :created (utils/time-now-str)))]
      (when-not db-result
        (log/debug (format "[UPDATE] Added new link to db: %s" new-link))
        (cache/cache-miss-link (str (:aggregate-id identifier) "/" (:entity-id identifier) "/"
                                    (:version identifier))
                               new-link)
        (db/insert-link new-link))
      new-link)))

(defn update-statement-content
  "Updates the text of a statement and bumps the version."
  [statement updated-text]
  (let [updated-statement (-> statement
                              (assoc-in [:content :text] (str updated-text))
                              (update-in [:identifier :version] inc))]
    (when (s/valid? ::specs/statement updated-statement)
      (db/insert-statement updated-statement)
      updated-statement)))

(defn fork-statement
  "Forks a statement with a new identifier, text and author."
  [statement identifier text author]
  (let [updated-statement (-> statement
                              (assoc-in [:content :text] (str text))
                              (assoc-in [:content :author] (utils/json->edn author))
                              (assoc :identifier identifier)
                              (assoc-in [:identifier :version] 1)
                              (assoc :predecessors [(:identifier statement)]))]
    (when (s/valid? ::specs/statement updated-statement)
      (db/insert-statement updated-statement)
      updated-statement)))

(defn- statement-from-minimal
  "Generate a statement from the minimal needed information."
  [unfinished-statement author]
  (let [text (:text unfinished-statement)
        prepared-statement (dissoc unfinished-statement :text)
        statement {:content {:text text
                             :author author
                             :created (utils/time-now-str)}
                   :identifier {:aggregate-id config/aggregate-name
                                :entity-id (str (java.util.UUID/randomUUID))
                                :version 1}
                   :delete-flag false
                   :predecessors []}]
    (merge prepared-statement statement)))

(defn- link-premise-conclusion
  "Given a premise and a conclusion, link them both with an argument link."
  [premise conclusion link-type author]
  (let [premise-id (:identifier premise)
        conclusion-id (:identifier conclusion)]
    {:type (keyword link-type)
     :author author
     :source premise-id
     :destination conclusion-id
     :delete-flag false
     :identifier {:aggregate-id config/aggregate-name
                  :entity-id (str "link_" (java.util.UUID/randomUUID))
                  :version 1}
     :created (utils/time-now-str)}))

(defn- get-or-create-statement
  [statement author]
  (if (contains? statement :identifier)
    (db/exact-statement (:identifier statement))
    (statement-from-minimal statement author)))

(defn add-argument
  "Adds an argument to the database. Asuming the author exists and belongs to the local DGEP."
  [premise conclusion link-type author-id]
  (let [author (dbas/get-author author-id)
        complete-premise (get-or-create-statement premise author)
        complete-conclusion (get-or-create-statement conclusion author)
        link (link-premise-conclusion complete-premise complete-conclusion link-type author)]
    (update-statement complete-premise)
    (update-statement complete-conclusion)
    (update-link link)
    {:premise-id (:identifier complete-premise)
     :conclusion-id (:identifier complete-conclusion)
     :link-id (:identifier link)}))

(defn statement-from-text
  "Adds an argument only from text and author-id. Assumes author belongs to local DGEP."
  ([text author-id]
   (statement-from-text text author-id {}))
  ([text author-id additionals]
   (let [author (dbas/get-author author-id)
         unfinished-statement (merge additionals {:text text})]
     (update-statement (statement-from-minimal unfinished-statement author)))))

(defn quicklink
  "Add a link from source, destination, type and author. Assumes author belongs to local DGEP"
  [type source destination author-id]
  (let [author (dbas/get-author author-id)]
    {:type type
     :source source
     :destination destination
     :identifier {:aggregate-id config/aggregate-name
                  :entity-id (str "link-" (java.util.UUID/randomUUID))
                  :version 1}
     :delete-flag false
     :created (utils/time-now-str)
     :author author}))
