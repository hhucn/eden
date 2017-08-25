(ns aggregator.specs
  (:require [clojure.spec.alpha :as s]))

(s/def ::sequence_number pos-int?)
(s/def ::version pos-int?)
(s/def ::creator string?)
(s/def ::type string?)
(s/def ::aggregate_id string?)
(s/def ::entity_id string?)
(s/def ::data string?)

(s/def ::event
  (s/keys :req-un [::sequence_number ::version ::creator ::type]
          :opt-un [::aggregate_id ::entity_id ::data]))
(s/exercise ::event)
