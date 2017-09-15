(ns aggregator.search.core-test
  (:require [clojure.test :refer [deftest is are testing]]
            [aggregator.search.core :as search]))

(deftest create-delete-index
  (testing "Only one index of the same kind is allowed."
    (let [some-keyword :kangaroo]
      (are [x y] (= x y)
        :ok (:status (search/create-index some-keyword))
        :error (:status (search/create-index some-keyword))
        :ok (:status (search/delete-index some-keyword))
        :error (:status (search/delete-index some-keyword))))))
