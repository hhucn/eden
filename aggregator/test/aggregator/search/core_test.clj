(ns aggregator.search.core-test
  (:require [clojure.test :refer [deftest is are testing use-fixtures]]
            [aggregator.search.core :as search]
            [clojure.spec.alpha :as s]
            [aggregator.specs :as gspecs]))

(def statement {:author "kangaroo"
                :content "Schnapspralinen"
                :aggregate-id "huepfer.verlag"
                :entity-id "1"
                :version 1
                :created nil})

(def link (first (last (s/exercise ::gspecs/link))))

(defn fixtures [f]
  (search/add-statement statement)
  (f)
  (search/delete-statement statement))
(use-fixtures :once fixtures)


(deftest create-delete-index
  (testing "Only one index of the same kind is allowed."
    (let [some-keyword :kangaroo]
      (are [x y] (= x y)
        :ok (:status (search/create-index some-keyword))
        :error (:status (search/create-index some-keyword))
        :ok (:status (search/delete-index some-keyword))
        :error (:status (search/delete-index some-keyword))))))

(deftest add-delete-statements
  (testing "Adds new statements to the index and delete them again."
    (let [stmt (first (last (s/exercise ::gspecs/statement)))]
      (are [x y] (= x y)
        :ok (:status (search/add-statement stmt))
        :ok (:status (search/add-statement stmt))
        :ok (:status (search/delete-statement stmt))
        :error (:status (search/delete-statement stmt))))))

(deftest add-delete-links
  (testing "Adds new links to the index and delete them again."
    (let [lnk (first (last (s/exercise ::gspecs/link)))]
      (are [x y] (= x y)
        :ok (:status (search/add-link lnk))
        :ok (:status (search/add-link lnk))
        :ok (:status (search/delete-link lnk))
        :error (:status (search/delete-link lnk))))))

(deftest search-by-fulltext
  (testing "Find by fulltext-search."
    (are [x] (pos? (get-in x [:data :total]))
      (search/search :fulltext "kangar*")
      (search/search :fulltext "huepfer*")
      (search/search :fulltext "huepfer.verlag")
      (search/search :fulltext "*"))
    (are [x] (zero? (get-in x [:data :total]))
      (search/search :fulltext "kangarooy")
      (search/search :fulltext "penguin")
      (search/search :fulltext ""))))

(deftest search-default
  (testing "The default search is currently the same as :fulltext."
    (are [x] (pos? (get-in x [:data :total]))
      (search/search :default "kangar*")
      (search/search :default "huepfer*")
      (search/search :default "huepfer.verlag")
      (search/search :default "*"))
    (are [x] (zero? (get-in x [:data :total]))
      (search/search :default "kangarooy")
      (search/search :fulltext "penguin")
      (search/search :fulltext ""))))

(deftest search-with-fuzziness
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
      (search/search :fuzzy "")
      (search/search :fuzzy "*"))))

