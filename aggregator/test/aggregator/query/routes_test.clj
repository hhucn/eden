(ns aggregator.query.routes-test
  (:require [aggregator.query.routes :as routes]
            [aggregator.query.utils :as utils]
            [clojure.test :refer :all]
            [ring.mock.request :as mock]
            [clojure.data.json :as json]))

(deftest handler-test
  (is (= (:status (utils/http-response->map (routes/app (mock/request :get "/"))))
         "ok")))


