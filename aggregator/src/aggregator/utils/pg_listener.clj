(ns aggregator.utils.pg-listener
  (:require [postgres-listener.core :as pgl]
            [aggregator.config :as config]
            [aggregator.utils.common :as utils]
            [aggregator.query.update :as update]
            [taoensso.timbre :as log]
            [aggregator.graphql.dbas-connector :as dbas-conn :refer [get-statement-origin]]))

(defn- handle-statements
  "Handle changes in statements"
  [statement]
  (log/debug (format "[not implemented] Received new statement from D-BAS: %s" statement)))

(defn- handle-textversions
  "Handle changes in the textversions. They belong to the statements."
  [textversion]
  (log/debug (format "Received new textversion from D-BAS: %s" (:data textversion)))
  (let [author-id (get-in textversion [:data :author_uid])
        references (dbas-conn/get-references (get-in textversion [:data :statement_uid]))
        statement {:content {:author (dbas-conn/get-author author-id)
                             :text (get-in textversion [:data :content])
                             :created nil}
                   :identifier {:aggregate-id config/aggregate-name
                                :entity-id (str (get-in textversion [:data :uid]))
                                :version 1}
                   :delete-flag false
                   :predecessors []
                   :references references}
        origin (get-statement-origin (get-in textversion [:data :statement_uid]))]
    (if origin
      (update/update-statement
       (assoc-in (assoc statement
                        :identifier {:aggregate-id (str (:aggregate_id origin))
                                     :entity-id (str (:entity_id origin))
                                     :version (:version origin)})
                 [:content :author] {:dgep-native false
                                     :name (str (:author origin))
                                     :id 1234567}))
      (update/update-statement statement))))


(defn- link-type
  [argument]
  (let [supportive? (:is_supportive argument)
        conclusion-id (:conclusion_uid argument)]
    (if supportive?
      :support
      (if (nil? conclusion-id)
        :undercut
        :attack))))

(defn- links-from-argument
  [argument]
  (let [group-uid (:premisegroup_uid argument)
        premises (dbas-conn/premises-from-premisegroup group-uid)
        link-type-val (link-type argument)
        author (dbas-conn/get-author (:author_uid argument))
        destination-id (or (:conclusion_uid argument) (str "link_" (:argument_uid argument)))]
    (map (fn [premise]
           {:author author
            :created (utils/time-now-str)
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
            :delete-flag (:is_disabled argument)})
         (:premises premises))))

(defn- handle-arguments
  "Handle changes in arguments and update links correspondingly."
  [argument]
  (let [data (:data argument)]
    (log/debug (str "new argument: " data))
    (doall (map update/update-link (links-from-argument data)))))

(defn start-listeners
  "Start all important listeners."
  []
  (pgl/connect {:host (System/getenv "DB_HOST")
                :port (read-string (System/getenv "DB_PORT"))
                :database (System/getenv "DB_NAME")
                :user (System/getenv "DB_USER")
                :password (System/getenv "DB_PW")})
  (doseq [[f event] [[handle-textversions "textversions_changes"]
                     [handle-statements "statements_changes"]
                     [handle-arguments "arguments_changes"]]]
    (pgl/arm-listener f event)
    (log/debug (format "Listener for event %s started." event)))
  (log/debug "Started all listeners for DBAS-PG-DB"))
