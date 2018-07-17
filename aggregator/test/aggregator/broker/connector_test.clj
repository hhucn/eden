(ns aggregator.broker.connector-test
  (:require [aggregator.broker.connector :as connector]
            [clojure.test :refer [deftest is use-fixtures]]
            [aggregator.utils.common :as lib]))

;; Test preparation
(defn fixtures [f]
  (connector/init-connection!)
  (f)
  (connector/close-connection!))
(use-fixtures :once fixtures)


;; -----------------------------------------------------------------------------
;; Tests

(deftest open-channel-test
  (is (not (nil? (connector/open-channel)))))

(deftest close-channel-test
  (is (= :ok (:status (-> (connector/open-channel) connector/close-channel)))))

(deftest create-queue-test
  (is (= :ok (:status (connector/create-queue "i.am.groot")))))

(deftest delete-queue-test
  (let [queue (str (lib/uuid))]
    (is (= :ok (:status (connector/create-queue queue))))
    (is (= :ok (:status (connector/delete-queue queue))))))

(deftest queue-exists?
  (let [queue (str (lib/uuid))]
    (is (= :ok (:status (connector/create-queue queue))))
    (is (connector/queue-exists? queue))
    (is (= :ok (:status (connector/delete-queue queue))))
    (is (not (connector/queue-exists? queue)))))

(deftest connected?
  (connector/init-connection!)
  (is (connector/connected?))
  (connector/close-connection!)
  (is (not (connector/connected?)))
  (connector/init-connection!))
