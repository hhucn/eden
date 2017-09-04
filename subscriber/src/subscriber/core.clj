(ns subscriber.core
  (:require [clojure.core.async :as async :refer [go-loop <! timeout]]
            [langohr.core :as rmq]
            [langohr.channel :as lch]
            [langohr.consumers :as lcons]))

(defn message-handler
  [ch {:keys [content-type delivery-tag type]} ^bytes payload]
  (println (format "[consumer] Received a message: %s, delivery tag: %d, content type: %s, type: %s"
                   (String. payload "UTF-8") delivery-tag content-type type)))

(defn subscribe
  "Subscribe to queue and print output to Emacs Buffer *cider-repl localhost*."
  [queue]
  (let [conn (rmq/connect
              {:host "localhost"
               :username "groot"
               :password "iamgroot"})
        ch (lch/open conn)]
    (println (format "[main] Connected. Channel id: %d" (.getChannelNumber ch)))
    (go-loop []
      (<! (timeout 1000))
      (lcons/subscribe ch queue message-handler {:auto-ack true})
      (recur))))

(defn -main [& args]
  (subscribe "faz.net")
  )
