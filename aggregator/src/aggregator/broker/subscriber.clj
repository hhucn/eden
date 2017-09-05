(ns aggregator.broker.subscriber
  (:require [clojure.core.async :as async :refer [go-loop <! timeout]]
            [langohr.core :as rmq]
            [langohr.channel :as lch]
            [langohr.consumers :as lcons]
            [taoensso.timbre :as log]
            [aggregator.utils.common :as lib]))

(defn- message-handler
  "Creates a handler, which is called on new messages. Converts the payload into
  data and calls f/2 with it."
  [f ch meta ^bytes payload]
  (let [p (lib/json->edn (String. payload "UTF-8"))]
    (log/debug (format "[subscriber] Received a message: %s" p))
    (f p meta)))

(defn subscribe
  "Subscribe to queue and call a function f with the payload and meta-information.

  Example:
  (subscribe \"hhu.de\" (fn [payload] (println \"do something with: \" payload)))"
  [queue f]
  (let [conn (rmq/connect
              {:host "broker"
               :username "groot"
               :password "iamgroot"})
        ch (lch/open conn)]
    (log/debug "[subscriber] Connected. Channel id: " (.getChannelNumber ch))
    (go-loop []
      (<! (timeout 1000))
      (lcons/subscribe ch queue (partial message-handler f) {:auto-ack true})
      (recur))))


;; -----------------------------------------------------------------------------
;; Testing

(comment
  (subscribe "welt.de" (fn [payload meta] (println "i am groot" payload meta)))
  )
