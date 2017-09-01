(ns aggregator.api.routes-test
  (:require [aggregator.api.routes :as routes]
            [aggregator.query.utils :as utils]
            [clojure.test :refer :all]
            [ring.mock.request :as mock]
            [clojure.data.json :as json]))

(defn routestest-helper
  "Delivers the result-map and handles the default mock-request."
  [route]
  (utils/http-response->map (routes/app (mock/request :get route))))

(deftest handler-test
  (is (= (:status (routestest-helper "/"))
         "ok")))

(deftest statement-retrieval-test
  (is (= (get-in (routestest-helper "/statement/fantasy.dork/non-existent-id") [:data :payload])
         "not-found")))
