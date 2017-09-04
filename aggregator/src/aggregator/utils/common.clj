(ns aggregator.utils.common
  "Common functions, which can be used in several namespaces."
  (:require [clojure.spec.alpha :as s]
            [taoensso.timbre :as log]))

(defn valid?
  "Verify that data conforms to spec. Calls clojure.spec/explain-str to show a
  useful error message. Prints output to logs and returns a boolean."
  [spec data]
  (if (s/valid? spec data)
    true
    (do (log/debug (s/explain-str spec data))
        false)))
