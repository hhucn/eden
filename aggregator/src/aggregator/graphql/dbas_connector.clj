(ns aggregator.graphql.dbas-connector
  (:require [clj-http.client :as client]
            [clojure.data.json :as  json]
            [clojure.walk :refer [keywordize-keys]]
            [aggregator.config :as config]
            [taoensso.timbre :as log]))

(defn query-db
  [query]
  (->
   ;; This needs a dbas instance running under the name dbas in the same docker network.
   (client/get "http://dbas:4284/api/v2/query"
               {:query-params
                {"q" query}})
   :body
   json/read-str
   keywordize-keys
   ))

(defn get-statements
  []
  (let [result (query-db "query { statements { uid, textversions { content, authorUid} }}")]
    (map (fn [statement] {:content (get-in statement [:textversions :content])
                         :aggregate-id config/aggregate-name
                         :entity-id (:uid statement)
                         :version 1
                         :created nil ;; dbas won't play
                         :author (str config/aggregate-name
                                      " author#: "
                                      (get-in statement [:textversions :authorUid]))})
         (:statements result))))

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
  (let [group-uid (:premisesgroupUid argument)
        premises (query-db (format
                            "query {premises(premisesgroupUid: %d) {statementUid}}"
                            group-uid))
        link-type-val (link-type argument)]
    (map (fn [premise]
           (let [prepared-link 
                 {:author (str config/aggregate-name " author#: "
                               (:authorUid argument))
                  :type link-type-val
                  :from-aggregate-id config/aggregate-name
                  :from-entity-id (:statementUid premise)
                  :from-version 1
                  :to-aggregate-id config/aggregate-name
                  :aggregate-id config/aggregate-name
                  :entity-id (:uid argument)
                  :created nil}] ;; nil until we solve the graphql problem
             (if (not= link-type-val :undercut)
               (assoc prepared-link :to-version 1 :to-entity-id (:conclusionUid argument))
               (assoc prepared-link :to-entity-id (:argumentUid argument)))))
         (:premises premises))))


(defn get-links
  []
  (let [result (query-db "query {arguments {uid conclusionUid, isSupportive, authorUid, argumentUid, premisesgroupUid}}")
        return-val (mapcat links-from-argument (:arguments result))]
    return-val))

