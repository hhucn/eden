(ns aggregator.broker.subscriber
  (:require [clojure.core.async :as async :refer [go-loop <! timeout]]
            [langohr.core :as rmq]
            [langohr.channel :as lch]
            [langohr.consumers :as lcons]
            [taoensso.timbre :as log]))

(defn message-handler
  [ch {:keys [content-type delivery-tag type]} ^bytes payload]
  (log/debug (format "[consumer] Received a message: %s, delivery tag: %d, content type: %s, type: %s"
                   (String. payload "UTF-8") delivery-tag content-type type))
  (println (format "[consumer] Received a message: %s, delivery tag: %d, content type: %s, type: %s"
                   (String. payload "UTF-8") delivery-tag content-type type)))

(defn subscribe
  "Subscribe to queue and print output to Emacs Buffer *cider-repl localhost*."
  [queue]
  (let [conn (rmq/connect
              {:host "broker"
               :username "groot"
               :password "iamgroot"})
        ch (lch/open conn)]
    (log/debug "[subscriber] Connected. Channel id: " (.getChannelNumber ch))
    (go-loop []
      (<! (timeout 1000))
      (lcons/subscribe ch queue message-handler {:auto-ack true})
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
  (subscribe "iamgro.ot")
  (bpub/publish-statement statement)
  (let [ch (bc/open-channel)]
    (bc/create-queue ch statement)
    (bc/close-channel ch))
  )
