(ns aggregator.query.update-test
  (:require [aggregator.query.update :as update]
            [aggregator.config :as config]
            [clojure.test :refer [deftest is]]))

(deftest test-known-aggregators-update
  (reset! config/app-state {:known-aggregators #{}})
  (update/update-statement
   {:identifier {:aggregate-id "name-test" :entity-id "34" :version 1}
    :content {:author "Jorge"
              :content-string "money does not solve problems of our society"
              :created nil}
    :predecessors {}
    :delete-flag false})
  (is (contains? (:known-aggregators @config/app-state) "name-test")))

(deftest test-known-aggregators-from-links
  (reset! config/app-state {:known-aggregators #{}})
  (update/update-link
   {:author "Wegi" :created nil :type :undercut
    :source {:aggregate-id "schneider.gg"
             :entity-id "W01" :version 1337}
    :destination {:aggregate-id "test-link"
                  :entity-id "W_link_35" :version 1}
    :identifier {:aggregate-id "number2" :entity-id "link0r1337" :version 1}
    :delete-flag false})
  (is (contains? (:known-aggregators @config/app-state) "schneider.gg"))
  (is (contains? (:known-aggregators @config/app-state) "test-link"))
  (is (contains? (:known-aggregators @config/app-state) "number2")))
