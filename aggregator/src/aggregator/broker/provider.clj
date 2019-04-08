(ns aggregator.broker.provider
  (:require [aggregator.config :as config]))

(defn subscribe-to-queue
  "Subscribe to a certain queue for aggregator"
  [queue aggregator]
  (if (empty? (get-in @config/app-state [:broker :queues queue]))
    (swap! config/app-state assoc-in [:broker :queues queue] #{}))
  (swap! config/app-state update-in [:broker :queues queue] conj aggregator))

(defn get-subscriptions
  "Get the current queue-subscriptions"
  [queue]
  (get-in @config/app-state [:broker :queues queue]))

(defn remove-subscription
  "Remove subscription for specific queue"
  [queue aggregator]
  (swap! config/app-state update-in [:broker :queues queue] disj aggregator))

(defmulti pull-new identity
  "Pulls new objects from several supported queues.")

(defmethod pull-new :statements
  [_]
  (loop [to-do (get-subscriptions "statements")]
    (when (seq to-do)
      (query/remote-statements-since
       (first to-do)
       (quot (System/currentTimeMillis) 1000))
      (recur (rest to-do)))))

(defmethod pull-new :links
  [_]
  (loop [to-do (get-subscriptions "links")]
    (when (seq to-do)
      (query/remote-links-since
       (first to-do)
       (quot (System/currentTimeMillis) 1000))
      (recur (rest to-do)))))

(defn entrypoint
  "Call this on programm-start."
  ;; TODO start the thread here
  []
  nil)
