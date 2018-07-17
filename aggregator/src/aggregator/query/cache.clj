(ns aggregator.query.cache
  (:require [clojure.core.cache :as cache]
            [clojure.string :as str]
            [taoensso.timbre :as log]))

;; This module handles the dynamic caching to improve the performance of the queries.

(def storage (atom (cache/lru-cache-factory {} :threshold 2000)))
(def links-storage (atom (cache/lru-cache-factory {} :threshold 2000)))

(defn- hit-template
  "Touch the item in the cache and retrieve it."
  [store uri]
  (swap! store #(cache/hit % uri))
  (get (deref store) uri))

(defn- miss-template
  "Signalize that an item was missing and fill in the missing value."
  [store uri statement]
  (swap! store #(cache/miss % uri statement))
  (log/debug "[Cache] Added item to cache " statement)
  statement)

(defn cache-hit
  "Touch the statement in the cache and retrieve it."
  [uri]
  (hit-template storage uri))

(defn cache-hit-link
  "Touch the link in the link-cache and retrieve it."
  [uri]
  (hit-template links-storage uri))

(defn cache-miss
  "Signalize to the caching engine that the statement is missing."
  [uri statement]
  (miss-template storage uri statement))

(defn cache-miss-link
  "Signalize to the caching engine that the link is missing in the link-cache."
  [uri statement]
  (miss-template links-storage uri statement))

(defn- retrieve-template
  "Try to retrieve an item from cache and trigger the appropriate events.
  Should always be followed by a filling of the value if possible."
  [store uri]
  (if (cache/has? (deref store) uri)
    (hit-template store uri)
    :missing))

(defn retrieve
  "Retrieve the statement if it is present in the cache. Otherwise the function returns `:missing`.
  If `:missing` is returned it is best practice to trigger the `cache-miss` method."
  [uri]
  (retrieve-template storage uri))

(defn retrieve-link
  "Retrieve the link if it is present in the link-cache. Otherwise the function returns `:missing`.
  If `:missing` is returned it is best practice to trigger the `cache-miss-link` method."
  [uri]
  (retrieve-template links-storage uri))

(defn get-cached-statements
  "Retrieve all arguments currently in the cache"
  []
  @storage)

(defn get-cached-links
  "Returns all links currently in the link-cache."
  []
  @links-storage)
