(ns aggregator.broker.lib
  (:require [clojure.spec.alpha :as s]
            [aggregator.specs]
            [aggregator.utils.common :as lib]))

(alias 'gspecs 'aggregator.specs)

(defn get-queue-name [{:keys [aggregate-id entity-id]}]
  {:pre [(lib/check-argument ::gspecs/aggregate-id aggregate-id)
         (lib/check-argument ::gspecs/entity-id entity-id)]}
  (str "statement/queues" "/" aggregate-id "/" entity-id))

(s/fdef get-queue-name
        :args (s/cat :aggregate-id ::gspecs/aggregate-id :entity-id ::gspecs/entity-id))
