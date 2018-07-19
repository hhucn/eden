(ns aggregator.api.routes
  "Define and expose the routes for the REST API in this file."
  (:require [compojure.core :refer [POST defroutes]]
            [compojure.api.sweet :refer [GET api context resource]]
            [ring.middleware.keyword-params :refer [wrap-keyword-params]]
            [ring.middleware.json :refer [wrap-json-params wrap-json-response wrap-json-body]]
            [ring.util.response :refer [response]]
            [aggregator.query.query :as query]
            [spec-tools.spec :as spec]
            [clojure.spec.alpha :as s]
            [aggregator.specs :as eden-specs]
            [ring.util.http-response :refer [ok]]
            [taoensso.timbre :as log]))

#_(defroutes app-routes
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


(s/def ::statements (s/coll-of ::eden-specs/statement))
(s/def ::statements-map (s/or(s/keys :req-un [::statements])))

(def statement-routes
  (context "/statements" []
           :tags ["statements"]
           :coercion :spec

           (GET "/" []
                :summary "Returns all statements"
                :query-params []
                :return ::statements-map
                (ok {:statements (query/all-local-statements)}))

           (GET "/starter-set" []
                :summary "Returns up to 10 statements chosen by the Aggregator"
                :query-params []
                :return ::statements-map
                (ok {:statements (query/starter-set)}))

           (GET "/by-id" []
                :summary "Returns all statements matching aggregator and entity-id"
                :query-params [aggregate-id :- ::eden-specs/aggregate-id,
                               entity-id :- ::eden-specs/entity-id]
                :return ::statements-map
                (ok {:statements (query/tiered-retrieval aggregate-id entity-id
                                                         {:opts [:no-remote]})}))))

(def app
  (api {:coercion :spec
        :swagger
        {:ui "/swagger"
         :spec "/swagger.json"
         :data {:info {:title "EDEN Aggregator API"
                       :description "An API to request statements and links from the EDEN instance."}
                :tags [{:name "statements" :description "Retrieve Statements"}]}}}
       statement-routes
       #_(-> spec-routes
           (wrap-json-body {:keywords? true
                            :bigdecimals? true})
           wrap-keyword-params
           wrap-json-params
           wrap-json-response)))

(comment
  (use 'ring.adapter.jetty)
  (run-jetty app {:port 8080})
  )
