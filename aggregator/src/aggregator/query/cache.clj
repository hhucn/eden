(ns aggregator.query.cache
  (:require [clojure.core.cache :as cache]
            [clojure.string :as str]))

;; This module handles the dynamic caching to improve the performance of the queries.

(def storage (atom (cache/lru-cache-factory {} :threshold 2000)))

(defn cache-hit
  "Touch the item in the cache and retrieve it."
  [uri]
  (swap! storage #(cache/hit % uri))
  (get @storage uri))

(defn cache-miss
  "Signalize that an item was missing and fill in the missing value."
  [uri statement]
  (swap! storage #(cache/miss % uri statement))
  (statement))

(defn retrieve
  "Try to retrieve an item from cache and trigger the appropriate events.
  Should always be followed by a filling of the value if possible."
  [uri]
  (if (cache/has? @storage uri)
    (cache-hit uri)
    :missing))

(defn get-cached-statements
  "Retrieve all arguments currently in the cache"
  []
  (into {} 
        (map (fn [[k v]] [(second (str/split k #"::")) v])
             (filter (fn [[k v]] (str/starts-with? k "dbas::")) @storage))))

