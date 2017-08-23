(ns aggregator.query.cache
  (:require [clojure.core.cache :as cache]
            [clojure.string :as str]
            [argapi.query :as query]))

;; This module handles the dynamic caching to improve the performance of the queries.

(defn cache-hit
  "Touch the item in the cache and retrieve it."
  [uri]
  (swap! storage #(cache/hit % uri))
  (get @storage uri))

(defn cache-miss
  "Retrieve the item on the long way if possible and then add it to the cache before delivering it."
  [uri]
  (let [statement (query/query uri)]
    (swap! storage #(cache/miss % uri statement)))
  (get @storage uri))

(defn get-statement
  "Retrieve statements (currently only of the dbas:: domain)"
  [uri]
  (if (cache/has? @storage uri)
    (cache-hit uri)
    (cache-miss uri)))

                                        ;(get-statement "dbas::2")
                                        ;(get-statement "spon.de::35")

(defn get-cached-statements
  "Retrieve all arguments currently in the cache"
  []
  (into {} 
        (map (fn [[k v]] [(second (str/split k #"::")) v])
             (filter (fn [[k v]] (str/starts-with? k "dbas::")) @storage))))

