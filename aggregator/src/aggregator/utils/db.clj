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
  (entity-fields :aggregate_id :entity_id :version :content :author :created))

(defn statement-by-uri [uri]
  "Return all versions of the desired statement or :missing if it can not be found."
  (let [split-uri (str/split uri #"/")
        aggregate_id (first split-uri)
        entity_id (second split-uri)
        query-value (select statements
                            (where {:aggregate_id aggregate_id
                                    :entity_id entity_id}))]
    (if (= '() query-value)
      :missing
      query-value)))

(defn statement-by-author [author]
  "Return all statements with a certain author."
  (select statements (where {:author author})))
