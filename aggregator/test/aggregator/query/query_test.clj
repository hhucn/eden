(ns aggregator.query.query-test
  (:require [aggregator.query.query :as query]
            [aggregator.query.db :as db]
            [aggregator.query.cache :as cache]
            [aggregator.config :as config]
            [clojure.test :refer [deftest is use-fixtures]]
            [aggregator.query.utils :as utils]))

(defn fixtures [f]
  (db/insert-statement {:identifier {:aggregate-id "hhu.de" :entity-id "34" :version 1}
                        :content {:author {:name "Jorge"
                                           :dgep-native true
                                           :id 1234}
                                  :text "money does not solve problems of our society"
                                  :created nil}
                        :predecessors {}
                        :delete-flag false})
  (db/insert-statement {:identifier {:aggregate-id "hhu.de" :entity-id "P12" :version 1}
                        :content {:author {:name "George"
                                           :dgep-native true
                                           :id 1234}
                                  :text "we should shut down University Park"
                                  :created nil}
                        :predecessors {}
                        :delete-flag false})
  (db/insert-statement {:identifier {:aggregate-id "hhu.de" :entity-id "P13" :version 1}
                        :content {:author {:name "George"
                                           :dgep-native true
                                           :id 1234}
                                  :text "shutting down University Park will save $100.000 a year"
                                  :created nil}
                        :predecessors {}
                        :delete-flag false})
  (db/insert-statement {:identifier {:aggregate-id "hhu.de" :entity-id "P22" :version 1}
                        :content {:author {:name "AlterVerwalter"
                                           :dgep-native true
                                           :id 1234}
                                  :text "the city is planing a new park in the upcoming month"
                                  :created nil}
                        :predecessors {}
                        :delete-flag false})
  (db/insert-statement {:identifier {:aggregate-id "hhu.de" :entity-id "7" :version 1}
                        :content {:author {:name "Bolek"
                                           :dgep-native true
                                           :id 1234}
                                  :text "we should not abandon our town's core task"
                                  :created nil}
                        :predecessors {}
                        :delete-flag false})
  (db/insert-statement {:identifier {:aggregate-id "hhu.de" :entity-id "P23" :version 1}
                        :content {:author {:name "XxxBaerchiDarkDestoyerxxX"
                                           :dgep-native true
                                           :id 1234}
                                  :text "there is a smaller park in O-Town"
                                  :created nil}
                        :predecessors {}
                        :delete-flag false})
  (db/insert-statement {:identifier {:aggregate-id "hhu.de" :entity-id "P232" :version 1}
                        :content {:author {:name "XxxBestoyerxxX"
                                           :dgep-native true
                                           :id 1234}
                                  :text "there is a smaller park in O-Town"
                                  :created nil}
                        :predecessors {}
                        :delete-flag false})
  (db/insert-statement {:identifier {:aggregate-id "hhu.de" :entity-id "P231" :version 1}
                        :content {:author {:name "XxxBoyerxxX"
                                           :dgep-native true
                                           :id 1234}
                                  :text "there is a smaller park in O-Town"
                                  :created nil}
                        :predecessors {}
                        :delete-flag false})
  (db/insert-statement {:identifier {:aggregate-id "hhu.de" :entity-id "P230" :version 1}
                        :content {:author {:name "XxxBayerxxX"
                                           :dgep-native true
                                           :id 1234}
                                  :text "there is a smaller park in O-Town"
                                  :created nil}
                        :predecessors {}
                        :delete-flag false})
  (db/insert-statement {:identifier {:aggregate-id "hhu.de" :entity-id "P29" :version 1}
                        :content {:author {:name "XxxBaeryerxxX"
                                           :dgep-native true
                                           :id 1234}
                                  :text "there is a smaller park in O-Town"
                                  :created nil}
                        :predecessors {}
                        :delete-flag false})
  (db/insert-statement {:identifier {:aggregate-id "hhu.de" :entity-id "P420" :version 1}
                        :content {:author {:name "saywhaaat"
                                           :dgep-native true
                                           :id 1234}
                                  :text "califragilistic extrahotentific"
                                  :created nil}
                        :predecessors {}
                        :delete-flag false})
  (db/insert-statement {:identifier {:aggregate-id config/aggregate-name :entity-id "P29v2" :version 1}
                        :content {:author {:name "XxxBaeryerxxX"
                                           :dgep-native true
                                           :id 1234}
                                  :text "there is a smaller park in O-Town"
                                  :created nil}
                        :predecessors {}
                        :delete-flag false})
  
  (db/insert-link {:author {:name "Wegi"
                            :dgep-native true
                            :id 1234}
                   :created nil :type :undercut
                   :source {:aggregate-id "schneider.gg"
                            :entity-id "W01" :version 1337}
                   :destination {:aggregate-id "schneider.gg"
                                 :entity-id "W_link_35" :version 1}
                   :identifier {:aggregate-id "schneider.gg" :entity-id "link0r1337" :version 1}
                   :delete-flag false})
  (db/insert-link {:author {:name "Wegi"
                            :dgep-native true
                            :id 1234}
                   :created nil :type :support
                   :source {:aggregate-id "hhu.de"
                            :entity-id "P420" :version 1}
                   :destination {:aggregate-id config/aggregate-name
                                 :entity-id "P29v2" :version 1}
                   :identifier {:aggregate-id config/aggregate-name :entity-id "link0r7331" :version 1}
                   :delete-flag false})
  (Thread/sleep 2000)  ;; ElasticSearch needs around 2 seconds to add new entities to the index
  (f))
(use-fixtures :once fixtures)

;; Here be tests
(deftest test-retrieve-link
  (is (not (empty? (query/retrieve-link "schneider.gg" "link0r1337" 1))))
  (is (empty? (query/retrieve-link "foo.bar" "nonexistent" 1))))

(deftest local-tiered-retrieval
  (is (not (empty? (query/tiered-retrieval "hhu.de" "34" {:opts [:no-remote]}))))
  (is (empty? (query/tiered-retrieval "foo.bar" "nonexistent" {:opts [:no-remote]}))))


(deftest test-cached-statements
  (let [statement {:identifier {:aggregate-id "cache-aggregator" :entity-id "cache-id" :version 1}
                   :content {:author {:name "XxxBaeryerxxX"
                                      :dgep-native true
                                      :id 1234}
                             :text "there is a smaller park in O-Town"
                             :created nil}
                   :predecessors {}
                   :delete-flag false}
        id (:identifier statement)]
    (cache/cache-miss (utils/build-cache-pattern statement) statement)
    (is (= statement (query/exact-statement (:aggregate-id id) (:entity-id id) (:version id))))))

(deftest test-statements-contain
  (is (= 1 (count (query/statements-contain "califragilistic")))))

(deftest test-all-arguments
  (is (not (empty? (query/all-arguments)))))

(deftest test-argument-by-author
  (let [argument (first (query/arguments-by-author "Wegi"))]
    (is (= "Wegi" (get-in argument [:link :author :name])))
    (is (or (= :support (get-in argument [:link :type]))
            (= :attack (get-in argument [:link :type]))))))
