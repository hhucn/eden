(ns aggregator.api.routes-test
  (:require [aggregator.api.routes :as routes]
            [aggregator.query.utils :as utils]
            [clojure.test :refer [deftest is use-fixtures]]
            [aggregator.broker.connector :as connector]
            [ring.mock.request :as mock]))

(defn fixtures [f]
  (connector/init-connection!)
  (f)
  (connector/close-connection!))
(use-fixtures :once fixtures)

(defn- routestest-helper
  "Delivers the result-map and handles the default mock-request."
  [route]
  (utils/http-response->map (routes/app (mock/request :get route))))

(deftest handler-test
  (is (= "ok"
         (:status (routestest-helper "/")))))

(deftest statement-retrieval-test
  (is (= "not-found"
         (get-in (routestest-helper "/statements/fantasy.dork/non-existent-id") [:data :payload]))))
