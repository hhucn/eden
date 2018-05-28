(ns aggregator.query.db-test
  (:require [aggregator.query.db :as db]
            [aggregator.config :as config]
            [clojure.test :refer [deftest is use-fixtures]]))

(defn fixtures [f]
  (db/insert-statement {:identifier {:aggregate-id "hhu.de" :entity-id "34" :version 1}
                        :content {:author "Jorge"
                                  :content-string "money does not solve problems of our society"
                                  :created nil}
                        :predecessors {}
                        :delete-flag false})
  (db/insert-statement {:identifier {:aggregate-id "hhu.de" :entity-id "P12" :version 1}
                        :content {:author "George"
                                  :content-string "we should shut down University Park"
                                  :created nil}
                        :predecessors {}
                        :delete-flag false})
  (db/insert-statement {:identifier {:aggregate-id "hhu.de" :entity-id "P13" :version 1}
                        :content {:author "George"
                                  :content-string "shutting down University Park will save $100.000 a year"
                                  :created nil}
                        :predecessors {}
                        :delete-flag false})
  (db/insert-statement {:identifier {:aggregate-id "hhu.de" :entity-id "P22" :version 1}
                        :content {:author "AlterVerwalter"
                                  :content-string "the city is planing a new park in the upcoming month"
                                  :created nil}
                        :predecessors {}
                        :delete-flag false})
  (db/insert-statement {:identifier {:aggregate-id "hhu.de" :entity-id "7" :version 1}
                        :content {:author "Bolek"
                                  :content-string "we should not abandon our town's core task"
                                  :created nil}
                        :predecessors {}
                        :delete-flag false})
  (db/insert-statement {:identifier {:aggregate-id "hhu.de" :entity-id "P23" :version 1}
                        :content {:author "XxxBaerchiDarkDestoyerxxX"
                                  :content-string "there is a smaller park in O-Town"
                                  :created nil}
                        :predecessors {}
                        :delete-flag false})
  (db/insert-statement {:identifier {:aggregate-id "hhu.de" :entity-id "P232" :version 1}
                        :content {:author "XxxBestoyerxxX"
                                  :content-string "there is a smaller park in O-Town"
                                  :created nil}
                        :predecessors {}
                        :delete-flag false})
  (db/insert-statement {:identifier {:aggregate-id "hhu.de" :entity-id "P231" :version 1}
                        :content {:author "XxxBoyerxxX"
                                  :content-string "there is a smaller park in O-Town"
                                  :created nil}
                        :predecessors {}
                        :delete-flag false})
  (db/insert-statement {:identifier {:aggregate-id "hhu.de" :entity-id "P230" :version 1}
                        :content {:author "XxxBayerxxX"
                                  :content-string "there is a smaller park in O-Town"
                                  :created nil}
                        :predecessors {}
                        :delete-flag false})
  (db/insert-statement {:identifier {:aggregate-id "hhu.de" :entity-id "P29" :version 1}
                        :content {:author "XxxBaeryerxxX"
                                  :content-string "there is a smaller park in O-Town"
                                  :created nil}
                        :predecessors {}
                        :delete-flag false})
  (db/insert-statement {:identifier {:aggregate-id config/aggregate-name :entity-id "P29v2" :version 1}
                        :content {:author "XxxBaeryerxxX"
                                  :content-string "there is a smaller park in O-Town"
                                  :created nil}
                        :predecessors {}
                        :delete-flag false})
  
  (db/insert-link {:author "Wegi" :created nil :type :undercut
                   :source {:aggregate-id "schneider.gg"
                            :entity-id "W01" :version 1337}
                   :destination {:aggregate-id "schneider.gg"
                                 :entity-id "W_link_35" :version 1}
                   :identifier {:aggregate-id "schneider.gg" :entity-id "link0r1337" :version 1}
                   :delete-flag false})
  (Thread/sleep 2000)  ;; ElasticSearch needs around 2 seconds to add new entities to the index
  (f))
(use-fixtures :once fixtures)


(deftest statement-uri-retrieval-test
  (is (= (get-in (first (db/statements-by-uri "hhu.de/34")) [:identifier :version]) 1))
  (is (= (get-in (first (db/statements-by-uri "hhu.de/P12")) [:identifier :aggregate-id]) "hhu.de"))
  (is (= (get-in (first (db/statements-by-uri "hhu.de/P13")) [:identifier :entity-id]) "P13"))
  (is (= (get-in (first (db/statements-by-uri "hhu.de/P22")) [:content :content-string])
         "the city is planing a new park in the upcoming month"))
  (is (= (get-in (first (db/statements-by-uri "hhu.de/7")) [:content :author]) "Bolek")))

(deftest statement-by-author-test
  (is (= (count (db/statements-by-author "XxxBaerchiDarkDestoyerxxX")) 1))
  (is (= (count (db/statements-by-author "George")) 2)))

(deftest insert-statement-test
  (db/insert-statement {:content
                        {:author "Wegi" :content-string "Test me baby one more time" :created nil}
                        :identifier
                        {:aggregate-id "schneider.gg" :entity-id "W01" :version 1337}
                        :delete-flag false
                        :predecessors {}})
  (Thread/sleep 2000)
  (is (= (get-in (db/exact-statement "schneider.gg" "W01" 1337) [:content :content-string])
         "Test me baby one more time")))

(deftest insert-link-test
  (is (= (:author (db/exact-link "schneider.gg" "W01" 1337 "schneider.gg" "W_link_35" 1))
         "Wegi"))
  (is (= "Wegi" (:author (first (db/links-by-uri "schneider.gg/link0r1337"))))))

(deftest random-statements
  (let [results (db/random-statements 1)]
    (is (= (count results) 1))
    (is (= (get-in (rand-nth results) [:identifier :aggregate-id]) config/aggregate-name))))

(deftest test-entity-by-uri
  (let [results (db/entities-by-uri "hhu.de/34" :statements)]
    (is (= 1 (count results)))
    (is (= "Jorge" (get-in (first results) [:content :author])))))

(deftest test-undercuts
  (let [results (db/get-undercuts "schneider.gg" "W_link_35")]
    (is (= "link0r1337" (get-in (first results) [:identifier :entity-id])))))

(deftest test-links-by-target
  (let [results (db/links-by-target "schneider.gg" "W_link_35" 1)]
    (is (= "link0r1337" (get-in (first results) [:identifier :entity-id])))))
