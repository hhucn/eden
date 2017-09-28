(ns aggregator.query.query-test
  (:require [aggregator.query.query :as query]
            [clojure.test :refer [deftest is]]))

;; Here be tests

(deftest test-exact-statement
  (let [desired (first (query/tiered-retrieval "hhu.de/34" {:opts [:no-remote]}))]
    (is (= desired
           (query/exact-statement "hhu.de" "34" 1)))))
