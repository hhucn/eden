(ns aggregator.broker.connector-test
  (:require [aggregator.broker.connector :as connector]
            [clojure.test :refer [deftest is use-fixtures]]))

;; Test preparation
(defn fixtures [f]
  (connector/init-connection!)
  (f)
  (connector/close-connection!))
(use-fixtures :once fixtures)


;; -----------------------------------------------------------------------------
;; Tests

(deftest open-channel-test
  (is true (connector/open-channel)))

(deftest close-channel-test
  (is true (-> (connector/open-channel) connector/close-channel)))

(deftest create-queue-test
  (is true (connector/create-queue "i.am.groot")))
