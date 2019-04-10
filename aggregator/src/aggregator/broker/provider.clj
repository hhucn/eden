(ns aggregator.broker.provider
  (:require [aggregator.config :as config]
            [aggregator.query.query :as query]
            [aggregator.utils.common :as utils]
            [taoensso.timbre :as log]))

(defn subscribe-to-queue!
  "Subscribe to a certain queue for aggregator"
  [queue aggregator]
  (log/debug (format "Subscribing %s to queue %s" aggregator queue))
  (if (empty? (get-in @config/app-state [:broker :queues queue]))
    (swap! config/app-state assoc-in [:broker :queues queue] #{}))
  (swap! config/app-state update-in [:broker :queues queue] conj aggregator))

(defn get-subscriptions
  "Get the current queue-subscriptions"
  [queue]
  (get-in @config/app-state [:broker :queues queue]))

(defn remove-subscription!
  "Remove subscription for specific queue"
  [queue aggregator]
  (log/debug (format "Deleting %s from queue %s" aggregator queue))
  (swap! config/app-state update-in [:broker :queues queue] disj aggregator))

(defn- set-timestamp!
  [queue aggregator timestamp]
  (swap! config/app-state assoc-in [:broker :timestamps queue aggregator] timestamp))

(defn- last-timestamp!
  "Get the latest timestamp. If none is provided return 0."
  [queue aggregator]
  (or
   (get-in @config/app-state [:broker :timestamps queue aggregator])
   0))

(defmulti pull-new! identity)

(defmethod pull-new! :statements
  [_]
  (loop [to-do (get-subscriptions "statements")]
    (when (seq to-do)
      (log/debug (format "querying links for timestamp %s on aggregator %s" (last-timestamp! "statements" (first to-do)) (first to-do)))
      (query/remote-statements-since (first to-do) (last-timestamp! "statements" (first to-do)))
      (set-timestamp! "statements" (first to-do) (utils/time-now-str))
      (recur (rest to-do)))))

(defmethod pull-new! :links
  [_]
  (loop [to-do (get-subscriptions "links")]
    (when (seq to-do)
      (log/debug (format "querying links for timestamp %s on aggregator %s" (last-timestamp! "links" (first to-do)) (first to-do)))
      (query/remote-links-since (first to-do) (last-timestamp! "links" (first to-do)))
      (set-timestamp! "links" (first to-do) (utils/time-now-str))
      (recur (rest to-do)))))


(defn- start-background-pull!
  []
  (future
    (loop [queues-to-pull [:statements :links]]
      (log/debug (format "Pulling queues %s" queues-to-pull))
      (doseq [queue queues-to-pull]
        (pull-new! queue)
        ;; Sleep between queues
        (Thread/sleep 1000))
      ;; Sleep one Minute between full pulls
      (Thread/sleep 60000)
      (recur queues-to-pull))))

(defn entrypoint
  "Call this on programm-start."
  []
  ;; Set any neccesary preparations before the service launches here.
  (doseq [aggregator config/whitelist]
    (log/debug (format "Subscribing to queue statements for aggregator %s" aggregator))
    (subscribe-to-queue! "statements" aggregator)
    (log/debug (format "Subscribing to queue links for aggregator %s" aggregator))
    (subscribe-to-queue! "links" aggregator))
  (start-background-pull!)
  (log/debug "Initializing broker done."))
