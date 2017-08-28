(ns aggregator.broker.publish
  (:require [clojure.spec.alpha :as s]
            [clojure.spec.gen.alpha :as gen]
            [langohr.basic :as lb]
            [aggregator.broker.connector :as connector]
            [aggregator.specs]))

(alias 'gspecs 'aggregator.specs)

(defn- publish
  "Puts payload in a queue and handles channel for this command.

  TODO: We are currently ignoring the Exchange. I think we do not need it
  here..."
  [queue payload]
  (let [ch (connector/open-channel)]
    (lb/publish ch "" (get connector/queues queue) payload)
    (connector/close-channel ch)))

(defn publish-statement
  "Put a statement to the correct queue. Statement must conform spec."
  [statement]
  {:pre [(s/valid? ::gspecs/statement statement)]}
  (publish :statement statement))

(s/fdef publish-statement
        :args (s/cat :statement ::gspecs/statement))


;; -----------------------------------------------------------------------------
;; Testing-Area

(comment
  (connector/init-connection!)
  (publish :statement "foo")
  (publish-statement "foo")
  )
