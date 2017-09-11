(ns aggregator.broker.connector
  (:require [taoensso.timbre :as log]
            [langohr.core :as rmq]
            [langohr.channel :as lch]
            [langohr.queue :as lq]
            [aggregator.broker.config :as bconf]
            [aggregator.utils.common :as lib]))

(def ^:private conn (atom nil))

(defn connected?
  "Check if connection to broker is established."
  [] (and @conn (not (rmq/closed? @conn))))

(defmacro with-connection
  [error-msg & body]
  `(if (connected?)
     (do ~@body)
     (lib/return-error (str ~error-msg " Do you have a valid connection? Try (aggregator.broker.connector/init-connection!)."))))


;; -----------------------------------------------------------------------------

(defn- create-connection!
  "Read variables from environment and establish connection to the message
   broker."
  [] (reset! conn (rmq/connect {:host (System/getenv "BROKER_HOST")
                                :username (System/getenv "BROKER_USER")
                                :password (System/getenv "BROKER_PASS")})))

(defn open-channel
  "Opens a channel for an existing connection to the broker."
  [] (with-connection "Could not create channel."
       (lch/open @conn)))

(defn close-channel
  "Given a channel, close it!"
  [ch]
  (with-connection "Could not close channel."
    (when-not (lch/closed? ch)
      (lch/close ch))))

(defn create-queue
  "Creates a queue for a given entity. Extracts the original aggregator and the
  entity id from the provided entity."
  ([aggregator exchange routing-key]
   (with-connection "Could not create queue."
     (try
       (let [ch (open-channel)
             queue-name aggregator]
         (lq/declare ch queue-name {:durable true :auto-delete false :exclusive false})
         (lq/bind ch queue-name exchange {:routing-key routing-key})
         (close-channel ch)
         (lib/return-ok "Queue created."))
       (catch java.io.IOException e
         (lib/return-error "Could not create queue, caught IOException.")))))
  ([aggregator exchange]
   (create-queue aggregator exchange bconf/default-route))
  ([aggregator]
   (create-queue aggregator bconf/exchange bconf/default-route)))

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

(defn delete-queue
  "Given a queue-name, delete it!"
  [queue]
  (with-connection "Could not delete queue."
    (let [ch (open-channel)]
      (lq/delete ch queue)
      (close-channel ch)
      (lib/return-ok "Queue deleted."))))


;; -----------------------------------------------------------------------------
;; Testing

(comment
  (init-connection!)
  (create-queue "welt.dey")
  (open-channel)
  (close-connection!)
)
