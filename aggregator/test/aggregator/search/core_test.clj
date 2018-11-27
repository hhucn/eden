(ns aggregator.search.core-test
  (:require [clojure.test :refer [deftest is are testing use-fixtures]]
            [aggregator.search.core :as search]
            [clojure.spec.alpha :as s]
            [aggregator.specs :as gspecs]))

(def kangaroo {:content {:text "Schnapspralinen"
                         :author "kangaroo"
                         :created nil}
               :identifier {:aggregate-id "huepfer.verlag"
                            :entity-id "1"
                            :version 1}
               :predecessors {}
               :delete-flag false})

(def penguin {:content {:text "Teewurst"
                         :author "penguin"
                         :created nil}
               :identifier {:aggregate-id "penguin.books:8080"
                            :entity-id "1"
                            :version 1}
               :predecessors {}
              :delete-flag false})

(def penguin2 {:content {:text "Teewurst 2: Die Rache der Teewurst"
                         :author "penguin"
                         :created nil}
               :identifier {:aggregate-id "penguin.books:8080"
                            :entity-id "2"
                            :version 1}
               :predecessors {}
               :delete-flag false})


(def link (first (last (s/exercise ::gspecs/link))))

(defn fixtures [f]
  (search/entrypoint)
  (search/add-statement kangaroo)
  (search/add-statement penguin)
  (search/add-statement penguin2)
  (Thread/sleep 2000)  ;; ElasticSearch needs around 2 seconds to add new entities to the index
  (f)
  (search/delete-statement kangaroo)
  (search/delete-statement penguin)
  (search/delete-statement penguin2))
(use-fixtures :once fixtures)

(deftest create-delete-index-test
  (testing "Only one index of the same kind is allowed."
    (let [some-keyword :kangaroo]
      (are [x y] (= x y)
        :ok (:status (search/create-index some-keyword))
        :error (:status (search/create-index some-keyword)))
      (Thread/sleep 2000)
      (are [x y] (= x y)
        :ok (:status (search/delete-index some-keyword))
        :error (:status (search/delete-index some-keyword))))))

(deftest add-delete-statements-test
  (let [stmt (first (last (s/exercise ::gspecs/statement)))
        stmts (map first (s/exercise ::gspecs/statement))]
    (testing "Add statements to index."
      (is (= :ok (:status (search/add-statement stmt))))
      (is (= :ok (:status (search/add-statement stmt))))
      (is (every? #(= :ok %) (map :status (doall (map search/add-statement stmts))))))
    (Thread/sleep 2000)
    (testing "Now remove the generated statements from the index."
      (is (= :ok (:status (search/delete-statement stmt))))
      (is (= :error (:status (search/delete-statement stmt))))
      (is (every? #(= :ok %) (map :status (doall (map search/delete-statement stmts))))))))

(deftest add-delete-links-test
  (let [lnk (first (last (s/exercise ::gspecs/link)))
        lnks (map first (s/exercise ::gspecs/link))]
    (testing "Add some links to the index."
      (is (= :ok (:status (search/add-link lnk))))
      (is (= :ok (:status (search/add-link lnk))))
      (is (every? #(= :ok %) (map :status (doall (map search/add-link lnks))))))
    (Thread/sleep 2000)
    (testing "Now delete the recently added links."
      (is (= :ok (:status (search/delete-link lnk))))
      (is (= :error (:status (search/delete-link lnk))))
      (is (every? #(= :ok %) (map :status (doall (map search/delete-link lnks))))))))

(deftest search-by-fulltext-test
  (testing "Find by fulltext-search."
    (are [x] (pos? (get-in x [:data :total]))
      (search/search :fulltext "kangar")
      (search/search :fulltext "huepfer")
      (search/search :fulltext "huepfer.verlag"))
    (are [x] (zero? (get-in x [:data :total]))
      (search/search :fulltext "kangarooy")
      (search/search :fulltext "hatchingpenguineggs")
      (search/search :fulltext "*")
      (search/search :fulltext ""))))

(deftest search-default-test
  (testing "The default search is currently the same as :fulltext."
    (are [x] (pos? (get-in x [:data :total]))
      (search/search :default "kangar")
      (search/search :default "huepfer.ver")
      (search/search :default "huepfer.verlag"))
    (are [x] (zero? (get-in x [:data :total]))
      (search/search :default "kangarooy")
      (search/search :default "penguinswillruletheworld")
      (search/search :default "*")
      (search/search :default ""))))

(deftest search-with-fuzziness-test
  (testing "Do some fuzzy search."
    (are [x] (pos? (get-in x [:data :total]))
      (search/search :fuzzy "kangarooyy")
      (search/search :fuzzy "kangarooy")
      (search/search :fuzzy "kangaroo")
      (search/search :fuzzy "kangaro")
      (search/search :fuzzy "kangar")
      (search/search :fuzzy "kangar00")
      (search/search :fuzzy "kengar0o"))
    (are [x] (zero? (get-in x [:data :total]))
      (search/search :fuzzy "kangarooyyy")
      (search/search :fuzzy "kengar0")
      (search/search :fuzzy "*")
      (search/search :fuzzy ""))))

(deftest search-entity-test
  (testing "Test for exact entity"
    (are [x] (pos? (get-in x [:data :total]))
      (search/search :statements {:identifier.aggregate-id "huepfer.verlag"
                                  :identifier.entity-id "1"})
      (search/search :statements {:identifier.aggregate-id "penguin.books:8080"
                                  :identifier.entity-id "1"}))))

(deftest search-statements-by-aggregate-id-test
  (testing "Query by aggregate-id to retrieve all matched statements."
    (are [min-results response] (<= min-results (get-in response [:data :total]))
      1 (search/search :statements {:identifier.aggregate-id "huepfer.verlag"})
      2 (search/search :statements {:identifier.aggregate-id "penguin.books:8080"}))))

(deftest return-all-statements-for-aggregator-test
  (testing "Given an aggregator, return the first 10.000 statements in his index."
    (are [min-results response] (<= min-results (get-in response [:data :total]))
      0 (search/search :all-statements "razupaltuff-it-is-a-non-existent-index")
      1 (search/search :all-statements "huepfer.verlag")
      2 (search/search :all-statements "penguin.books:8080")
      3 (search/search :all-statements nil))))

