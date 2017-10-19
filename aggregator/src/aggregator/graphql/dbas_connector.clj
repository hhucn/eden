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
  (let [result (query-db "query { statements { uid, textversions { content, timestamp, authorUid} }}")]
    (map (fn [statement] {:content (get-in statement [:statement :textversions :content])
                         :aggregate-id config/aggregate-name
                         :entity-id (get-in statement [:statement :uid])
                         :version 1
                         :created (get-in statement [:statement :textversions :timestamp])
                         :author (str config/aggregate-name
                                      " author#: "
                                      (get-in statement [:statement :textversions :authorUid]))})
         result)))

(defn link-type
  [argument]
  (let [supportive? (get-in argument [:argument :isSupportive])
        conclusion-id (get-in argument [:argument :conclusionUid])]
    (if supportive?
      :support
      (if conclusion-id
        :attack
        :undercut))))

(defn links-from-argument
  "Use the strange structure of DBAS-arguments to create links. Needs a connection to the local dbas instance."
  [argument]
  (let [group-uid (argument :premisesgroupUid)
        premises (query-db (format
                            "query {premises(premisesgroupUid: %d) {statementUid }}"
                            group-uid))
        link-type (link-type argument)]
    (map (fn [premise]
           {:author (str config/aggregate-name " author#: "
                         (argument :authorUid))
            :type link-type
            :from-aggregate-id config/aggregate-name
            :from-entity-id (premise :statementUid)
            :from-version 1
            :to-aggregate-id config/aggregate-name
            :to-entity-id (if (= link-type :undercut)
                            (argument :argumentUid)
                            (argument :conclusionUid))
            :to-version (if (= link-type :undercut)
                          nil
                          1)
            :aggregate-id config/aggregate-name
            :entity-id (argument :uid)
            :created (argument :timestamp)})
         premises)))


(defn get-links
  []
  (let [result (query-db "query {arguments {uid conclusionUid, isSupportive, authorUid, timestamp, argumentUid, premisesgroupUid}}")]
    (mapcat links-from-argument result)))
