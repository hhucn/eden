(ns aggregator.broker.lib-test
  (:require [aggregator.broker.lib :as blib]
            [clojure.test :refer [deftest is are]]
            [clojure.spec.alpha :as s]
            [aggregator.specs]))

(alias 'gspecs 'aggregator.specs)

(def statement (second (last (s/exercise ::gspecs/statement))))

(deftest get-queue-name-test
  (are [x y] (= x y)
    nil (blib/get-queue-name nil)
    nil (blib/get-queue-name {:foo :bar})
    nil (blib/get-queue-name {:aggregate-id "foo"})
    nil (blib/get-queue-name {:aggregate-id "foo" :entity-id nil})
    "statement/queues/foo/bar" (blib/get-queue-name {:aggregate-id "foo" :entity-id "bar"}))
  (is (s/valid? string? (blib/get-queue-name statement))))
