(ns aggregator.query.db-test
  (:require [aggregator.query.db :as db]
            [aggregator.config :as config]
            [aggregator.search.core :as search]
            [clojure.test :refer [deftest is use-fixtures]]))

(defn fixtures [f]
  (search/add-statement {:aggregate-id "hhu.de" :entity-id "34" :author "Jorge" :content "money does not solve problems of our society" :version 1})
  (search/add-statement {:aggregate-id "hhu.de" :entity-id "P12" :author "George" :content "we should shut down University Park" :version 1})
  (search/add-statement {:aggregate-id "hhu.de" :entity-id "P13" :author "George" :content "shutting down University Park will save $100.000 a year" :version 1})
  (search/add-statement {:aggregate-id "hhu.de" :entity-id "P22" :author "AlterVerwalter" :content "the city is planing a new park in the upcoming month" :version 1})
  (search/add-statement {:aggregate-id "hhu.de" :entity-id "7" :author "Bolek" :content "we should not abandon our town's core task" :version 1})
  (search/add-statement {:aggregate-id "hhu.de" :entity-id "P23" :author "XxxBaerchiDtoyerxxX" :content "there is a smaller park in O-Town" :version 1})
  (search/add-statement {:aggregate-id "hhu.de" :entity-id "P232" :author "XxxBestoyerxxX" :content "there is a smaller park in O-Town" :version 1})
  (search/add-statement {:aggregate-id "hhu.de" :entity-id "P231" :author "XxxBoyerxxX" :content "there is a smaller park in O-Town" :version 1})
  (search/add-statement {:aggregate-id "hhu.de" :entity-id "P230" :author "XxxBayerxxX" :content "there is a smaller park in O-Town" :version 1})
  (search/add-statement {:aggregate-id "hhu.de" :entity-id "P29" :author "XxxBaeryerxxX" :content "there is a smaller park in O-Town" :version 1})
  (Thread/sleep 2000)  ;; ElasticSearch needs around 2 seconds to add new entities to the index
  (f))
(use-fixtures :once fixtures)


(deftest statement-uri-retrieval-test
  (is (= (get-in (db/statements-by-uri "hhu.de/34") [:data :hits 0 :_source :version])
         1))
  (is (= (get-in (db/statements-by-uri "hhu.de/P12") [:data :hits 0 :_source :aggregate-id])
         "hhu.de"))
  (is (= (get-in (db/statements-by-uri "hhu.de/P13") [:data :hits 0 :_source :entity-id])
         "P13"))
  (is (= (get-in (db/statements-by-uri "hhu.de/P22") [:data :hits 0 :_source :content])
         "the city is planing a new park in the upcoming month"))
  (is (= (get-in (db/statements-by-uri "hhu.de/7") [:data :hits 0 :_source :author])
         "Bolek")))

(deftest statement-by-author-test
  (is (= (count (get-in (db/statements-by-author "XxxBaerchiDarkDestoyerxxX") [:data :hits]))
         1))
  (is (= (count (get-in (db/statements-by-author "George") [:data :hits]))
         2)))

(deftest insert-statement-test
  (db/insert-statement {:author "Wegi" :content "Test me baby one more time"
                        :aggregate-id "schneider.gg" :entity-id "W01" :version 1337})
  (Thread/sleep 2000)
  (is (= (get-in (db/exact-statement "schneider.gg" "W01" 1337) [:data :hits 0 :_source :content])
         "Test me baby one more time")))

(deftest insert-link-test
  (db/insert-link {:author "Wegi" :type "undercut" :from-aggregate-id "schneider.gg"
                   :from-entity-id "W01" :from-version 1337 :to-aggregate-id "schneider.gg"
                   :to-entity-id "W_link_35" :aggregate-id "schneider.gg" :entity-id "link0r1337"})
  (Thread/sleep 2000)
  (is (= (get-in (db/exact-link "schneider.gg" "W01" 1337 "schneider.gg" "W_link_35")
                 [:data :hits 0 :_source :author])
         "Wegi")))

(deftest random-statements
  (let [results (db/random-statements 10)]
    (is (= (count results) 10))
    (is (= (:aggregate_id (rand-nth results)) config/aggregate-name))))
