(ns aggregator.query.db
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

(defentity events
  (entity-fields :aggregate_id :entity_id :data))

(defn statement-by-uri [uri]
  (let [split-uri (str/split uri #"/")
        aggregate_id (first split-uri)
        entity_id (second split-uri)
        query-value (select events
                            (where {:aggregate_id aggregate_id
                                    :entity_id entity_id}))]
    (if (= '() query-value)
      :missing
      query-value)))
