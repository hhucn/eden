(ns aggregator.query.update-test
  (:require [aggregator.query.update :as update]
            [aggregator.query.query :as query]
            [aggregator.config :as config]
            [clojure.test :refer [deftest is]]))

(def some-statement
  {:identifier {:aggregate-id "name-test" :entity-id "34" :version 1}
   :content {:author {:name "Jorge"
                      :id 666
                      :dgep-native false}
             :text "money does not solve problems of our society"
             :created nil}
   :predecessors {}
   :delete-flag false})

(def some-link
  {:author {:name "Wegi"
            :dgep-native true
            :id 1234}
   :created nil
   :type :undercut
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
    (is (= "lolz" (get-in updated-statement [:content :text])))
    (is (= ":fooo" (get-in updated-statement-2 [:content :text])))
    (is (nil? not-a-statement))))

(deftest test-fork-statement
  (let [updated-statement (update/fork-statement some-statement {:aggregate-id "new-agg.de"
                                                                 :entity-id "42"
                                                                 :version 15}
                                                 "New content"
                                                 {:name "Der Wetschi"
                                                  :dgep-native true
                                                  :id 1})
        predecessor (first (:predecessors updated-statement))]
    (is (= 1 (get-in updated-statement [:identifier :version])))
    (is (= "New content" (get-in updated-statement [:content :text])))
    (is (= "new-agg.de" (get-in updated-statement [:identifier :aggregate-id])))
    (is (= "42" (get-in updated-statement [:identifier :entity-id])))
    (is (= "Der Wetschi" (get-in updated-statement [:content :author :name])))
    (is (= "name-test" (:aggregate-id predecessor)))
    (is (= "34" (:entity-id predecessor)))))


(deftest test-add-argument
  (let [{:keys [premise-id conclusion-id link-id]} (update/add-argument
                                                    "Der Kalli testet"
                                                    "Conclusion wird supportet"
                                                    1)]
    (is (= "anonymous"
           (get-in (query/exact-statement (:aggregate-id premise-id)
                                          (:entity-id premise-id)
                                          (:version premise-id))
                   [:content :author :name])))
    (is (= "Conclusion wird supportet"
           (get-in (query/exact-statement (:aggregate-id conclusion-id)
                                          (:entity-id conclusion-id)
                                          (:version conclusion-id))
                   [:content :text])))
    (is (not (nil?
              (count (query/retrieve-link (:aggregate-id link-id)
                                          (:entity-id link-id)
                                          (:version link-id))))))))
