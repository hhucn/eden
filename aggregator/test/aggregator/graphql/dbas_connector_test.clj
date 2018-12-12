(ns aggregator.graphql.dbas-connector-test
  (:require [aggregator.graphql.dbas-connector :as dbas-conn]
            [clojure.test :refer [deftest is testing]]))


(deftest test-statement-retrieval
  (testing "The default instance shall always return some statements"
    (let [result (dbas-conn/get-statements)]
      (is (seq? result))
      (if (seq result)
        (do
          (is (contains? (first result) :identifier))
          (is (contains? (first result) :content))
          (is (contains? (first result) :delete-flag))
          (is (contains? (first result) :predecessors)))))))

(deftest test-link-type
  (testing "Test whether the right link types are determined from the graphQL response"
    (let [support {:uid 2, :conclusionUid 2, :isSupportive true, :authorUid 1, :argumentUid nil, :premisegroupUid 2}
          attack {:uid 3, :conclusionUid 2, :isSupportive false, :authorUid 1, :argumentUid nil, :premisegroupUid 3}
          undercut {:uid 6, :conclusionUid nil, :isSupportive false, :authorUid 1, :argumentUid 4, :premisegroupUid 6}]
      (is (= :support (dbas-conn/link-type support)))
      (is (= :attack (dbas-conn/link-type attack)))
      (is (= :undercut (dbas-conn/link-type undercut))))))

(deftest test-link-retrieval
  (testing "Test whether the right link format is returned. The test instance should have links ready."
    (let [result (dbas-conn/get-links)]
      (is (seq? result))
      (if (seq result)
        (do
          (is (contains? (first result) :identifier))
          (is (contains? (first result) :source))
          (is (contains? (first result) :destination))
          (is (contains? (first result) :type)))))))
