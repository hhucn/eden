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

(defn get-statement-origin
  [statement-uid]
  (let [result (query-db (format "query {statementOrigin(statementUid: %s) {entityId, aggregateId, author, version}}" statement-uid))]
    (:statementOrigin result)))

(defn get-statements
  []
  (let [result (query-db "query { statements { uid, textversions { content, authorUid} }}")]
    (map (fn [statement]
           (let [default-statement {:content
                                    {:content-string (get-in statement [:textversions :content])
                                     :created nil ;; dbas won't play
                                     :author (str "author#: "
                                                  (get-in statement [:textversions :authorUid]))}
                                    :identifier
                                    {:aggregate-id config/aggregate-name
                                     :entity-id (:uid statement)
                                     :version 1}
                                    :predecessor {}
                                    :delete-flag false}
                 origin (get-statement-origin (:uid statement))]
             (if origin
               (assoc-in (assoc default-statement
                                :identifier {:aggregate-id (:aggregate_id origin)
                                             :entity-id (:entity_id origin)
                                             :version (:version origin)})
                         [:content :author]
                         (:author origin))
               default-statement)))
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
                 {:author (str "author#: "
                               (:authorUid argument))
                  :created nil ;; nil until we solve the graphql problem
                  :type link-type-val
                  :source {:aggregate-id config/aggregate-name
                           :entity-id (:statementUid premise)
                           :version 1}
                  :destination {:aggregate-id config/aggregate-name}
                  :identifier {:aggregate-id config/aggregate-name
                               :entity-id (:uid argument)
                               :version 1}}]
             (if (not= link-type-val :undercut)
               (assoc prepared-link :destination {:aggregate-id config/aggregate-name
                                                  :version 1
                                                  :entity-id (:conclusionUid argument)})
               (assoc-in prepared-link [:destination :entity-id] (:argumentUid argument)))))
         (:premises premises))))


(defn get-links
  []
  (let [result (query-db "query {arguments {uid conclusionUid, isSupportive, authorUid, argumentUid, premisesgroupUid}}")
        return-val (mapcat links-from-argument (:arguments result))]
    return-val))

