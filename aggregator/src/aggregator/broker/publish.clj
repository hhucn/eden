(ns aggregator.broker.publish
  (:require [clojure.spec.alpha :as s]
            [langohr.basic :as lb]
            [aggregator.broker.connector :as connector]
            [aggregator.specs]
            [aggregator.broker.lib :as blib]
            [aggregator.utils.common :as lib]))

(alias 'gspecs 'aggregator.specs)

(defn- publish
  "Puts payload in a queue and handles channel for this command.

  TODO: We are currently ignoring the Exchange. I think we do not need it
  here..."
  [entity]
  (let [ch (connector/open-channel)]
    (lb/publish ch "" (blib/get-queue-name entity) entity)
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
  (connector/init-connection!)
  (publish :statement "foo")
  (publish-statement "foo")
  )
