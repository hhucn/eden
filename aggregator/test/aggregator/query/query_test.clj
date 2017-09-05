(ns aggregator.query.query-test
  (:require [aggregator.query.cache :as cache]
            [aggregator.utils.db :as db]
            [aggregator.query.query :as query]
            [clojure.test :refer :all]))

;; Here be tests

(deftest test-exact-statement
  (let [desired (query/tiered-retrieval "hhu.de/34" {:opts [:no-remote]})]
    (is (= desired
           (query/exact-statement "hhu.de" "34" 1)))))
