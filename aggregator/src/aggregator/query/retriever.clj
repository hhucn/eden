(ns aggregator.query.retriever
  (:require [aggregator.settings :as settings]))

(defn whitelisted?
  "Return whether the source of a link is whitelisted."
  [link]
  (some #{(:from_aggregate_id link)} settings/whitelist))

(defn next
  "Accepts a list and retrieves the statement the head-link is sourced by if its provider is whitelisted. Then retrieves all links connected to it and queues them. Returns the updated list."
  [queue]
  (if (whitelisted? (first queue))
    (let []
      :new-queue)
    (rest queue)))
