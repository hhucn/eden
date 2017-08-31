(ns aggregator.utils.common
  "Common functions, which can be used in several namespaces."
  (:require [clojure.spec.alpha :as s]))

(defn check-argument
  "Verify that data conforms to spec. Calls clojure.spec/explain-str to show a
  useful error message. Throws an IllegalArgumentException."
  [type data]
  (if (s/valid? type data)
    true
    (throw (IllegalArgumentException. (s/explain-str type data)))))
