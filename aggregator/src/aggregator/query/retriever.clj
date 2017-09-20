(ns aggregator.query.retriever
  (:require [aggregator.config :as config]
            [aggregator.query.query :as query]
            [aggregator.query.cache :as cache]
            [taoensso.timbre :as log]))

(defn whitelisted?
  "Return whether the source of a link is whitelisted."
  [link]
  (some #{(:from_aggregate_id link)} config/whitelist))

(defn next
  "Accepts a list and retrieves the statement the head-link is sourced by if its provider is whitelisted. Then retrieves all links connected to it and queues them. Returns the updated list."
  [queue]
  (if (whitelisted? (first queue))
    (let [link (first queue)
          aggregate (:from_aggregate_id link)
          entity-id (:from_entity_id link)
          version (:from_version link)
          statement (query/retrieve-exact-statement aggregate entity-id version)
          undercuts (query/retrieve-undercuts link)
          additional-links (query/links-to statement)]
      (concat (rest queue) undercuts additional-links))
    (rest queue)))

(defn loop-next
  "Loop the next function with the queue until its empty."
  [queue]
  (loop [q queue]
    (let [next-step (next q)]
      (when next-step
        (recur next-step)))))

(defn lookup-related
  "Lookup all related links and statements 'downstream' from the starting statement. Runs in a separate thread and returns the future. Warning: dereferencing the future might block the system if the lookup is still going on."
  [statement]
  (log/debug (format "[retriever] Starting to pull related data for %s" statement))
  (let [startlinks (query/links-to statement)]
    (future (loop-next startlinks))))

(defn automatic-retriever
  "Starts an automatic retriever that looks up statements and links related to things contained in the cache."
  []
  (future
    (loop [starter (rand-nth (keys (cache/get-cached-statements)))]
      (lookup-related starter)
      (Thread/sleep 60000)
      (log/debug "[retriever] Automatic search waking up.")
      (recur (rand-nth (keys (cache/get-cached-statements)))))))

(defn bootstrap
  "Call this method when the aggregator starts. Pulls the whitelisted aggregators for a starting-set of arguments, puts them into the cache and then spins up the automatic retriever."
  []
  (query/remote-starter-set)
  (automatic-retriever))
