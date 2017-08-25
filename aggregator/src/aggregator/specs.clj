(ns aggregator.specs
  (:require [clojure.spec.alpha :as s]))

(s/def ::sequence_number pos-int?)
(s/def ::version pos-int?)
(s/def ::creator string?)
(s/def ::type string?)
(s/def ::aggregate_id string?)
(s/def ::entity_id uuid?)
(s/def ::data string?)

(s/def ::event
  (s/keys :req-un [::sequence_number ::version ::creator ::type]
          :opt-un [::aggregate_id ::entity_id ::data]))
(comment
  (s/exercise ::event)
  )

(s/def ::data map?)

(s/def ::statement
  (s/keys :req-un [::aggregate_id ::data]
          :opt-un [::entity_id]))

(s/exercise ::statement)

