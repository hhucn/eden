(ns aggregator.search.core
  (:require [clojure.spec.alpha :as s]
            [clojure.string :refer [blank?]]
            [qbits.spandex :as sp]
            [aggregator.utils.common :as lib]
            [taoensso.timbre :as log]
            [aggregator.specs :as gspecs]))

(def ^:private conn (atom nil))

(defn- create-connection!
  "Read variables from environment and establish connection to the message
  broker."
  [] (reset! conn (sp/client {:hosts ["http://search:9200"]
                              :http-client {:basic-auth {:user "elastic"
                                                         :password "changeme"}}})))

(defn init-connection!
  "Initializes connection to ElasticSearch."
  []
  (create-connection!)
  (log/debug "Connection to ElasticSearch established.")
  (lib/return-ok "Connection established." {:conn @conn}))


;; -----------------------------------------------------------------------------

(defn create-index
  "Create an index based on the provided string. Indexes are necessary to
  categorize the data. Valid settings can be found here:
  http://elasticsearch-cheatsheet.jolicode.com/#indexes"
  ([index-name settings mappings]
   (try
     (lib/return-ok "Index successfully created."
                    (sp/request @conn {:url [(keyword index-name)]
                                       :method :put
                                       :body {:settings settings
                                              :mappings mappings}}))
     (catch Exception e
       (lib/return-error (-> e ex-data :body :error :reason) (ex-data e)))))
  ([index-name] (create-index index-name {} {})))

(defn delete-index
  "Deletes an index from elasticsearch."
  [index-name]
  (try
    (lib/return-ok "Index successfully deleted."
                   (sp/request @conn {:url [(keyword index-name)]
                                      :method :delete}))
    (catch Exception e
      (lib/return-error (-> e ex-data :body :error :reason) (ex-data e)))))


(defn add-statement
  "Add a statement to the statement-index."
  [{:keys [aggregate-id entity-id] :as statement}]
  (lib/return-ok "Added statement to index."
                 (sp/request @conn {:url [:statements aggregate-id entity-id]
                                    :method :put
                                    :body statement})))

(defn delete-statement
  "Deletes a statement from the search-index."
  [{:keys [aggregate-id entity-id]}]
  (try
    (lib/return-ok "Statement deleted."
                   (sp/request @conn {:url [:statements aggregate-id entity-id]
                                      :method :delete}))
    (catch Exception e
      (lib/return-error "Statement could not be deleted, because it was not found in the index."
                        (ex-data e)))))


;; -----------------------------------------------------------------------------
;; Entrypoint

(defn entrypoint []
  (init-connection!))
(entrypoint)


;; -----------------------------------------------------------------------------
;; Specs

(s/def ::index-name (s/and (s/or :keyword keyword? :string string?)
                           #(nil? (re-find #"\ |\"|\*|\\|<|\||,|>|/|\?"
                                           (name (second %))))
                           #(not (blank? (name (second %))))))
(s/def ::number_of_replicas integer?)
(s/def ::number_of_shards integer?)
(s/def ::analysis map?)
(s/def ::refresh_interval string?)
(s/def ::index-settings (s/keys :opt-un [::number_of_replicas ::number_of_shards
                                         ::analysis ::refresh_interval]))
(s/fdef create-index
        :args (s/cat :index-name ::index-name :settings ::index-settings :mappings map?)
        :ret map?)

(s/fdef delete-index
        :args (s/cat :index-name ::index-name)
        :ret map?)

(s/fdef add-statement
        :args (s/cat :statement ::gspecs/statement)
        :ret map?)

(s/fdef delete-statement
        :args (s/cat :statement ::gspecs/statement)
        :ret map?)

(comment
  (-> `create-index s/get-spec :args s/exercise)
  (s/exercise-fn `create-index)
  (stest/check `create-index)
  (s/valid? ::index-settings nil)
  )
