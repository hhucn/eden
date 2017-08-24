(ns aggregator.query.routes

  ;; Define and expose the routes for the REST API in this file.

  (:require [compojure.core :refer [GET POST defroutes]]
            [compojure.route :as route]
            [ring.middleware.keyword-params :refer [wrap-keyword-params]]
            [ring.middleware.json :refer [wrap-json-params wrap-json-response wrap-json-body]]
            [ring.util.response :refer [response]]
            [aggregator.query.query :as query])
  (:use [clojure.repl :refer [source]]))

(defroutes app-routes
  (GET "/" []
       (response {:status :ok
                  :data {:payload "Its definitely the horsesized chicken."}}))
  (GET "/statement/:id{[0-9]+}" {:keys [params]}
       (query/tiered-retrieval (str "localhost::" (:id params)) {:no-remote 1})))
;; At this part localhost should be replaced by host configuration

(def app
  (-> app-routes
      (wrap-json-body {:keywords? true
                       :bigdecimals? true})
      wrap-keyword-params
      wrap-json-params
      wrap-json-response))
