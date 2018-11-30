(ns aggregator.api.routes-test
  (:require [aggregator.api.routes :as routes]
            [clojure.test :refer [deftest is use-fixtures testing]]
            [aggregator.broker.connector :as connector]
            [ring.mock.request :as mock]
            [cheshire.core :as cheshire]))

(defn fixtures [f]
  (connector/init-local-connection!)
  (f)
  (connector/close-local-connection!))
(use-fixtures :once fixtures)

(defn- parse-body [body]
  (cheshire/parse-string (slurp body) true))


(deftest handler-test
  (testing "Test root route for status 302"
    (let [response (routes/app (mock/request :get "/"))]
      (is (= (:status response) 302)))))

(deftest statement-nonexistent-route
  (testing "Test nonexistent-route for status 500"
    (let [response (routes/app (mock/request :get "/statements/fantasy.dork/nonexistent"))]
      (is (= (:status response) 404)))))

(deftest statement-retrieval-test
  (testing "Test for empty statements"
    (let [response (routes/app (mock/request :get "/statements/by-id?aggregate-id=foo&entity-id=bar"))
          body     (parse-body (:body response))]
      (is (= (:status response) 200))
      (is (= (:statements body) [])))))
