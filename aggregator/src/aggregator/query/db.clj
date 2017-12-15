(ns aggregator.query.db
  (:use [korma.db]
        [korma.core])
  (:require [clojure.string :as str]
            [aggregator.config :as config]
            [aggregator.search.core :as elastic]))

(defn part-uri
  [uri]
  (let [split-uri (str/split uri #"/")]
    [(first split-uri) (second split-uri)]))

(defn unpack-elastic
  "Unpack an elasticsearch response to a list of entities."
  [response]
  (map :_source (get-in response [:data :hits])))

(defn entities-by-uri
  "Returns all entities matched by the uri."
  [uri entity-type]
  (let [uri-info (part-uri uri)
        query-values (unpack-elastic (elastic/search entity-type {:aggregate-id (first uri-info)
                                                                  :entity-id (second uri-info)}))]
    (if (= '() query-values)
      :missing
      query-values)))

(defn statements-by-uri
  "Return all versions of the desired statement or :missing if it can not be found."
  [uri]
  (entities-by-uri uri :statements))

(defn statements-by-author
  "Return all statements with a certain author."
  [author]
  (unpack-elastic (elastic/search :statements {:author author})))

(defn links-by-uri
  "Return all link-versions defined by the uri"
  [uri]
  (entities-by-uri uri :links))

(defn exact-statement
  "Return the exact statement and only that if possible."
  [aggregate-id entity-id version]
  (first (unpack-elastic (elastic/search :statements {:aggregate-id aggregate-id
                                                      :entity-id entity-id
                                                      :version version}))))

(defn all-statements
  []
  (unpack-elastic (elastic/search :all-statements config/aggregate-name)))

(defn insert-statement
  "Requires a map conforming to the ::aggregator.specs/statement as input. Inserts the statement into the database."
  [statement]
  (elastic/add-statement statement))

(defn exact-link
  "Return the exact link and only that if possible."
  [from-aggregate from-entity from-version to-aggregate to-entity & [to-version]]
  (let [query-map {:from-aggregate-id from-aggregate :from-entity-id from-entity
                   :from-version from-version :to-aggregate-id to-aggregate
                   :to-entity-id to-entity}
        query (if to-version (assoc query-map :to-version to-version) query-map)
        db-result (first (unpack-elastic (elastic/search :links query)))]
    db-result))

(defn insert-link
  "Requires a map conforming to the ::aggregator.specs/link as input. Inserts the statement into the database."
  [link]
  (elastic/add-link link))

(defn get-undercuts
  "Returns all undercuts that point to target aggregator and entity-id."
  [target-aggregator target-entity-id]
  (unpack-elastic (elastic/search :links {:type "undercut"
                                          :to-aggregate-id target-aggregator
                                          :to-entity-id target-entity-id})))

(defn links-by-target
  "Return all links with the corresponding target."
  ([target-aggregator target-entity]
   (unpack-elastic (elastic/search :links {:to-aggregate-id target-aggregator
                                           :to-entity-id target-entity})))
  ([target-aggregator target-entity target-version]
   (unpack-elastic (elastic/search :links {:to-aggregate-id target-aggregator
                                           :to-entity-id target-entity
                                           :to-version target-version}))))

(defn random-statements
  "Return *num* random statements from the db."
  [num]
  (clojure.core/take
   num (shuffle (unpack-elastic (elastic/search :statements {:aggregate-id config/aggregate-name})))))
