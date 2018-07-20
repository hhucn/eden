(ns aggregator.query.retriever
  (:require [aggregator.config :as config]
            [aggregator.query.query :as query]
            [aggregator.query.cache :as cache]
            [taoensso.timbre :as log]))

(defn whitelisted?
  "Return whether the source of a link is whitelisted."
  [link]
  (some #{(get-in link [:source :aggregate-id])} config/whitelist))

(defn next-entity
  "Accepts a list and retrieves the statement the head-link is sourced by if its provider is whitelisted. Then retrieves all links connected to it and queues them. Returns the updated list."
  [queue]
  (if (whitelisted? (first queue))
    (let [link (first queue)
          aggregate (get-in link [:source :aggregate-id])
          entity-id (get-in link [:source :entity-id])
          version (get-in link [:source :link])
          statement (query/retrieve-exact-statement aggregate entity-id version)
          undercuts (query/retrieve-undercuts aggregate entity-id)
          additional-links (query/links-to statement)]
      (concat (rest queue) undercuts additional-links))
    (rest queue)))

(defn loop-next
  "Loop the next function with the queue until its empty."
  [queue]
  (loop [q queue]
    (let [next-step (next-entity q)]
      (when (seq next-step)
        (recur next-step)))))

(defn lookup-related
  "Lookup all related links and statements 'downstream' from the starting statement."
  [statement]
  (log/debug (format "[retriever] Starting to pull related data for %s" statement))
  (let [startlinks (query/links-to statement)]
    (loop-next startlinks)))

(defn automatic-retriever
  "Starts an automatic retriever that looks up statements and links related to things contained in the cache."
  []
  (future
    (loop [starter (rand-nth (keys (cache/get-cached-statements)))]
      (lookup-related starter)
      (log/debug "[retriever] sleeping")
      (Thread/sleep 60000)
      (log/debug "[retriever] Automatic search waking up.")
      (query/remote-starter-set)
      (recur (rand-nth (keys (cache/get-cached-statements)))))))

(defn bootstrap
  "Call this method when the aggregator starts. Pulls the whitelisted aggregators
  for a starting-set of arguments, puts them into the cache and then spins up
  the automatic retriever."
  []
  (log/debug "PULLING related starter-set")
  (query/all-remote-statements)
  (query/all-remote-links)
  (log/debug "Pulled a random starter set from whitelisted aggregators successfully.")
  (automatic-retriever))
