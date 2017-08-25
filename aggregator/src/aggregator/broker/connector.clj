(ns aggregator.broker.connector
  (:require [clojure.spec.alpha :as s]
            [clojure.spec.gen.alpha :as gen]
            [langohr.core :as rmq]
            [langohr.channel :as lch]
            [langohr.basic :as lb]
            [langohr.exchange :as le]
            [langohr.queue :as lq]))

(alias 'gspecs 'aggregator.specs)

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
  (-> (lch/open @conn)
      (create-exchange "argweb")
      (create-queue "statement.id.*")
      (create-queue "issue.id.*")
      lch/close)
  )

(defn- publish []
  (let [ch (lch/open @conn)]
    (lb/publish ch "argweb" "awesomequeue" "")
    (lch/close ch)))

(defn fuck-up-rabbitmq []
  (dotimes [_ 100000]
    (let [ch (lch/open @conn)]
      (create-queue ch (gen/generate (s/gen string?)))
      (lch/close ch))))

(comment
  (init-connection!)
  (fuck-up-rabbitmq)
  )

;; -----------------------------------------------------------------------------
;; Publishing

(defn- publish-statement [ch exchange statement routing-key]
  (lb/publish ch exchange routing-key statement {:content-type "application/json"
                                                 :type "statement.new"}))

(defn publish-new-statement
  "Publishs a statement in the broker."
  [{:keys [aggregator-id local-id content] :as statement}]
  (let [ch (create-exchange "argweb")]
    (publish-statement ch "argweb" (byte-array 4) (format "new.%s" aggregator-id))))

(comment
  (create-exchange "argweb")
  (publish-new-statement {:aggregator-id "weltDE" :local-id 42 :content {:whoami :iamgroot}})
  )

(s/exercise ::gspecs/statement)



