(ns aggregator.broker.subscriber
  (:require [clojure.core.async :as async :refer [go-loop <! timeout]]
            [langohr.core :as rmq]
            [langohr.channel :as lch]
            [langohr.consumers :as lcons]
            [taoensso.timbre :as log]
            [aggregator.utils.common :as lib]
            [aggregator.query.update :as qupd]
            [aggregator.specs]))

(alias 'gspecs 'aggregator.specs)

(defn- message-handler
  "Creates a handler, which is called on new messages. Converts the payload into
  data and calls f/2 with it."
  [f ch meta ^bytes payload]
  (let [p (lib/json->edn (String. payload "UTF-8"))]
    (log/debug (format "Received a message: %s" p))
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
    (log/debug "Connected. Channel id: " (.getChannelNumber ch))
    (lcons/subscribe ch queue (partial message-handler f) {:auto-ack true})))


(defn dispatch-query
  [entity {:keys [type]}]
  (let [entity-type (keyword type)]
    (case entity-type
      :statement (when (lib/valid? ::gspecs/statement entity) (qupd/update-statement entity))
      :link (when (lib/valid? ::gspecs/link entity) (qupd/update-link entity))
      :default (log/debug "Received invalid entity: " entity meta))))

;; -----------------------------------------------------------------------------
;; Testing

(comment
  (subscribe "zeit.de" dispatch-query)
  )
