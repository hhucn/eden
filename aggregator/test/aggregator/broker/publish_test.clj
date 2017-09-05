(ns aggregator.broker.publish-test
  (:require [clojure.test :refer [deftest is use-fixtures]]
            [aggregator.broker.publish :as pub]
            [aggregator.broker.connector :as connector]
            [aggregator.utils.common :as lib]
            [clojure.spec.alpha :as s]
            [aggregator.specs]))

(alias 'gspecs 'aggregator.specs)

(def queue (str (lib/uuid)))

(def statement {:author "kangaroo"
                :content "Schnapspralinen"
                :aggregate-id "huepfer.verlag"
                :entity-id "1"
                :version 1
                :created nil})

(def statements (for [[x _] (s/exercise ::gspecs/statement)] x))
(def links (for [[x _] (s/exercise ::gspecs/link)] x))

;; Test preparation
(defn fixtures [f]
  (connector/init-connection!)
  (connector/create-queue queue)
  (f)
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
