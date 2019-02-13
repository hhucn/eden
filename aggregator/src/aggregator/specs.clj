(ns aggregator.specs
  (:require [clojure.spec.alpha :as s]))

(s/def ::no-slash (s/and string? #(not (re-find #"/" %)) #(pos? (count %))))
(s/def ::non-empty-string (s/and string? #((complement clojure.string/blank?) %)))

(s/def ::dgep-native boolean?)
(s/def ::name ::non-empty-string)
(s/def ::id pos-int?)
(s/def ::author (s/keys :req-un [::dgep-native ::name ::id]))

(s/def ::text ::non-empty-string)
(s/def ::created (s/or :nil nil? :timestamp string?)) ;; timestamp
(s/def ::content
  (s/keys :req-un [::text ::created ::author]))

(s/def ::aggregate-id ::no-slash)
(s/def ::entity-id ::no-slash)
(s/def ::version pos-int?)
(s/def ::identifier
  (s/keys :req-un [::aggregate-id ::entity-id ::version]))

(s/def ::host ::non-empty-string)
(s/def ::path ::non-empty-string)
(s/def ::reference (s/keys :req-un [::text ::host ::path]))
(s/def ::references (s/coll-of ::reference))

(s/def ::predecessors (s/coll-of ::identifier))
(s/def ::delete-flag boolean?)
(s/def ::tag ::non-empty-string)
(s/def ::tags (s/coll-of ::tag))
(s/def ::statement
  (s/keys :req-un [::content
                   ::identifier ::predecessors
                   ::delete-flag]
          :opt-un [::references
                   ::tags]))

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

(s/def ::premise ::statement)
(s/def ::conclusion ::statement)
(s/def ::argument (s/keys :req-un [::premise ::conclusion ::link]))
