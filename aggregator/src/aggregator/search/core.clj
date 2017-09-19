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

(defn- add
  "Add new content to the index."
  [index {:keys [aggregate-id entity-id] :as entity} msg]
  (lib/return-ok msg
                 (sp/request @conn {:url [index aggregate-id entity-id]
                                    :method :put
                                    :body entity})))

(defn- delete
  "Delete entity from ElasticSearch."
  ([index-name msg] (delete index-name nil msg))
  ([index-name {:keys [aggregate-id entity-id]} msg]
   (let [deletion-path (vec (remove nil? (conj [(keyword index-name)] aggregate-id entity-id)))]
     (try
       (lib/return-ok msg
                      (sp/request @conn {:url deletion-path
                                         :method :delete}))
       (catch Exception e
         (lib/return-error (or (-> e ex-data :body :error :reason)
                               "Entity not found.")
                           (ex-data e)))))))


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
  [index-name] (delete index-name "Index successfully deleted."))

(defn add-statement
  "Add a statement to the statement-index. Updates existing entities, identified
  by aggregate-id and entity-id, and updates them if they already existed."
  [statement] (add :statements statement "Added statement to index."))

(defn delete-statement
  "Deletes a statement from the search-index."
  [statement] (delete :statements statement "Statement deleted."))

(defn add-link
  "Add a link to the link-index. Updates existing entities, identified
  by aggregate-id and entity-id, and updates them if they already existed."
  [link] (add :links link "Added link to index."))

(defn delete-link
  "Deletes a link from ElasticSearch."
  [link] (delete :links link "Link deleted."))


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
        :ret :aggregator.utils.common/return-map)

(s/fdef delete-index
        :args (s/cat :index-name ::index-name)
        :ret :aggregator.utils.common/return-map)

(s/fdef add
        :args (s/cat :index ::index-name
                     :entity (s/keys :req-un [::gspecs/aggregate-id ::gspecs/entity-id])
                     :msg string?)
        :ret :aggregator.utils.common/return-map)

(s/fdef delete
        :args (s/cat :index ::index-name
                     :entity (s/keys :req-un [::gspecs/aggregate-id ::gspecs/entity-id])
                     :msg string?)
        :ret :aggregator.utils.common/return-map)

(s/fdef add-statement
        :args (s/cat :statement ::gspecs/statement)
        :ret :aggregator.utils.common/return-map)

(s/fdef delete-statement
        :args (s/cat :statement ::gspecs/statement)
        :ret :aggregator.utils.common/return-map)

(s/fdef add-link
        :args (s/cat :link ::gspecs/link)
        :ret :aggregator.utils.common/return-map)

(s/fdef delete-link
        :args (s/cat :link ::gspecs/link)
        :ret :aggregator.utils.common/return-map)

(comment
  (-> `create-index s/get-spec :args s/exercise)
  (s/exercise-fn `create-index)
  (stest/check `create-index)
  (s/valid? ::index-settings nil)
  )
