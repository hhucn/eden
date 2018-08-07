(ns aggregator.query.db
  "This module is an API which wraps elastic as a database to be used in EDEN."
  (:require [clojure.string :as str]
            [aggregator.config :as config]
            [aggregator.search.core :as elastic]))

(defn part-uri
  "A helper function which splits an URI at `/`.
  Returns a tuple with the first and second part."
  [uri]
  (let [split-uri (str/split uri #"/")]
    [(first split-uri) (second split-uri)]))

(defn unpack-elastic
  "Unpack an elasticsearch response to a list of entities."
  [response]
  (map :_source (get-in response [:data :hits])))

(defn- keywordize-types
  "Keywordize the type value of links coming from the elastic db."
  [link-map]
  (map #(update % :type keyword) link-map))

(defn entities-by-uri
  "Returns all entities matched by the uri."
  [uri entity-type]
  (let [uri-info (part-uri uri)
        query-values (unpack-elastic (elastic/search entity-type 
                                                     {:identifier.aggregate-id (first uri-info)
                                                      :identifier.entity-id (second uri-info)}))]
    (if (= '() query-values)
      :missing
      (if (= entity-type :links)
        (keywordize-types query-values)
        query-values))))

(defn statements-by-uri
  "Return all versions of the desired statement or :missing if it can not be found."
  [uri]
  (entities-by-uri uri :statements))

(defn statements-by-author
  "Return all statements with a certain author."
  [author]
  (unpack-elastic (elastic/search :statements {:content.author author})))

(defn links-by-uri
  "Return all link-versions defined by the uri"
  [uri]
  (entities-by-uri uri :links))

(defn exact-statement
  "Return the exact statement and only that if possible."
  [aggregate-id entity-id version]
  (first (unpack-elastic (elastic/search :statements {:identifier.aggregate-id aggregate-id
                                                      :identifier.entity-id entity-id
                                                      :identifier.version version}))))

(defn all-statements
  "Returns all statements currently saved in the elasticsearch database."
  []
  (unpack-elastic (elastic/search :all-statements config/aggregate-name)))

(defn all-links
  "Returns all links currently saved in the elasticsearch database."
  []
  (keywordize-types (unpack-elastic (elastic/search :all-links config/aggregate-name))))

(defn insert-statement
  "Requires a map conforming to the ::aggregator.specs/statement as input. Inserts the statement into the database."
  [statement]
  (elastic/add-statement statement))

(defn exact-link
  "Return the exact link and only that if possible."
  ([aggregate-id entity-id version]
   (let [query-map {:identifier.aggregate-id aggregate-id
                    :identifier.entity-id entity-id
                    :identifier.version version}]
     (first (keywordize-types (unpack-elastic (elastic/search :links query-map))))))
  ([from-aggregate from-entity from-version to-aggregate to-entity to-version]
   (let [query-map {:source.aggregate-id from-aggregate :source.entity-id from-entity
                    :source.version from-version :destination.aggregate-id to-aggregate
                    :destination.entity-id to-entity :destination.version to-version}]
     (first (keywordize-types (unpack-elastic (elastic/search :links query-map)))))))

(defn insert-link
  "Requires a map conforming to the ::aggregator.specs/link as input. Inserts the statement into the database."
  [link]
  (elastic/add-link link))

(defn get-undercuts
  "Returns all undercuts that point to target aggregator and entity-id."
  [target-aggregator target-entity-id]
  (keywordize-types (unpack-elastic (elastic/search :links {:type "undercut"
                                                           :destination.aggregate-id target-aggregator
                                                           :destination.entity-id target-entity-id}))))

(defn links-by-target
  "Return all links with the corresponding target."
  [target-aggregator target-entity target-version]
  (keywordize-types (unpack-elastic (elastic/search :links {:destination.aggregate-id target-aggregator
                                                           :destination.entity-id target-entity
                                                           :destination.version target-version}))))

(defn random-statements
  "Return *num* random statements from the db."
  [num]
  (clojure.core/take
   num (shuffle (unpack-elastic
                 (elastic/search :statements
                                 {:identifier.aggregate-id config/aggregate-name})))))

(defn statements-contain
  "Return all statements from the elasticsearch-db where content.content-string containts `query`"
  [query]
  (unpack-elastic (elastic/search :statements-fuzzy query)))

