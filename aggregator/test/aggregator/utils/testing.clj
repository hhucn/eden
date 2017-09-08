(ns aggregator.utils.testing
  (:require [clojure.test :refer [is]]
            [clojure.spec.test.alpha :as stest]
            [clojure.pprint :refer [pprint]]))

(defn- summarize-results' [spec-check]
  (doall (map #(-> %
                   (select-keys [:clojure.spec.test.check/ret :sym])
                   vals
                   pprint) spec-check))
  (println))

(defn check' [function]
  (let [spec-result (stest/check function)]
    (summarize-results' spec-result)
    (is (nil? (-> spec-result first :failure)))))
