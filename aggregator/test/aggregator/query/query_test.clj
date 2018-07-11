(ns aggregator.query.query-test
  (:require [aggregator.query.query :as query]
            [aggregator.query.db :as db]
            [aggregator.query.update :as update]
            [aggregator.query.cache :as cache]
            [aggregator.config :as config]
            [clojure.test :refer [deftest is use-fixtures]]
            [aggregator.query.utils :as utils]))

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

;; Here be tests
(deftest test-retrieve-link
  (is (not= :not-found (query/retrieve-link "schneider.gg/link0r1337")))
  (is (= :not-found (query/retrieve-link "foo.bar/nonexistent"))))

(deftest local-tiered-retrieval
  (is (not= :not-found (query/tiered-retrieval "hhu.de/34" {:opts [:no-remote]})))
  (is (= :not-found (query/tiered-retrieval "foo.bar/nonexistent" {:opts [:no-remote]}))))


(deftest test-cached-statements
  (let [statement {:identifier {:aggregate-id "cache-aggregator" :entity-id "cache-id" :version 1}
                   :content {:author "XxxBaeryerxxX"
                             :content-string "there is a smaller park in O-Town"
                             :created nil}
                   :predecessors {}
                   :delete-flag false}
        id (:identifier statement)]
    (cache/cache-miss (utils/build-cache-pattern statement) statement)
    (is (= statement (query/exact-statement (:aggregate-id id) (:entity-id id) (:version id))))))
