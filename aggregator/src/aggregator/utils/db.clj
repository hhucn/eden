(ns aggregator.utils.db
  (:use [korma.db]
        [korma.core])
  (:require [clojure.string :as str]))

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
