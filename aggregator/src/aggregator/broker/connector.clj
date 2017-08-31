(ns aggregator.broker.connector
  (:require [clojure.tools.logging :as log]
            [langohr.core :as rmq]
            [langohr.channel :as lch]
            [langohr.exchange :as le]
            [langohr.queue :as lq]
            [aggregator.broker.lib :as blib]))

(def exchange "argweb")

;; -----------------------------------------------------------------------------
;; Setup broker

(def ^:private conn (atom nil))

(defn- create-connection!
  "Read variables from environment and establish connection to the message
   broker."
  []
  (reset! conn (rmq/connect {:host (System/getenv "BROKER_HOST")
                             :username (System/getenv "BROKER_USER")
                             :password (System/getenv "BROKER_PASS")})))

(defn- create-exchange
  "Creates an Exchange in RabbitMQ with the type 'topic'."
  [channel exchange-name]
  (le/declare channel exchange-name "topic" {:durable true :auto-delete false :exclusive false})
  channel)

(defn create-queue
  "Creates a queue for a given entity. Extracts the original aggregator and the
  entity id from the provided entity."
  [channel entity]
  (let [queue-name (blib/get-queue-name entity)]
    (lq/declare channel queue-name {:durable true :auto-delete false :exclusive false})
    (lq/bind channel queue-name exchange))
  channel)

(defn open-channel
  "Opens a channel for an existing connection to the broker."
  []
  (lch/open @conn))

(defn close-channel
  "Given a channel, close it!"
  [ch]
  (lch/close ch))

(defn init-connection!
  "Initializes connection to broker and creates an exchange."
  []
  (create-connection!)
  (log/debug "Connection to Message Broker established.")
  (let [ch (open-channel)]
    (-> ch
        (create-exchange exchange)
        (close-channel))))
