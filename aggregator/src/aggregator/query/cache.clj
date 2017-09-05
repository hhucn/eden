(ns aggregator.query.cache
  (:require [clojure.core.cache :as cache]
            [clojure.string :as str]))

;; This module handles the dynamic caching to improve the performance of the queries.

(def storage (atom (cache/lru-cache-factory {} :threshold 2000)))
(def links-storage (atom (cache/lru-cache-factory {} :threshold 2000)))

(defn hit-template
  "Touch the item in the cache and retrieve it."
  [store uri]
  (swap! store #(cache/hit % uri))
  (get (deref store) uri))

(defn miss-template
  "Signalize that an item was missing and fill in the missing value."
  [store uri statement]
  (swap! store #(cache/miss % uri statement))
  statement)

(defn cache-hit
  [uri]
  (hit-template storage uri))

(defn cache-hit-link
  [uri]
  (hit-template links-storage uri))

(defn cache-miss
  [uri statement]
  (miss-template storage uri statement))

(defn cache-miss-link
  [uri statement]
  (miss-template links-storage uri statement))

(defn retrieve-template
  "Try to retrieve an item from cache and trigger the appropriate events.
  Should always be followed by a filling of the value if possible."
  [store uri]
  (if (cache/has? (deref store) uri)
    (hit-template store uri)
    :missing))

(defn retrieve
  [uri]
  (retrieve-template storage uri))

(defn retrieve-link
  [uri]
  (retrieve-template links-storage uri))

(defn get-cached-statements
  "Retrieve all arguments currently in the cache"
  []
  @storage)

(defn get-cached-links
  []
  @links-storage)
