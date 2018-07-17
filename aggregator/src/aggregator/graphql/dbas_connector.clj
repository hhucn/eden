(ns aggregator.graphql.dbas-connector
  (:require [clj-http.client :as client]
            [clojure.data.json :as  json]
            [clojure.walk :refer [keywordize-keys]]
            [aggregator.config :as config]))

(defn- query-db
  [query]
  (->
   ;; This needs a dbas instance running under the name specified in DBAS_HOST in the same network.
   (client/get (if-let [dbas-host (System/getenv "DBAS_HOST")]
                 (str "http://" dbas-host ":4284/api/v2/query")
                 "http://dbas:4284/api/v2/query")
               {:query-params
                {"q" query}})
   :body
   json/read-str
   keywordize-keys
   ))

(defn get-statement-origin
  "Return a certain statement-origin from D-BAS. This is needed from arguments which are not native to the queried D-BAS instance."
  [statement-uid]
  (let [result (query-db (format "query {statementOrigin(statementUid: %s) {entityId, aggregateId, author, version}}" statement-uid))]
    (:statementOrigin result)))

(defn get-statements
  "Return all statements from the D-BAS instance. The result is already in the EDEN-conform format."
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
  "Takes an `argument` (link) and returns the type of the link as a keyword.
  (One of `:support`, `:attack`, `:undercut`)"
  [argument]
  (let [supportive? (get-in argument [:argument :isSupportive])
        conclusion-id (get-in argument [:argument :conclusionUid])]
    (if supportive?
      :support
      (if conclusion-id
        :attack
        :undercut))))

(defn links-from-argument
  "Use the strange structure of D-BAS-arguments to create links. Needs a connection to the local dbas instance.
  Returned links in EDEN format."
  [argument]
  (let [group-uid (:premisesgroupUid argument)
        premises (query-db (format
                            "query {premises(premisegroupUid: %d) {statementUid}}"
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
  "Return a map of all links that can be requested from the connected D-BAS instance."
  []
  (let [result (query-db "query {arguments {uid conclusionUid, isSupportive, authorUid, argumentUid, premisegroupUid}}")
        return-val (mapcat links-from-argument (:arguments result))]
    return-val))

