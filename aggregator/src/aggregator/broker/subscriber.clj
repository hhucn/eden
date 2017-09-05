(ns aggregator.broker.subscriber
  (:require [langohr.core :as rmq]
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
    (f meta p)))

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


(defmulti to-query (fn [meta _] (keyword (:type meta))))
(defmethod to-query :default [meta payload]
  (log/error "Could not dispatch type of received message: " meta payload))

(defmethod to-query :statement [_ statement]
  (when (lib/valid? ::gspecs/statement statement)
    (log/debug "Received a valid statement. Passing it to query-module...")
    (qupd/update-statement statement)))

(defmethod to-query :link [_ link]
  (when (lib/valid? ::gspecs/link link)
    (log/debug "Received a valid link. Passing it to query-module...")
    (qupd/update-link link)))


;; -----------------------------------------------------------------------------
;; Testing

(comment
  (subscribe "zeit.de" to-query)
  )
