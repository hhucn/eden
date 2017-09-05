(ns aggregator.query.retriever
  (:require [aggregator.settings :as settings]
            [aggregator.query.query :as query]
            [aggregator.query.update :as update]))

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
          undercuts (query/remote-undercuts link)
          additional-links (query/links-to statement)]
      :construct result-queue
      :return-queue
      )
    (rest queue)))
