(ns aggregator.broker.lib
  (:require [clojure.spec.alpha :as s]
            [aggregator.utils.common :as lib]
            [clojure.spec.test.alpha :as stest]
            [aggregator.specs]))

(alias 'gspecs 'aggregator.specs)

(defn get-queue-name
  "When the ids match the corresponding specs, create a valid queue name."
  [{:keys [aggregate-id entity-id]}]
  (when (and (lib/valid? ::gspecs/aggregate-id aggregate-id)
             (lib/valid? ::gspecs/entity-id entity-id))
    (str "statement/queues" "/" aggregate-id "/" entity-id)))


;; -----------------------------------------------------------------------------
;; Specs

(s/fdef get-queue-name
        :args (s/cat :statement ::gspecs/statement)
        :ret string?)
