(ns aggregator.broker.connector
  (:require [taoensso.timbre :as log]
            [langohr.core :as rmq]
            [langohr.channel :as lch]
            [langohr.queue :as lq]
            [aggregator.broker.config :as bconf]))

;; -----------------------------------------------------------------------------
;; Setup broker

(def ^:private conn (atom nil))

(defn- create-connection!
  "Read variables from environment and establish connection to the message
   broker."
  [] (reset! conn (rmq/connect {:host (System/getenv "BROKER_HOST")
                                :username (System/getenv "BROKER_USER")
                                :password (System/getenv "BROKER_PASS")})))

(defn open-channel
  "Opens a channel for an existing connection to the broker."
  [] (lch/open @conn))

(defn close-channel
  "Given a channel, close it!"
  [ch] (lch/close ch))

(defn create-queue
  "Creates a queue for a given entity. Extracts the original aggregator and the
  entity id from the provided entity."
  ([aggregator exchange routing-key]
   (let [ch (open-channel)
         queue-name aggregator]
     (lq/declare ch queue-name {:durable true :auto-delete false :exclusive false})
     (lq/bind ch queue-name exchange {:routing-key routing-key})
     (close-channel ch)))
  ([aggregator exchange]
   (create-queue aggregator exchange bconf/default-route))
  ([aggregator]
   (create-queue aggregator bconf/exchange bconf/default-route)))

(defn init-connection!
  "Initializes connection to broker and creates an exchange."
  []
  (create-connection!)
  (log/debug "Connection to Message Broker established.")
  #_(let [ch (open-channel)]
    (-> ch
        (close-channel))))


;; -----------------------------------------------------------------------------
;; Testing

(comment
  (create-queue "welt.de")
)
