(ns aggregator.graphql.dbas-connector
  (:require [clj-http.client :as client]
            [clojure.data.json :as  json]
            [clojure.walk :refer [keywordize-keys]]
            [aggregator.config :as config]
            [taoensso.timbre :as log]))

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
  (let [result (query-db "query {statements {uid, textversions {content, author {publicNickname, uid}} references {text, host, path}}}")]
    (map (fn [statement]
           (let [author-name (get-in statement [:textversions :author :publicNickname])
                 author-id (get-in statement [:textversions :author :uid])
                 default-statement {:content
                                    {:text (get-in statement [:textversions :content])
                                     :created nil ;; dbas won't play
                                     :author {:dgep-native true
                                              :name author-name
                                              :id (Integer/parseInt author-id)}}
                                    :identifier
                                    {:aggregate-id config/aggregate-name
                                     :entity-id (:uid statement)
                                     :version 1}
                                    :predecessors []
                                    :delete-flag false
                                    :references (:references statement)}
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
  (let [supportive? (:isSupportive argument)
        conclusion-id (:conclusionUid argument)]
    (if supportive?
      :support
      (if (nil? conclusion-id)
        :undercut
        :attack))))

(defn links-from-argument
  "Use the strange structure of D-BAS-arguments to create links. Needs a connection to the local dbas instance.
  Returned links in EDEN format."
  [argument]
  (let [group-uid (:premisegroupUid argument)
        premises (query-db (format
                            "query {premises(premisegroupUid: %d) {statementUid}}"
                            group-uid))
        link-type-val (link-type argument)
        author (:author argument)
        destination-id (or (:conclusionUid argument) (str "link_" (:argumentUid argument)))]
    (map (fn [premise]
           {:author {:dgep-native true
                     :name (:publicNickname author)
                     :id (Integer/parseInt (:uid author))}
            :created nil ;; nil until we solve the graphql problem
            :type link-type-val
            :source {:aggregate-id config/aggregate-name
                     :entity-id (str (:statementUid premise))
                     :version 1}
            :destination {:aggregate-id config/aggregate-name
                          :version 1
                          :entity-id (str destination-id)}
            :identifier {:aggregate-id config/aggregate-name
                         :entity-id (str "link_" (:uid argument))
                         :version 1}
            :delete-flag (:isDisabled argument)})
         (:premises premises))))

(defn get-links
  "Return a map of all links that can be requested from the connected D-BAS instance."
  []
  (let [result (query-db "query {arguments {uid conclusionUid, isSupportive, author {publicNickname uid}, argumentUid, premisegroupUid, isDisabled}}")
        return-val (mapcat links-from-argument (:arguments result))]
    return-val))

(defn get-author
  "Queries D-BAS for an author and returns an author-map."
  [author-id]
  (let [id (if (int? author-id)
             author-id
             (Integer/parseInt author-id))
        result (query-db (format "query {user(uid: %d){publicNickname}}" id))]
    (log/debug result)
    {:dgep-native true
     :id id
     :name (get-in result [:user :publicNickname])}))

(defn get-references
  "Return the references for given statement-id."
  [sid]
  (let [id (if (int? sid)
             sid
             (Integer/parseInt sid))
        result (query-db (format "query {statement(uid: %d){references{text, host, path}}}"
                                 sid))]
    (get-in result [:statement :references])))
