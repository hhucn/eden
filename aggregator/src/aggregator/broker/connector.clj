(ns aggregator.broker.connector
  "General function to establish a connection to the local broker."
  (:require [taoensso.timbre :as log]
            [langohr.core :as rmq]
            [langohr.channel :as lch]
            [langohr.queue :as lq]
            [aggregator.broker.config :as bconf]
            [aggregator.config :as config]
            [aggregator.utils.common :as lib]))

(defn broker-data
  "Helper shortcut to get the broker-data from the state."
  [name port]
  (get-in @config/app-state [:broker-info name port]))

(defn connected?
  "Check if connection to broker is established. When no broker-name is given, the local broker is checked. If connection is established return connection, otherwise nil."
  ([broker-name broker-port]
   (:conn (broker-data broker-name broker-port)))
  ([broker-name]
   (connected? broker-name 5672))
  ([]
   (connected? (System/getenv "BROKER_HOST"))))

#_(defmacro with-connection
  "Executes the body if the local connection is established.
  Returns the error message otherwise."
  [error-msg & body]
  `(if (connected?)
     (do ~@body)
     (lib/return-error (str ~error-msg " Do you have a valid connection? Try (aggregator.broker.connector/init-connection!)."))))


;; -----------------------------------------------------------------------------
;; Connection-related

(defn- create-connection!
  "Read variables from environment and establish connection to the message
  broker."
  ([hostname]
   (try (Integer/parseInt (System/getenv "BROKER_PORT"))
        (catch Exception e
          (do
            (log/warn "BROKER_PORT Variable not set")
            (create-connection! hostname 5672)))))
  ([hostname port]
   (rmq/connect {:host hostname
                 :username (System/getenv "BROKER_USER")
                 :password (System/getenv "BROKER_PASS")
                 :port port})))

(defn get-connection!
  "Opens a connection to a remote broker. If the connection is already opened, returns the opened connection.
  Connections are stored within the app-state inside the config-module."
  ([broker-name port]
   (let [conn (:conn (broker-data broker-name port))]
     (or conn
         (let [new-conn (create-connection! broker-name port)]
           (swap! config/app-state assoc-in [:broker-info broker-name port :conn] new-conn)
           new-conn))))
  ([broker-name]
   (get-connection! broker-name 5672))
  ([]
   (get-connection! (System/getenv "BROKER_HOST"))))

(defn init-local-connection!
  "Initializes connection to local broker and creates an exchange."
  []
  (get-connection! (System/getenv "BROKER_HOST") 5672)
  (log/debug "Connection to Message Broker established.")
  (lib/return-ok "Connection established."))

(declare close-all-channels!)
(defn close-connection!
  "Closes a connection for a given broker and returns :ok. If the connection is already closed, return nil."
  ([broker-name]
   (close-connection! broker-name 5672))
  ([broker-name port]
   (when-let [conn (:conn (broker-data broker-name port))]
     (close-all-channels! broker-name)
     (rmq/close conn)
     (swap! config/app-state assoc-in [:broker-info broker-name port :conn] nil)
     :ok)))

(defn close-local-connection!
  "Close connection to the local message broker."
  []
  (close-connection! (System/getenv "BROKER_HOST"))
  (lib/return-ok "Connection closed."))


;; -----------------------------------------------------------------------------
;; For the communication with the broker

(defn open-channel!
  "Opens a channel returns it.
  Keep in mind that you need to close this connection when finished."
  ([broker-name]
   (when-let [connection (get-connection! broker-name)]
     (lch/open connection)))
  ([] (open-channel! (System/getenv "BROKER_HOST"))))

(defn close-channel!
  "Given a channel, close it!"
  [ch]
  (when-not (lch/closed? ch)
    (lch/close ch)
    (lib/return-ok "Channel closed.")))

(defn close-all-channels!
  "Closes all known channels for a certain broker."
  [broker-name]
  (let [channels (:subscriptions (broker-data broker-name 5672))]
    (run! (fn [[_ chan]] (lch/close chan)) channels)
    (swap! config/app-state assoc-in [:broker-info broker-name 5672 :subscriptions] {})))

(defn create-queue
  "Creates a queue for a given aggregator. Uses aggregator as the queue name and
  binds this queue to the default exchange if no other is provided. Typically,
  aggregator is the hostname of the aggregator.

  Example:
  (create-queue \"statements\")"
  ([queue-name exchange]
   (let [ch (open-channel!)]
     (try
       (lq/declare ch queue-name {:durable true
                                  :auto-delete false})
       (lq/bind ch queue-name exchange {:routing-key queue-name})
       (log/debug "Created queue:" queue-name)
       (lib/return-ok "Queue created." {:queue-name queue-name})
       (catch java.io.IOException _e
         (log/error "Could not create queue" queue-name)
         (lib/return-error "Could not create queue, caught IOException."))
       (finally (close-channel! ch)))))
  ([queue-name]
   (create-queue queue-name bconf/exchange)))

(defn queue-exists?
  "Check if queue exists. Returns a Boolean when connection is established."
  [queue]
  (let [_ (get-connection!)
        ch (open-channel!)]
    (try
      (lq/status ch queue)
      true
      (catch java.io.IOException _e false)
      (finally (close-channel! ch)))))

(defn delete-queue
  "Given a queue-name, delete it!"
  [queue]
  (let [_ (get-connection!)
        ch (open-channel!)]
    (when queue-exists?
      (lq/delete ch queue)
      (close-channel! ch)
      (lib/return-ok "Queue deleted."))))


;; -----------------------------------------------------------------------------
;; Entrypoint

(defn entrypoint []
  (init-local-connection!)
  (create-queue "statements")
  (create-queue "links"))
;;(entrypoint)


;; -----------------------------------------------------------------------------
;; Testing

(comment
  (init-connection!)
  (create-queue "statements")
  (create-queue "links")
  (delete-queue "links")
  (close-connection!)
  )
