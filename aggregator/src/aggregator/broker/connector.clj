(ns aggregator.broker.connector
  (:require [clojure.spec.alpha :as s]
            [clojure.spec.gen.alpha :as gen]
            [langohr.core :as rmq]
            [langohr.channel :as lch]
            [langohr.basic :as lb]
            [langohr.exchange :as le]
            [langohr.queue :as lq]
            [aggregator.specs]))

(alias 'gspecs 'aggregator.specs)

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

(defn init-connection! []
  (create-connection!)
  (let [ch (lch/open @conn)]
    (-> ch
        (create-exchange exchange)
        (create-queue (:statement queues))
        (create-queue (:issue queues)))
    (lq/bind ch (:statement queues) exchange)
    (lq/bind ch (:issue queues) exchange)
    (lch/close ch)))


;; -----------------------------------------------------------------------------
;; Publishing

(defn- publish
  "Puts payload in a queue and handles channel for this command."
  [queue payload]
  (let [ch (lch/open @conn)]
    (lb/publish ch "" (get queues queue) payload)
    (lch/close ch)))

(defn publish-statement
  "Put a statement to the correct queue. Statement must conform spec."
  [statement]
  {:pre [(s/valid? ::gspecs/statement statement)]}
  (publish :statement statement))

(s/fdef publish-statement
        :args (s/cat :statement ::gspecs/statement))


;; -----------------------------------------------------------------------------
;; Testing-Area

(comment
  (init-connection!)
  (publish :statement "foo")
  (publish-statement "foo")
  )
