(ns aggregator.broker.subscriber-test
  (:require [clojure.test :refer [deftest is are use-fixtures testing]]
            [aggregator.broker.subscriber :as sub]
            [aggregator.broker.publish :as pub]
            [aggregator.broker.connector :as connector]
            [aggregator.utils.common :as lib]
            [clojure.spec.alpha :as s]
            [aggregator.specs :as gspecs]))

(def queue (str (lib/uuid)))
(def queue-two (str (lib/uuid)))  ;; like Mewto, hihi

(def statement {:author "kangaroo"
                :content "Schnapspralinen"
                :aggregate-id "huepfer.verlag"
                :entity-id "1"
                :version 1
                :created nil})

(def link (first (last (s/exercise ::gspecs/link))))

(def broker
  {:host (System/getenv "BROKER_HOST")
   :user (System/getenv "BROKER_USER")
   :password (System/getenv "BROKER_PASS")})

;; Test preparation
(defn fixtures [f]
  (connector/init-local-connection!)
  (connector/create-queue queue)
  (connector/create-queue queue-two)
  (pub/publish-statement statement)
  (pub/publish-link link)
  (f)
  (connector/delete-queue queue)
  (connector/delete-queue queue-two)
  (connector/close-local-connection!))
(use-fixtures :once fixtures)


(defn handler [_meta payload]
  (println "Received" payload))

;; -----------------------------------------------------------------------------
;; Tests

(deftest subscribe-test
  (testing "Valid credentials should subscribe successfully"
    (is (= :ok (:status (sub/subscribe handler queue broker))))
    (is (= :ok (:status (sub/subscribe queue broker)))))
  (testing "Wrong host gives status :error"
    (is (= :error (:status
                   (sub/subscribe handler queue
                                  (assoc broker :host "deathstar#4"))))))
  (testing "Can't subscribe to a non-existent queue"
    (is (= :error (:status
                   (sub/subscribe handler
                                  "Fear is the path to the dark side" broker))))))

(deftest to-query
  (testing "Handling received messages directly to the query module"
    (is (= :ok (:status (sub/subscribe sub/to-query queue-two broker))))))
