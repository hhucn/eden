(ns aggregator.query.db-test
  (:require [aggregator.query.db :as db]
            [aggregator.config :as config]
            [clojure.test :refer [deftest is]]))


(deftest statement-uri-retrieval-test
  (is (= (:version (first (db/statements-by-uri "hhu.de/34")))
         1))
  (is (= (:aggregate_id (first (db/statements-by-uri "hhu.de/P12")))
         "hhu.de"))
  (is (= (:entity_id (first (db/statements-by-uri "hhu.de/P13")))
         "P13"))
  (is (= (:content (first (db/statements-by-uri "hhu.de/P22")))
         "the city is planing a new park in the upcoming month"))
  (is (= (:author (first (db/statements-by-uri "hhu.de/7")))
         "Bolek")))

(deftest statement-by-author-test
  (is (= (count (db/statements-by-author "XxxBaerchiDarkDestoyerxxX"))
         1))
  (is (= (count (db/statements-by-author "George"))
         2)))

(deftest insert-statement-test
  (db/insert-statement {:author "Wegi" :content "Test me baby one more time"
                        :aggregate-id "schneider.gg" :entity-id "W01" :version 1337})
  (Thread/sleep 2000)
  (is (= (:content (db/exact-statement "schneider.gg" "W01" 1337))
         "Test me baby one more time")))

(deftest insert-link-test
  (db/insert-link {:author "Wegi" :type "undercut" :from-aggregate-id "schneider.gg"
                   :from-entity-id "W01" :from-version 1337 :to-aggregate-id "schneider.gg"
                   :to-entity-id "W_link_35" :aggregate-id "schneider.gg" :entity-id "link0r1337"})
  (Thread/sleep 2000)
  (is (= (:author (db/exact-link "schneider.gg" "W01" 1337 "schneider.gg" "W_link_35"))
         "Wegi")))

(deftest random-statements
  (let [results (db/random-statements 10)]
    (is (= (count results) 10))
    (is (= (:aggregate_id (rand-nth results)) config/aggregate-name))))
