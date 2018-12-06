(ns aggregator.search.core
  (:require [clojure.spec.alpha :as s]
            [clojure.data.json :as json]
            [clojure.walk :refer [keywordize-keys]]
            [clojure.string :as string]
            [qbits.spandex :as sp]
            [aggregator.utils.common :as lib]
            [taoensso.timbre :as log]
            [aggregator.specs :as gspecs]
            [clj-http.client :as client]))

(def ^:private conn (atom nil))

(def host (if-let [host (System/getenv "ELASTICSEARCH_HOST")]
            (str "http://" host ":9200")
            "http://search:9200"))

(defn- create-connection!
  "Read variables from environment and establish connection to the message
  broker."
  [] (reset! conn (sp/client {:hosts [host]})))

(defn init-connection!
  "Initializes connection to ElasticSearch."
  []
  (create-connection!)
  (log/debug "Connection to ElasticSearch established.")
  (lib/return-ok "Connection established." {:conn @conn}))


;; -----------------------------------------------------------------------------
;; Helper functions

(defn- append-star-if-not-empty [querystring]
  (let [escaped-query (string/escape querystring lib/es-special-characters)]
    (if-not (empty? escaped-query)
      (str escaped-query "*")
      "")))

(defn- construct-query
  "Construct a list for an elastic query with the arguments surrounded by :match hash-maps."
  [querymap]
  (vec (map (fn [[k v]] {:match {k v}}) querymap)))

;; -----------------------------------------------------------------------------

(defn- add
  "Add new content to the index."
  [index type {:keys [identifier] :as entity} msg]
  (lib/return-ok msg
                 (sp/request @conn {:url [index type (str (:aggregate-id identifier)
                                                          "_"
                                                          (:entity-id identifier)
                                                          "_"
                                                          (:version identifier))]
                                    :method :put
                                    :body entity})))

