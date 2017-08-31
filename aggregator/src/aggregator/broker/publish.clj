(ns aggregator.broker.publish
  (:require [clojure.spec.alpha :as s]
            [clojure.data.json :as json]
            [langohr.basic :as lb]
            [aggregator.broker.connector :as connector]
            [aggregator.broker.lib :as blib]
            [aggregator.utils.common :as lib]
            [aggregator.specs]))

(alias 'gspecs 'aggregator.specs)

(defn- publish
  "Create queue for entity and publish it on this queue.

  TODO: We are currently ignoring the Exchange. I think we do not need it
  here..."
  [entity]
  (let [ch (connector/open-channel)]
    (connector/create-queue ch entity)
    (lb/publish ch "" (blib/get-queue-name entity) (json/write-str entity))
    (connector/close-channel ch)))

(defn publish-statement
  "Put a statement to the correct queue. Statement must conform spec."
  [statement]
  {:pre [(lib/check-argument ::gspecs/statement statement)]}
  (publish statement))

(s/fdef publish-statement
        :args (s/cat :statement ::gspecs/statement))


;; -----------------------------------------------------------------------------
;; Testing-Area

(comment
  (def statement (ffirst (s/exercise ::gspecs/statement)))
  (connector/init-connection!)
  (publish-statement statement)
  (blib/get-queue-name statement)
  (json/write-str statement)
  )
