(ns aggregator.broker.publish
  (:require [clojure.spec.alpha :as s]
            [clojure.data.json :as json]
            [langohr.basic :as lb]
            [aggregator.broker.connector :as connector]
            [aggregator.utils.common :as lib]
            [aggregator.broker.config :as bconf]
            [aggregator.specs]))

(alias 'gspecs 'aggregator.specs)

(defn- publish
  "Create queue for entity and publish it on this queue."
  [payload routing-key]
  (let [ch (connector/open-channel)]
    (try
      (lb/publish ch bconf/exchange routing-key (json/write-str payload))
      (catch Throwable t
        (.printStackTrace t))
      (finally
        (connector/close-channel ch)))))

(defn publish-statement
  "Put a statement to the correct queue. Statement must conform spec to be
  published."
  [statement]
  (when (lib/valid? ::gspecs/statement statement)
    (publish statement bconf/default-route)))

(s/fdef publish-statement
        :args (s/cat :statement ::gspecs/statement))


;; -----------------------------------------------------------------------------
;; Testing-Area

(comment
  (def statement (ffirst (s/exercise ::gspecs/statement)))
  (connector/init-connection!)
  (publish {:iam :groot} bconf/default-route)
  (publish-statement statement)
  (blib/get-queue-name statement)
  (json/write-str statement)
  )
