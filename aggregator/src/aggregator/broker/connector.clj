(ns aggregator.broker.connector
  (:require [langohr.core :as rmq]
            [langohr.channel :as lch]
            [langohr.basic :as lb]
            [langohr.exchange :as le]))

(defn- create-connection
  "Read variables from environment and establish connection to the message
   broker."
  []
  (rmq/connect
   {:host "broker"
    :username (System/getenv "BROKER_USER")
    :password (System/getenv "BROKER_PASS")}))

(defn- startup-exchange
  "Creates an Exchange in RabbitMQ with the type 'topic'."
  [exchange]
  (let [conn (create-connection)
        ch (lch/open conn)]
    (le/declare ch exchange "topic" {:durable true :auto-delete false :exclusive false})
    ch))

(defn- publish-statement [ch exchange statement routing-key]
  (lb/publish ch exchange routing-key statement {:content-type "application/json"
                                                 :type "statement.new"}))

(defn publish-new-statement
  "Publishs a statement in the broker."
  [{:keys [aggregator-id local-id content] :as statement}]
  (let [ch (startup-exchange "argweb")]
    (publish-statement ch "argweb" (byte-array 4) (format "new.%s" aggregator-id))))

;; (publish-new-statement {:aggregator-id 1 :local-id 42 :content {:whoami :iamgroot}})
