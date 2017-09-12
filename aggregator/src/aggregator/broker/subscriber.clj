(ns aggregator.broker.subscriber
  (:require [langohr.core :as rmq]
            [langohr.channel :as lch]
            [langohr.consumers :as lcons]
            [taoensso.timbre :as log]
            [aggregator.utils.common :as lib]
            [aggregator.query.update :as qupd]
            [aggregator.specs])
  (:import [com.rabbitmq.client AuthenticationFailureException]))

(alias 'gspecs 'aggregator.specs)

(def exceptions
  {:auth-ex "Subscription failed, wrong credentials."
   :host-ex "Can't connect to remote RabbitMQ instance. Make sure you provided the correct URL."
   :queue-ex "The queue you are trying to connect to, does not exist. Did you mistype the name?"})

(defn- message-handler
  "Creates a handler, which is called on new messages. Converts the payload into
  EDN-format if possible (otherwise returns it as a string) and calls f/2 with
  it."
  [f ch meta ^bytes payload]
  (let [p (lib/json->edn (String. payload "UTF-8"))]
    (f meta p)))

(defn subscribe
  "Subscribe to queue and call a function f with meta-information and payload.

  Example:
  (subscribe (fn [meta payload] [meta payload])) \"hhu.de\" {:host \"broker\" :user \"groot\" :password \"iamgroot\"})"
  [f queue {:keys [host user password]}]
  (try
    (let [conn (rmq/connect
                {:host host
                 :username (or user (System/getenv "BROKER_USER"))
                 :password (or password (System/getenv "BROKER_PASS"))})
          ch (lch/open conn)]
      (lcons/subscribe ch queue (partial message-handler f) {:auto-ack true})
      (log/debug "Connected. Channel id:" (.getChannelNumber ch))
      (lib/return-ok "Connection to message queue established."))
    (catch AuthenticationFailureException e
      (log/debug (:auth-ex exceptions))
      (lib/return-error (:auth-ex exceptions)))
    (catch java.net.UnknownHostException e
      (log/debug (:host-ex exceptions))
      (lib/return-error (:host-ex exceptions)))
    (catch java.io.IOException e
      (log/debug (:queue-ex exceptions))
      (lib/return-error (:queue-ex exceptions)))))


(defmulti to-query (fn [meta _] (keyword (:type meta))))
(defmethod to-query :default [meta payload]
  (log/error "Could not dispatch type of received message: " meta payload))

(defmethod to-query :statement [_ statement]
  (when (lib/valid? ::gspecs/statement statement)
    (log/debug "Received a valid statement. Passing it to query-module...")
    (qupd/update-statement statement)))

(defmethod to-query :link [_ link]
  (let [nlink (assoc link :type (-> link :type keyword))]
    (when (lib/valid? ::gspecs/link nlink)
      (log/debug "Received a valid link. Passing it to query-module...")
      (qupd/update-link nlink))))


;; -----------------------------------------------------------------------------
;; Testing

(comment
  (connector/init-connection!)
  (subscribe to-query "iamgro.ot" {:host "broker" :user "groot" :password "iamgroot"})
  )
