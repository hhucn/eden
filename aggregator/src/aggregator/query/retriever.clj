(ns aggregator.query.retriever
  (:require [aggregator.settings :as settings]
            [aggregator.query.query :as query]
            [aggregator.query.cache :as cache]))

(defn whitelisted?
  "Return whether the source of a link is whitelisted."
  [link]
  (some #{(:from_aggregate_id link)} settings/whitelist))

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
      (if next-step
        (recur next-step)
        nil))))

(defn lookup-related
  "Lookup all related links and statements 'downstream' from the starting statement. Runs in a separate thread and returns the future. Warning: dereferencing the future might block the system if the lookup is still going on."
  [statement]
  (let [startlinks (query/links-to statement)]
    (future (loop-next startlinks))))

(defn automatic-retriever
  "Starts an automatic retriever that looks up statements and links related to things contained in the cache.")

(rand-nth (keys {:foo :bar :baz :buf}))
