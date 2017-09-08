(ns aggregator.broker.subscriber-test
  (:require [clojure.test :refer [deftest is are use-fixtures]]
            [aggregator.broker.subscriber :as sub]
            [aggregator.broker.publish :as pub]
            [aggregator.broker.connector :as connector]
            [aggregator.utils.common :as lib]))

(def queue (str (lib/uuid)))

(def statement {:author "kangaroo"
                :content "Schnapspralinen"
                :aggregate-id "huepfer.verlag"
                :entity-id "1"
                :version 1
                :created nil})

;; Test preparation
(defn fixtures [f]
  (connector/init-connection!)
  (connector/create-queue queue)
  (pub/publish-statement statement)
  (f)
  (connector/delete-queue queue)
  (connector/close-connection!))
(use-fixtures :once fixtures)


;; -----------------------------------------------------------------------------
;; Tests

(deftest subscribe
  (is true (sub/subscribe (fn [_meta payload] (println "Received some content: " payload)) queue "broker")))
