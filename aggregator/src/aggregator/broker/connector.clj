(ns aggregator.broker.connector
  "General function to establish a connection to the local broker."
  (:require [taoensso.timbre :as log]
            [langohr.core :as rmq]
            [langohr.channel :as lch]
            [langohr.queue :as lq]
            [aggregator.broker.config :as bconf]
            [aggregator.utils.common :as lib]))

(def ^:private conn (atom nil))

(defn connected?
  "Check if connection to broker is established."
  [] (if (and @conn (not (rmq/closed? @conn))) true false))

(defmacro with-connection
  [error-msg & body]
  `(if (connected?)
     (do ~@body)
     (lib/return-error (str ~error-msg " Do you have a valid connection? Try (aggregator.broker.connector/init-connection!)."))))


;; -----------------------------------------------------------------------------
;; Connection-related

(defn- create-connection!
  "Read variables from environment and establish connection to the message
   broker."
  [] (reset! conn (rmq/connect {:host (System/getenv "BROKER_HOST")
                                :username (System/getenv "BROKER_USER")
                                :password (System/getenv "BROKER_PASS")})))

(defn init-connection!
  "Initializes connection to broker and creates an exchange."
  []
  (create-connection!)
  (log/debug "Connection to Message Broker established.")
  (lib/return-ok "Connection established."))

(defn close-connection!
  "Close connection to message broker."
  []
  (with-connection "Connection could not be closed."
    (rmq/close @conn)
    (reset! conn nil)
    (lib/return-ok "Connection closed.")))


;; -----------------------------------------------------------------------------
;; For the communication with the broker

(defn open-channel
  "Opens a channel for an existing connection to the broker."
  []
  (with-connection "Could not create channel."
    (lch/open @conn)))

(defn close-channel
  "Given a channel, close it!"
  [ch]
  (with-connection "Could not close channel."
    (when-not (lch/closed? ch)
      (lch/close ch)
      (lib/return-ok "Channel closed."))))

(defn create-queue
  "Creates a queue for a given aggregator. Uses aggregator as the queue name and
  binds this queue to the default exchange if no other is provided. Typically,
  aggregator is the hostname of the aggregator.

  Example:
  (create-queue \"hhu.de\")"
  ([aggregator exchange routing-key]
   (with-connection "Could not create queue."
     (try
       (let [ch (open-channel)
             queue-name aggregator
             expires-in-ms (* 30 60 1000)]
         (lq/declare ch queue-name {:arguments {"x-expires" expires-in-ms}})
         (lq/bind ch queue-name exchange {:routing-key routing-key})
         (close-channel ch)
         (lib/return-ok "Queue created." {:queue-name queue-name :expires-in-ms expires-in-ms}))
       (catch java.io.IOException e
         (lib/return-error "Could not create queue, caught IOException.")))))
  ([aggregator exchange]
   (create-queue aggregator exchange bconf/default-route))
  ([aggregator]
   (create-queue aggregator bconf/exchange bconf/default-route)))

(defn queue-exists?
  "Check if queue exists. Returns a Boolean when connection is established."
  [queue]
  (with-connection (format "Can't query existence of queue '%s'." queue)
    (try
      (let [ch (open-channel)]
        (lq/status ch queue)
        (close-channel ch)
        true)
      (catch java.io.IOException _e false))))

(defn delete-queue
  "Given a queue-name, delete it!"
  [queue]
  (with-connection "Could not delete queue."
    (when queue-exists?
      (let [ch (open-channel)]
        (lq/delete ch queue)
        (close-channel ch)
        (lib/return-ok "Queue deleted.")))))


;; -----------------------------------------------------------------------------
;; Entrypoint

(defn entrypoint []
  (init-connection!))
(entrypoint)


;; -----------------------------------------------------------------------------
;; Testing

(comment
  (init-connection!)
  (create-queue "welt.de")
  (delete-queue "welt.dey")
  (close-connection!)
  )
