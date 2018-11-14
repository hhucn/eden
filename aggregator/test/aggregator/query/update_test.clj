(ns aggregator.query.update-test
  (:require [aggregator.query.update :as update]
            [aggregator.config :as config]
            [clojure.test :refer [deftest is]]))

(def some-statement
  {:identifier {:aggregate-id "name-test" :entity-id "34" :version 1}
   :content {:author "Jorge"
             :content-string "money does not solve problems of our society"
             :created nil}
   :predecessors {}
   :delete-flag false})

(def some-link
  {:author "Wegi" :created nil :type :undercut
   :source {:aggregate-id "schneider.gg"
            :entity-id "W01" :version 1337}
   :destination {:aggregate-id "test-link"
                 :entity-id "W_link_35" :version 1}
   :identifier {:aggregate-id "number2" :entity-id "link0r1337" :version 1}
   :delete-flag false})

(deftest test-known-aggregators-update
  (reset! config/app-state {:known-aggregators #{}})
  (update/update-statement some-statement)
  (is (contains? (:known-aggregators @config/app-state) "name-test")))

(deftest test-known-aggregators-from-links
  (reset! config/app-state {:known-aggregators #{}})
  (update/update-link some-link)
  (is (contains? (:known-aggregators @config/app-state) "schneider.gg"))
  (is (contains? (:known-aggregators @config/app-state) "test-link"))
  (is (contains? (:known-aggregators @config/app-state) "number2")))

(deftest test-updated-content
  (let [updated-statement (update/update-statement-content some-statement "lolz")
        updated-statement-2 (update/update-statement-content some-statement :fooo)
        not-a-statement (update/update-statement-content some-link "fail")]
    (is (= 2 (get-in updated-statement [:identifier :version])))
    (is (= "lolz" (get-in updated-statement [:content :content-string])))
    (is (= ":fooo" (get-in updated-statement-2 [:content :content-string])))
    (is (nil? not-a-statement))))

(deftest test-fork-statement
  (let [updated-statement (update/fork-statement some-statement {:aggregate-id "new-agg.de"
                                                                 :entity-id "42"
                                                                 :version 15}
                                                 "New content" "Der Wetschi")
        predecessor (first (:predecessors updated-statement))]
    (is (= 1 (get-in updated-statement [:identifier :version])))
    (is (= "New content" (get-in updated-statement [:content :content-string])))
    (is (= "new-agg.de" (get-in updated-statement [:identifier :aggregate-id])))
    (is (= "42" (get-in updated-statement [:identifier :entity-id])))
    (is (= "Der Wetschi" (get-in updated-statement [:content :author])))
    (is (= "name-test" (:aggregate-id predecessor)))
    (is (= "34" (:entity-id predecessor)))))
