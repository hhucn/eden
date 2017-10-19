(ns aggregator.utils.pg-listener
  (:require [postgres-listener.core :as pgl]
            [aggregator.config :as config]
            [taoensso.timbre :as log]
            [aggregator.graphql.dbas-connector :refer [links-from-argument]]))


(defn- handle-statements
  "Handle changes in statements"
  [statement]
  (log/debug (str "new statement: " statement)))

(defn- handle-textversions
  "Handle changes in the textversions. They belong to the statements."
  [textversion]
  (log/debug "new textversion: " (:data textversion))
  {:author (get-in textversion [:data :author_uid])
   :content (get-in textversion [:data :content])
   :aggregate-id config/aggregate-name
   :entity-id (get-in textversion [:data :uid])
   :version 1
   :created (get-in textversion [:data :timestamp])})

(defn- handle-arguments
  "Handle changes in arguments and update links correspondingly."
  [argument]
  (let [data (:data argument)]
    (log/debug "new argument: " data)
    (links-from-argument data))) ;; List of links, thanks premisegroup

(defn start-listeners
  "Start all important listeners."
  []
  (pgl/connect {:host "dbas-db"
                :port (read-string (System/getenv "DBAS_DB_PORT"))
                :database "discussion"
                :user (System/getenv "DBAS_DB_USER")
                :password (System/getenv "DBAS_DB_PW")})
  (pgl/arm-listener handle-statements "statements_changes")
  (pgl/arm-listener handle-textversions "textversions_changes")
  (pgl/arm-listener handle-arguments "arguments_changes")
  (log/debug "Started listeners for DBAS-PG-DB"))

(start-listeners)