(defn- delete
  "Delete entity from ElasticSearch."
  ([index-name msg] (delete index-name  nil nil msg))
  ([index-name type {:keys [identifier]} msg]
   (let [deletion-path (vec (remove nil? (conj [(keyword index-name)]
                                               type)))
         final-path (if identifier
                      (vec (conj deletion-path (str (:aggregate-id identifier)
                                                    "_"
                                                    (:entity-id identifier)
                                                    "_"
                                                    (:version identifier))))
                      deletion-path)]
     (try
       (lib/return-ok msg
                      (sp/request @conn {:url final-path
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
  [index-name]
  (delete index-name "Index successfully deleted."))

(defn add-statement
  "Add a statement to the statement-index. Updates existing entities, identified
  by aggregate-id and entity-id, and updates them if they already existed."
  [statement]
  (add :statements :statement statement "Added statement to index."))

(defn delete-statement
  "Deletes a statement from the search-index."
  [statement]
  (delete :statements :statement statement "Statement deleted."))

(defn add-link
  "Add a link to the link-index. Updates existing entities, identified
  by aggregate-id and entity-id, and updates them if they already existed."
  [link]
  (add :links :link link "Added link to index."))

(defn delete-link
  "Deletes a link from ElasticSearch."
  [link]
  (delete :links :link link "Link deleted."))


;; -----------------------------------------------------------------------------

(defn- search-request
  "Pass search-request to ElasticSearch."
  ([body-query]
   (search-request body-query nil))
  ([body-query index]
   (try
     (let [index-path (if index [host (name index) "_search"] [host "_search"])
           res (client/get (string/join "/" index-path)
                           {:body (json/write-str body-query)
                            :content-type :json})
           res-edn (-> res :body json/read-str keywordize-keys)]
       (lib/return-ok (str (get-in res-edn [:hits :total]) " hit(s)")
                      (:hits res-edn)))
     (catch Exception e
       (lib/return-error (-> e ex-data :body :error :reason) (ex-data e))))))

(defmulti search
  "Multimethod to dispatch the different search types. Currently defaults to the
  fulltext-search."
  (fn [field _] field))

(defmethod search :fulltext [_ querystring]
  "Classic search-box style full-text query."
  (search-request
   {:query
    {:query_string
     {:query (append-star-if-not-empty querystring)}}}))

(defmethod search :fuzzy [_ querystring]
  "Allow a bit of fuzziness, max. of two edits allowed."
  (search-request
   {:query
    {:match
     {:_all
      {:query (append-star-if-not-empty querystring)
       :fuzziness "AUTO"}}}}))

(defmethod search :default [_ querystring]
  (search :fulltext querystring))

(defmethod search :statements [_ querymap]
  "Search for a matching entity (multiple versions possible)."
  (search-request {:query {:bool {:must (construct-query querymap)}}} :statements))

(defmethod search :statements-fuzzy [_ querystring]
  (search-request
   {:query
    {:match
     {:content.text
      {:query (append-star-if-not-empty querystring)
       :fuzziness "AUTO"}}}} :statements))

(defmethod search :all-statements [_ aggregate-id]
  "Return the first 10.000 results of the statements from a specified
  aggregate-id. Returns the first 10k statements on the queried host if an empty
  aggregate-id is provided."
  (if aggregate-id
    (search-request {:from 0
                     :size 10000
                     :query {:bool {:must {:match {:identifier.aggregate-id aggregate-id}}}}} :statements)
    (search-request {:from 0 :size 10000} (str "statements/"))))

(defmethod search :all-links [_ aggregate-id]
  "Return the first 10.000 results of the links from a specified
   aggregate-id. Returns the first 10k links on the queried host
   if an empty aggregate-id is provided."
  (if aggregate-id
    (search-request {:from 0
                     :size 10000
                     :query {:bool {:must {:match {:identifier.aggregate-id aggregate-id}}}}} :links)
    (search-request {:from 0 :size 10000} (str "links/"))))

(defmethod search :links [_ querymap]
  "Search for a matching entity (multiple versions possible)."
  (search-request {:query {:bool {:must (construct-query querymap)}}} :links))
;; Dispatch between statement or link? (version)

;; -----------------------------------------------------------------------------
;; Entrypoint

(defn entrypoint
  "Initializes the connection to the elasticsearch instance and creates indices properly. Should be executed at the start of the instance and before tests using elasticsearch."
  []
  (init-connection!)
  (create-index "statements" {:index
                              {:analysis
                               {:filter
                                {:synonym_filter
                                 {:expand true
                                  :type "synonym"
                                  :synonyms_path "synonyms_english.txt"}}
                                :analyzer
                                {:synonym_analyzer
                                 {:tokenizer "standard"
                                  :filter ["lowercase" "synonym_filter"]}}}}}
                {:statement {:properties  {:identifier.aggregate-id {:type :keyword}
                                           :identifier.entity-id {:type :keyword}
                                           :content.text {:type "text"
                                                          :analyzer "synonym_analyzer"}
                                           :predecessors {:type "nested"}
                                           :references {:type "nested"}}}})
  (create-index "links" {} {:link {:properties {:identifier.aggregate-id {:type :keyword}
                                                :identifier.entity-id {:type :keyword}}}}))


;; -----------------------------------------------------------------------------
;; Specs

(s/def ::index-name (s/and (s/or :keyword keyword? :string string?)
                           #(nil? (re-find #"\ |\"|\*|\\|<|\||,|>|/|\?"
                                           (name (second %))))
                           #(not (string/blank? (name (second %))))))
(s/def ::number_of_replicas integer?)
(s/def ::number_of_shards integer?)
(s/def ::analysis map?)
(s/def ::refresh_interval string?)
(s/def ::index-settings (s/keys :opt-un [::number_of_replicas ::number_of_shards
                                         ::analysis ::refresh_interval]))
(s/fdef create-index
        :args (s/cat :index-name ::index-name :settings ::index-settings :mappings map?)
        :ret ::lib/return-map)

(s/fdef delete-index
        :args (s/cat :index-name ::index-name)
        :ret ::lib/return-map)

(s/fdef add
        :args (s/cat :index ::index-name
                     :entity (s/keys :req-un [::gspecs/aggregate-id ::gspecs/entity-id])
                     :msg string?)
        :ret ::lib/return-map)

(s/fdef delete
        :args (s/cat :index ::index-name
                     :entity (s/keys :req-un [::gspecs/aggregate-id ::gspecs/entity-id])
                     :msg string?)
        :ret ::lib/return-map)

(s/fdef add-statement
        :args (s/cat :statement ::gspecs/statement)
        :ret ::lib/return-map)

(s/fdef delete-statement
        :args (s/cat :statement ::gspecs/statement)
        :ret ::lib/return-map)

(s/fdef add-link
        :args (s/cat :link ::gspecs/link)
        :ret ::lib/return-map)

(s/fdef delete-link
        :args (s/cat :link ::gspecs/link)
        :ret ::lib/return-map)

(s/fdef append-star-if-not-empty
        :args (s/cat :querystring string?)
        :ret string?)

(s/fdef construct-query
        :args (s/cat :querymap map?))

(s/fdef search
        :args (s/cat :type keyword? :query (s/or :string string? :map map?))
        :ret ::lib/return-map)
