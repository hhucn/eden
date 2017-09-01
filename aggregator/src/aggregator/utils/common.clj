(ns aggregator.utils.common
  "Common functions, which can be used in several namespaces."
  (:require [clojure.spec.alpha :as s]
            [taoensso.timbre :as log]))

(defn check-argument
  "Verify that data conforms to spec. Calls clojure.spec/explain-str to show a
  useful error message. Prints output to logs."
  [type data]
  (if (s/valid? type data)
    true
    (log/info (s/explain-str type data))))
