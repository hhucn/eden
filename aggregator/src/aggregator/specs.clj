(ns aggregator.specs
  (:require [clojure.spec.alpha :as s]))

(s/def ::no-slash (s/and string? #(not (re-find #"/" %)) #(pos? (count %))))

(s/def ::author string?)
(s/def ::content-string string?)
(s/def ::created (s/or :nil nil? :timestamp string?)) ;; timestamp
(s/def ::content
  (s/keys :req-un [::content-string ::created ::author]))

(s/def ::aggregate-id ::no-slash)
(s/def ::entity-id ::no-slash)
(s/def ::version pos-int?)
(s/def ::identifier
  (s/keys :req-un [::aggregate-id ::entity-id ::version]))

(s/def ::predecessors (s/coll-of ::identifier))
(s/def ::delete-flag boolean?)
(s/def ::statement
  (s/keys :req-un [::content
                   ::identifier ::predecessors
                   ::delete-flag]))
;; (s/exercise ::statement)

(s/def ::type keyword?)
(s/def ::source ::identifier)
(s/def ::destination ::identifier)
(s/def ::link
  (s/keys :req-un [::type
                   ::source ::destination
                   ::identifier ::delete-flag]
          :opt-un [::created ::author]))
;; (s/exercise ::link)
