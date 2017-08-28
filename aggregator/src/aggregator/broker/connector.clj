(ns aggregator.broker.connector
  (:require [clojure.tools.logging :as log]
            [langohr.core :as rmq]
            [langohr.channel :as lch]
            [langohr.exchange :as le]
            [langohr.queue :as lq]))

(def exchange "argweb")
(def queues
  {:statement "statement.update"
   :issue "issue.update"})


;; -----------------------------------------------------------------------------
;; Setup broker

(def ^:private conn (atom nil))

(defn- create-connection!
  "Read variables from environment and establish connection to the message
   broker."
  []
  (reset! conn (rmq/connect {:host "broker"
                             :username (System/getenv "BROKER_USER")
                             :password (System/getenv "BROKER_PASS")})))

(defn- create-exchange
  "Creates an Exchange in RabbitMQ with the type 'topic'."
  [channel exchange-name]
  (le/declare channel exchange-name "topic" {:durable true :auto-delete false :exclusive false})
  channel)

(defn- create-queue [channel queue-name]
  (lq/declare channel queue-name {:durable true :auto-delete false :exclusive false})
  channel)

(defn open-channel []
  (lch/open @conn))
(defn close-channel [ch]
  (lch/close ch))

(defn init-connection! []
  (create-connection!)
  (log/debug "Connection to Message Broker established.")
  (let [ch (open-channel)]
    (-> ch
        (create-exchange exchange)
        (create-queue (:statement queues))
        (create-queue (:issue queues)))
    (lq/bind ch (:statement queues) exchange)
    (lq/bind ch (:issue queues) exchange)
    (close-channel ch)))
