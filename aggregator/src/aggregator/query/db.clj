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
  (entity-fields :aggregate_id :text))

(System/getenv "POSTGRES_USER")

(select events)
