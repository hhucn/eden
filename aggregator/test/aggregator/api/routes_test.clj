(ns aggregator.api.routes-test
  (:require [aggregator.api.routes :as routes]
            [clojure.test :refer [deftest is use-fixtures testing]]
            [aggregator.broker.connector :as connector]
            [ring.mock.request :as mock]
            [cheshire.core :as cheshire]))

(defn fixtures [f]
  (connector/init-connection!)
  (f)
  (connector/close-connection!))
(use-fixtures :once fixtures)

(defn- parse-body [body]
  (cheshire/parse-string (slurp body) true))


(deftest handler-test
  (testing "Test root route for status 200"
    (is (= 200 (:status (routes/app (mock/request :get "/")))))))

(deftest statement-nonexistent-route
  (testing "Test nonexistent-route for status 500"
    (is (= 404 (:status (routes/app (mock/request :get "/statements/fantasy.dork/nonexistent")))))))

(deftest statement-retrieval-test
  (let [response (routes/app (mock/request :get "/statements/by-id?aggregate-id=foo&entity-id=bar"))
        body     (parse-body (:body response))]
    (is (= (:status response) 200))
    (is (= (:statements body) []))))
