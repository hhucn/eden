(ns aggregator.api.routes
  "Define and expose the routes for the REST API in this file."
  (:require [compojure.core :refer [GET POST defroutes]]
            [ring.middleware.keyword-params :refer [wrap-keyword-params]]
            [ring.middleware.json :refer [wrap-json-params wrap-json-response wrap-json-body]]
            [ring.util.response :refer [response]]
            [aggregator.query.query :as query]
            [taoensso.timbre :as log]
            [aggregator.broker.connector :as connector]))

(defroutes app-routes
  (GET "/" []
       (response {:status :ok
                  :data {:payload "Its definitely the horsesized chicken."}}))
  (GET "/statements/starter-set" {:keys [server-name]}
       (response {:status :ok
                  :data {:payload (query/starter-set)
                         :queue (connector/create-queue server-name)}}))
  ;; The Order of those GET Requests is important!
  ;; The other way around starter-set will be interpreted as an entity.
  (GET "/statements/:entity{.+}" {:keys [params server-name]}
       (log/debug "[REST] Someone just retrieved statements")
       (response {:status :ok
                  :data {:payload (query/tiered-retrieval
                                   (str (:entity params))
                                   {:opts [:no-remote]})
                         :queue (connector/create-queue server-name)}}))
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
  (GET "/statement/:aggregate{.+}/:entity{.+}/:version{[0-9]+}" {:keys [params server-name]}
       (log/debug "[REST] Someone just retrieved a specific statement")
       (response {:status :ok
                  :data {:payload (query/exact-statement (:aggregate params)
                                                         (:entity params)
                                                         (read-string (:version params)))
                         :queue (connector/create-queue server-name)}})))

(def app
  (-> app-routes
      (wrap-json-body {:keywords? true
                       :bigdecimals? true})
      wrap-keyword-params
      wrap-json-params
      wrap-json-response))

(comment
  (use 'ring.adapter.jetty)
  (run-jetty app {:port 8080})
  )
