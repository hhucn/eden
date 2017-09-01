(ns aggregator.utils.common
  "Common functions, which can be used in several namespaces."
  (:require [clojure.spec.alpha :as s]
            [taoensso.timbre :as log]))

(defn log-argument
  "Verify that data conforms to spec. Calls clojure.spec/explain-str to show a
  useful error message. Prints output to logs and *always* return true for usage
  within pre-conditions."
  [type data]
  (when-not (s/valid? type data) (log/info (s/explain-str type data)))
  true)
