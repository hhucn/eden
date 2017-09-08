(ns aggregator.specs
  (:require [clojure.spec.alpha :as s]))

(s/def ::no-slash (s/and string? #(not (re-find #"/" %)) #(pos? (count %))))

(s/def ::author string?)
(s/def ::content string?)
(s/def ::aggregate-id ::no-slash)
(s/def ::entity-id ::no-slash)
(s/def ::version pos-int?)
(s/def ::created string?) ;; timestamp
(s/def ::ancestor-aggregate-id ::no-slash)
(s/def ::ancestor-entity-id ::no-slash)
(s/def ::ancestor-version ::version)
(s/def ::statement
  (s/keys :req-un [::author ::content
                   ::aggregate-id ::entity-id ::version
                   ::created]
          :opt-un [::ancestor-aggregate-id ::ancestor-entity-id ::ancestor-version]))
;; (s/exercise ::statement)


(s/def ::from-aggregate-id ::no-slash)
(s/def ::from-entity-id ::no-slash)
(s/def ::from-version ::version)
(s/def ::type keyword?)
(s/def ::to-aggregate-id ::no-slash)
(s/def ::to-entity-id ::no-slash)
(s/def ::to-version ::version)
(s/def ::link
  (s/keys :req-un [::author ::type
                   ::from-aggregate-id ::from-entity-id ::from-version
                   ::to-aggregate-id ::to-entity-id
                   ::aggregate-id ::entity-id
                   ::created]
          :opt-un [::to-version]))
;; (s/exercise ::link)


;; Error messages
(s/def ::status keyword?)
(s/def ::message string?)

(s/def ::error (s/keys :req-un [::status ::message]))
