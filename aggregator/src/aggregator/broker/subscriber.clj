(ns aggregator.broker.subscriber
  (:require [clojure.core.async :as async :refer [go-loop <! timeout]]
            [langohr.core :as rmq]
            [langohr.channel :as lch]
            [langohr.consumers :as lcons]
            [taoensso.timbre :as log]
            [aggregator.broker.connector :as bc]
            [aggregator.broker.lib :as blib]
            [aggregator.broker.publish :as bpub]
            [aggregator.specs]))

(alias 'gspecs 'aggregator.specs)

(defn- make-handler
  [ch {:keys [type]} ^bytes payload]
  (log/debug (format "[consumer] Received a message: %s, delivery tag: %d, content type: %s, type: %s"
                     (String. payload "UTF-8") type))
  (println (format "[consumer] Received a message: %s, delivery tag: %d, content type: %s, type: %s"
                   (String. payload "UTF-8") type)))

(defn subscribe
  "Subscribe to queue and print output to Emacs Buffer *cider-repl localhost*."
  [entity]
  (let [conn (rmq/connect
              {:host "broker"
               :username "groot"
               :password "iamgroot"})
        ch (lch/open conn)
        queue (blib/get-queue-name entity)]
    (println (format "[main] Connected. Channel id: %d" (.getChannelNumber ch)))
    (println "Connecting to queue '" queue "'")
    (def queue queue)
    (go-loop []
      (<! (timeout 1000))
      #_(bc/create-queue ch entity)
      (lcons/subscribe ch "statement/update/hhu.de/42" make-handler {:auto-ack true})
      (recur))))


;; -----------------------------------------------------------------------------
;; Testing

(comment
  queue
  (def statement {:author "groot",
                  :content "iamgroot",
                  :aggregate-id "hhu.de",
                  :entity-id "42",
                  :version 1,
                  :created nil})
  (subscribe statement)
  (bpub/publish-statement statement)
  (let [ch (bc/open-channel)]
    (bc/create-queue ch statement)
    (bc/close-channel ch))
  )
