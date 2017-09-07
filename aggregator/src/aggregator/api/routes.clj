(ns aggregator.api.routes

  ;; Define and expose the routes for the REST API in this file.

  (:require [compojure.core :refer [GET POST defroutes]]
            [ring.middleware.keyword-params :refer [wrap-keyword-params]]
            [ring.middleware.json :refer [wrap-json-params wrap-json-response wrap-json-body]]
            [ring.util.response :refer [response]]
            [aggregator.query.query :as query]
            [taoensso.timbre :as log])
  (:use [clojure.repl :refer [source]]))

(defroutes app-routes
  (GET "/" []
       (response {:status :ok
                  :data {:payload "Its definitely the horsesized chicken."}}))
  (GET "/statements/:entity{.+}" {:keys [params]}
       (log/debug "[REST] Someone just retrieved statements")
       (response {:status :ok
                  :data {:payload (query/tiered-retrieval
                                   (str (:entity params))
                                   {:opts [:no-remote]})}}))
  (GET "/link/undercuts/:target-entity{.+}" {:keys [params]}
       (log/debug "[REST] Someone just retrieved undercuts")
       (response {:status :ok
                  :data {:payload (query/local-undercuts (str (:target-entity params)))}}))
  (GET "/link/to/:aggregate{.+}/:entity{.+}/:version{[0-9]+}" {:keys [params]}
       (log/debug "[REST] Someone just retrieved pointed links")
       (response {:status :ok
                  :data {:payload (query/links-by-target (:aggregate params)
                                                         (:entity params)
                                                         (read-string (:version params)))}}))
  (GET "/link/:entity{.+}" {:keys [params]}
       (log/debug "[REST] Someone just retrieved a link")
       (response {:status :ok
                  :data {:payload (query/retrieve-link (str (:entity params)))}}))
  (GET "/statement/:aggregate{.+}/:entity{.+}/:version{[0-9]+}" {:keys [params]}
       (log/debug "[REST] Someone just retrieved a specific statement")
       (response {:status :ok
                  :data {:payload (query/exact-statement (:aggregate params)
                                                         (:entity params)
                                                         (read-string (:version params)))}})))

(def app
  (-> app-routes
      (wrap-json-body {:keywords? true
                       :bigdecimals? true})
      wrap-keyword-params
      wrap-json-params
      wrap-json-response))

(comment (use 'ring.adapter.jetty)
         (run-jetty app {:port 8080}))
