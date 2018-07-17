(ns aggregator.api.routes
  "Define and expose the routes for the REST API in this file."
  (:require [compojure.core :refer [GET POST defroutes]]
            [ring.middleware.keyword-params :refer [wrap-keyword-params]]
            [ring.middleware.json :refer [wrap-json-params wrap-json-response wrap-json-body]]
            [ring.util.response :refer [response]]
            [aggregator.query.query :as query]
            [taoensso.timbre :as log]))

(defroutes app-routes
  "The routes of the aggregator defined are RESTful and can be used to inquire for entities. Singular worded routes like `statement/` always require a specificity in the route following. Plural forms like `statements/` usualy return multiple things when not specified further."
  (GET "/" []
       (response {:status :ok
                  :data {:payload "Its definitely the horsesized chicken."}}))
  (GET "/statements" _request
       (response {:status :ok
                  :data {:payload (query/all-local-statements)}}))
  (GET "/statements/starter-set" _request
       (response {:status :ok
                  :data {:payload (query/starter-set)}}))
  ;; The Order of those GET Requests is important!
  ;; The other way around starter-set will be interpreted as an entity.
  (GET "/statements/:entity{.+}" {:keys [params]}
       (log/debug "[REST] Someone just retrieved statements")
       (response {:status :ok
                  :data {:payload (query/tiered-retrieval
                                   (str (:entity params))
                                   {:opts [:no-remote]})}}))
  (GET "/links" _request
       (response {:status :ok
                  :data {:payload (query/all-local-links)}}))
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

(comment
  (use 'ring.adapter.jetty)
  (run-jetty app {:port 8080})
  )
