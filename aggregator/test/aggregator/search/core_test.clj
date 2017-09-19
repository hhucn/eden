(ns aggregator.search.core-test
  (:require [clojure.test :refer [deftest is are testing]]
            [aggregator.search.core :as search]))

(def statement {:author "kangaroo"
                :content "Schnapspralinen"
                :aggregate-id "huepfer.verlag"
                :entity-id "1"
                :version 1
                :created nil})

(deftest create-delete-index
  (testing "Only one index of the same kind is allowed."
    (let [some-keyword :kangaroo]
      (are [x y] (= x y)
        :ok (:status (search/create-index some-keyword))
        :error (:status (search/create-index some-keyword))
        :ok (:status (search/delete-index some-keyword))
        :error (:status (search/delete-index some-keyword))))))

(deftest add-delete-statements
  (testing "Adds new statements to the index and deletes them again."
    (are [x y] (= x y)
      :ok (:status (search/add-statement statement))
      :ok (:status (search/add-statement statement))
      :ok (:status (search/delete-statement statement))
      :error (:status (search/delete-statement statement)))))
