(ns aggregator.query.query-test
  (:require [aggregator.query.query :as query]
            [aggregator.search.core :as search]
            [clojure.test :refer [deftest is use-fixtures]]))

(defn fixtures [f]
  (search/add-statement {:aggregate-id "hhu.de" :entity-id "34" :author "Jorge" :content "money does not solve problems of our society" :version 1})
  (search/add-statement {:aggregate-id "hhu.de" :entity-id "P12" :author "George" :content "we should shut down University Park" :version 1})
  (search/add-statement {:aggregate-id "hhu.de" :entity-id "P13" :author "George" :content "shutting down University Park will save $100.000 a year" :version 1})
  (search/add-statement {:aggregate-id "hhu.de" :entity-id "P22" :author "AlterVerwalter" :content "the city is planing a new park in the upcoming month" :version 1})
  (search/add-statement {:aggregate-id "hhu.de" :entity-id "7" :author "Bolek" :content "we should not abandon our town's core task" :version 1})
  (search/add-statement {:aggregate-id "hhu.de" :entity-id "P23" :author "XxxBaerchiDarkDestoyerxxX" :content "there is a smaller park in O-Town" :version 1})
  (search/add-statement {:aggregate-id "hhu.de" :entity-id "P232" :author "XxxBestoyerxxX" :content "there is a smaller park in O-Town" :version 1})
  (search/add-statement {:aggregate-id "hhu.de" :entity-id "P231" :author "XxxBoyerxxX" :content "there is a smaller park in O-Town" :version 1})
  (search/add-statement {:aggregate-id "hhu.de" :entity-id "P230" :author "XxxBayerxxX" :content "there is a smaller park in O-Town" :version 1})
  (search/add-statement {:aggregate-id "hhu.de" :entity-id "P29" :author "XxxBaeryerxxX" :content "there is a smaller park in O-Town" :version 1})
  (search/add-link {:author "Wegi" :type "undercut" :from-aggregate-id "schneider.gg"
                   :from-entity-id "W01" :from-version 1337 :to-aggregate-id "schneider.gg"
                   :to-entity-id "W_link_35" :aggregate-id "schneider.gg" :entity-id "link0r1337"})
  (Thread/sleep 2000)  ;; ElasticSearch needs around 2 seconds to add new entities to the index
  (f))
(use-fixtures :once fixtures)

;; Here be tests
(deftest test-retrieve-link
  (is (not= :not-found (query/retrieve-link "schneider.gg/link0r1337")))
  (is (= :not-found (query/retrieve-link "foo.bar/nonexistent"))))

(deftest local-tiered-retrieval
  (is (not= :not-found (query/tiered-retrieval "hhu.de/34" {:opts [:no-remote]})))
  (is (= :not-found (query/tiered-retrieval "foo.bar/nonexistent" {:opts [:no-remote]}))))
