(ns subscriber.core
  (:require [clojure.core.async :as async :refer [go-loop <!]]
            [langohr.core :as rmq]
            [langohr.channel :as lch]
            [langohr.queue :as lq]
            [langohr.consumers :as lcons]))

(defn start-consumer []
  (let [conn (rmq/connect
              {:host "localhost"
               :username "groot"
               :password "iamgroot"})
        ch (lch/open conn)
        queue-name (:queue (lq/declare ch "testname" {:exclusive false :auto-delete true}))
        handler (fn [ch {:keys [routing-key] :as meta} ^bytes payload]
                  (println (format "[consumer] got new statement")))]
    (lq/bind ch queue-name "argweb" {:routing-key "new.weltDE"})
    (lcons/subscribe ch queue-name handler {:auto-ack true})))

(defn -main [& args]
  (go-loop []
    (<! (timeout 1000))
    (start-consumer)
    (recur)))
