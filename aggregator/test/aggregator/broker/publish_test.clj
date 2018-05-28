(ns aggregator.broker.publish-test
  (:require [clojure.test :refer [deftest is use-fixtures]]
            [clojure.spec.alpha :as s]
            [aggregator.broker.publish :as pub]
            [aggregator.broker.connector :as connector]
            [aggregator.specs :as gspecs]))

(defonce queue (first (last (s/exercise string?))))

(def statement {:content {:author "kangaroo"
                          :content-string "Schnapspralinen"
                          :created nil}
                :identifier {
                             :aggregate-id "huepfer.verlag"
                             :entity-id "1"
                             :version 1}
                :predecessors {}
                :delete-flag false})

(def statements (map first (s/exercise ::gspecs/statement)))
(def links (map first (s/exercise ::gspecs/link)))

;; Test preparation
(defn fixtures [f]
  (connector/init-connection!)
  (f)
  (connector/close-connection!))
(use-fixtures :once fixtures)


;; -----------------------------------------------------------------------------
;; Tests

(deftest publish-statement
  (is (= :ok (:status (pub/publish-statement statement))))
  (is (every? #(= :ok %) (map :status (doall (map pub/publish-statement statements))))))

(deftest publish-link
  (is (every? #(= :ok %) (map :status (doall (map pub/publish-link links))))))
