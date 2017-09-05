(ns aggregator.query.db
  (:use [korma.db]
        [korma.core])
  (:require [clojure.string :as str]
            [aggregator.query.utils :as utils]))

(defdb db (postgres {
                     :host "db"
                     :port "5432"
                     :db "aggregator"
                     :user (System/getenv "POSTGRES_USER")
                     :password (System/getenv "POSTGRES_PASSWORD")
                     :delimiters ""}))

(defentity statements
  (entity-fields :aggregate_id :entity_id :version :content :author :created :ancestor_aggregate_id
                 :ancestor_entity_id :ancestor_version))

(defentity links
  (entity-fields :author :type :aggregate_id :entity_id :from_aggregate_id :from_entity_id :from_version
                 :to_aggregate_id :to_entity_id :to_version :created))

(defn part-uri
  [uri]
  (let [split-uri (str/split uri #"/")]
    [(first split-uri) (second split-uri)]))

(defn entity-by-uri
  "Returns all entities matched by the uri."
  [uri entity-type]
  (let [uri-info (part-uri uri)
        query-value (select entity-type
                            (where {:aggregate_id (first uri-info)
                                    :entity_id (second uri-info)}))]
    (if (= '() query-value)
      :missing
      query-value)))

(defn statements-by-uri
  "Return all versions of the desired statement or :missing if it can not be found."
  [uri]
  (entity-by-uri uri statements))

(defn statements-by-author
  "Return all statements with a certain author."
  [author]
  (select statements (where {:author author})))

(defn links-by-uri
  "Return all link-versions defined by the uri"
  [uri]
  (entity-by-uri uri links))

(defn exact-statement
  "Return the exact statement and only that if possible."
  [aggregate-id entity-id version]
  (let [db-result (select statements (where {:aggregate_id aggregate-id
                                             :entity_id entity-id
                                             :version version}))]
    (first db-result)))

(defn insert-statement
  "Requires a map conforming to the ::aggregator.specs/statement as input. Inserts the statement into the database."
  [statement-map]
  (insert statements (values (utils/underscore-keys statement-map))))

(defn exact-link
  "Return the exact link and only that if possible."
  [from-aggregate from-entity from-version to-aggregate to-entity & [to-version]]
  (let [db-result (select links (where {:from_aggregate_id from-aggregate :from_entity_id from-entity
                                        :from_version from-version :to_aggregate_id to-aggregate
                                        :to_entity_id to-entity :to_version to-version}))]
    (first db-result)))

(defn insert-link
  "Requires a map conforming to the ::aggregator.specs/link as input. Inserts the statement into the database."
  [link-map]
  (insert links (values (utils/underscore-keys link-map))))

(defn get-undercuts
  "Returns all undercuts that point to target aggregator and entity-id."
  [target-aggregator target-entity-id]
  (select links (where {:type "undercut"
                        :to_aggregate_id target-aggregator
                        :to_entity_id target-entity-id})))

(defn links-by-target
  "Return all links with the corresponding target."
  [target-aggregator target-entity target-version]
  (select links (where {:to_aggregate_id target-aggregator
                        :to_entity_id target-entity
                        :to_version target-version})))
