(ns aggregator.search.core
  (:require [clojure.spec.alpha :as s]
            [clojure.string :refer [blank?]]
            [qbits.spandex :as sp]
            [aggregator.utils.common :as lib]))

(defonce es-conn
  (sp/client {:hosts ["http://search:9200"]
              :http-client {:basic-auth {:user "elastic"
                                         :password "changeme"}}}))

(defn create-index
  "Create an index based on the provided string. Indexes are necessary to
  categorize the data. Valid settings can be found here:
  http://elasticsearch-cheatsheet.jolicode.com/#indexes"
  ([index-name settings mappings]
   (try
     (->>
      (sp/request es-conn {:url [(keyword index-name)]
                           :method :put
                           :body {:settings settings
                                  :mappings mappings}})
      (lib/return-ok "Index successfully created."))
     (catch Exception e
       (lib/return-error (-> e ex-data :body :error :reason) (ex-data e)))))
  ([index-name] (create-index index-name {} {})))

(defn delete-index
  "Deletes an index from elasticsearch."
  [index-name]
  (try
    (->>
     (sp/request es-conn {:url [(keyword index-name)]
                          :method :delete})
     (lib/return-ok "Index successfully deleted."))
    (catch Exception e
      (lib/return-error (-> e ex-data :body :error :reason) (ex-data e)))))


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

(comment
  (-> `create-index s/get-spec :args s/exercise)
  (s/exercise-fn `create-index)
  (stest/check `create-index)
  (s/valid? ::index-settings nil)
  )
