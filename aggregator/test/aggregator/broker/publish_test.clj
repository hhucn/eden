(ns aggregator.broker.publish-test
  (:require [clojure.test :refer [deftest is use-fixtures]]
            [aggregator.broker.publish :as pub]
            [aggregator.broker.connector :as connector]
            [clojure.spec.alpha :as s]
            [aggregator.specs]))

(alias 'gspecs 'aggregator.specs)

(defonce queue (first (last (s/exercise string?))))

(def statement {:author "kangaroo"
                :content "Schnapspralinen"
                :aggregate-id "huepfer.verlag"
                :entity-id "1"
                :version 1
                :created nil})

(def statements (map first (s/exercise ::gspecs/statement)))
(def links (map first (s/exercise ::gspecs/link)))

;; Test preparation
(defn fixtures [f]
  (connector/init-connection!)
  (connector/create-queue queue)
  (Thread/sleep 1000)
  (f)
  (Thread/sleep 1000)
  (connector/delete-queue queue)
  (connector/close-connection!))
(use-fixtures :once fixtures)


;; -----------------------------------------------------------------------------
;; Tests

(deftest publish-statement
  (is true (pub/publish-statement statement))
  (is (every? nil? (doall (map pub/publish-statement statements)))))

(deftest publish-link
  (is (every? nil? (doall (map pub/publish-link links)))))
