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

(defn entity-by-uri
  "Returns all entities matched by the uri."
  [uri entity-type]
  (let [uri-info (part-uri uri)
        query-value (elastic/search entity-type {:aggregate-id (first uri-info)
                                                 :entity-id (second uri-info)})]
    (if (= '() query-value)
      :missing
      query-value)))

(defn statements-by-uri
  "Return all versions of the desired statement or :missing if it can not be found."
  [uri]
  (entity-by-uri uri :statements))

(defn statements-by-author
  "Return all statements with a certain author."
  [author]
  (elastic/search :links {:author author}))

(defn links-by-uri
  "Return all link-versions defined by the uri"
  [uri]
  (entity-by-uri uri :links))

(defn exact-statement
  "Return the exact statement and only that if possible."
  [aggregate-id entity-id version]
  (elastic/search :statements {:aggregate-id aggregate-id
                                           :entity-id entity-id
                                           :version version}))

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
        db-result (elastic/search :links query)]
    db-result))

(defn insert-link
  "Requires a map conforming to the ::aggregator.specs/link as input. Inserts the statement into the database."
  [link]
  (elastic/add-link link))

(defn get-undercuts
  "Returns all undercuts that point to target aggregator and entity-id."
  [target-aggregator target-entity-id]
  (elastic/search :links {:type "undercut"
                          :to-aggregate-id target-aggregator
                          :to-entity-id target-entity-id}))

(defn links-by-target
  "Return all links with the corresponding target."
  [target-aggregator target-entity target-version]
  (elastic/search :links {:to-aggregate-id target-aggregator
                          :to-entity-id target-entity
                          :to-version target-version}))

(defn random-statements
  "Return *num* random statements from the db."
  [num]
  (clojure.core/take
   num (shuffle (elastic/search :statements {:aggregate-id config/aggregate-name}))))
