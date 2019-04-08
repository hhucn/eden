(ns aggregator.broker.provider-test
  (:require [aggregator.broker.provider :as provider]
            [clojure.test :refer [is deftest testing]]))

(deftest test-queues
  (testing "Test whether the queues work properly."
    (is (empty? (provider/get-subscriptions "statements")))

    (provider/subscribe-to-queue "statements" "test-aggregator")
    (provider/subscribe-to-queue "statements" "another-test")
    (is (contains? (provider/get-subscriptions "statements") "another-test"))
    (is (contains? (provider/get-subscriptions "statements") "test-aggregator"))

    (provider/remove-subscription "statements" "another-test")
    (is (not (contains? (provider/get-subscriptions "statements") "another-test")))
    (is (contains? (provider/get-subscriptions "statements") "test-aggregator"))
    (is (empty? (provider/get-subscriptions "links")))))
