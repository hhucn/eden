(ns aggregator.query.query-test
  (:require [aggregator.query.query :as query]
            [clojure.test :refer [deftest is]]))

;; Here be tests
(deftest test-retrieve-link
  (is (not= :not-found (query/retrieve-link "schneider.gg/link0r1337")))
  (is (= :not-found (query/retrieve-link "foo.bar/nonexistent"))))

(deftest local-tiered-retrieval
  (is (not= :not-found (query/tiered-retrieval "hhu.de/34" {:opts [:no-remote]})))
  (is (= :not-found (query/tiered-retrieval "foo.bar/nonexistent" {:opts [:no-remote]}))))
