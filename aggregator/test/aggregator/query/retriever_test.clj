(ns aggregator.query.retriever-test
  (:require [aggregator.query.retriever :as retriever]
            [aggregator.config :as config]
            [aggregator.query.db :as db]
            [clojure.test :refer [deftest is use-fixtures testing]]))

(def default-link
  {:author {:name "Wegi"
            :dgep-native true
            :id 1234}
   :created nil :type :undercut
   :source {:aggregate-id config/aggregate-name
            :entity-id "1" :version 1}
   :destination {:aggregate-id config/aggregate-name
                 :entity-id "2" :version 1}
   :identifier {:aggregate-id "schneider.gg" :entity-id "link0r1337" :version 1}
   :delete-flag false})

(defn fixtures [f]
  (db/insert-statement {:identifier {:aggregate-id config/aggregate-name :entity-id "1" :version 1}
                        :content {:author {:name "XxxBaeryerxxX"
                                           :dgep-native true
                                           :id 1234}
                                  :text "there is a smaller park in O-Town"
                                  :created nil}
                        :predecessors {}
                        :delete-flag false})
  (db/insert-statement {:identifier {:aggregate-id config/aggregate-name :entity-id "2" :version 1}
                        :content {:author {:name "XxxBaeryerxxX"
                                           :dgep-native true
                                           :id 1234}
                                  :text "there is a smaller park in O-Town"
                                  :created nil}
                        :predecessors {}
                        :delete-flag false})
  (db/insert-link default-link)
  (f))

(use-fixtures :once fixtures)

(deftest test-whitelisted?
  (testing "Test whitelist check"
    (is (retriever/whitelisted? {:identifier {:aggregate-id "aggregator:8888"}}))
    (is (retriever/whitelisted? {:identifier {:aggregate-id "aggregator_set2:8888"}}))
    (is (not (retriever/whitelisted? {:identifier {:aggregate-id "notinthelist"}})))))
