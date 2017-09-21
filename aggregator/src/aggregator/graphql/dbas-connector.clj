(ns aggregator.graphql.dbas-connector
  (:require [clj-http.client :as client]
            [clojure.data.json :as  json]
            [clojure.walk :refer [keywordize-keys]]
            [aggregator.config :as config]))

(defn query-db
  [query]
  (->
   ;; This needs a dbas instance running under the name dbas in the same docker network.
   (client/get "dbas:4284/api/v2/query"
               {:query-params
                {"q" query}})
   :body
   json/read-str
   keywordize-keys
   ))

(defn get-statements
  []
  (let [result (query-db "query { statements { uid, textversions { content timestamp authorUid} }}")]
    (map (fn [statement] {:content (get-in statement [:statement :textversions :content])
                         :aggregate-id config/aggregate-name
                         :entity-id (get-in statement [:statement :uid])
                         :version 1
                         :created (get-in statement [:statement :textversions :timestamp])
                         :author (str config/aggregate-name
                                      " author#: "
                                      (get-in statement [:statement :textversions :authorUid]))})
         result)))

(defn get-statement
  [uid]
  (query-db (format "query { statement(uid: %d) { textversions { content } }}" uid)))

(defn get-issues
  []
  (query-db "query { issues { uid title }}"))

(defn get-issue
  [uid]
  (query-db (format "query { issue(uid: %d) { title }}" uid)))

(defn get-issue-graph
  [uid]
  (query-db (format "query{issue(uid: %d){statements {textversions{content authorUid}}}}" uid)))
