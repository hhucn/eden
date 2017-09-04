(ns aggregator.broker.subscriber
  (:require [clojure.core.async :as async :refer [go-loop <! timeout]]
            [langohr.core :as rmq]
            [langohr.channel :as lch]
            [langohr.consumers :as lcons]
            [taoensso.timbre :as log]
            [aggregator.utils.common :as lib]))

(defn- message-handler
  [ch _ ^bytes payload]
  (log/debug (format "[subscriber] Received a message: %s"
                     (lib/json->edn (String. payload "UTF-8")))))

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
  (subscribe "nobo.dy")
  )
