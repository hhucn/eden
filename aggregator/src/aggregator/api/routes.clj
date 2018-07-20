(ns aggregator.api.routes
  "Define and expose the routes for the REST API in this file."
  (:require [compojure.core :refer [POST defroutes]]
            [compojure.route]
            [compojure.api.sweet :refer [GET api context resource undocumented]]
            [ring.middleware.keyword-params :refer [wrap-keyword-params]]
            [ring.middleware.json :refer [wrap-json-params wrap-json-response wrap-json-body]]
            [ring.util.response :refer [response]]
            [aggregator.query.query :as query]
            [spec-tools.spec :as spec]
            [clojure.spec.alpha :as s]
            [aggregator.specs :as eden-specs]
            [ring.util.http-response :refer [ok not-found]]
            [taoensso.timbre :as log]))

#_(defroutes app-routes
  "The routes of the aggregator defined are RESTful and can be used to inquire for entities. Singular worded routes like `statement/` always require a specificity in the route following. Plural forms like `statements/` usualy return multiple things when not specified further."
  (GET "/link/:entity{.+}" {:keys [params]}
       (log/debug "[REST] Someone just retrieved a link")
       (response {:status :ok
                  :data {:payload (query/retrieve-link (str (:entity params)))}})))


(s/def ::welcome-message spec/string?)
(s/def ::statements (s/coll-of ::eden-specs/statement))
(s/def ::statements-map (s/keys :req-un [::statements]))
(s/def ::links (s/coll-of ::eden-specs/link))
(s/def ::links-map (s/keys :req-un [::links]))
(s/def ::statement-map (s/keys :req-un [::eden-specs/statement]))
(s/def ::link-map (s/keys :req-un [::eden-specs/link]))

(def statements-routes
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

(def links-routes
  (context "/links" []
    :tags ["links"]
    :coercion :spec

    (GET "/" []
      :summary "Returns all links"
      :query-params []
      :return ::links-map
      (ok {:links (query/all-local-links)}))

    (GET "/undercuts" []
      :summary "Return all undercuts targeting `aggregate-id/entity-id`"
      :query-params [aggregate-id :- ::eden-specs/aggregate-id
                     entity-id :- ::eden-specs/entity-id]
      :return ::links-map
      (ok {:links (query/local-undercuts aggregate-id entity-id)}))

    (GET "/to" []
      :summary "Return all links pointing to a specific statement"
      :query-params [aggregate-id :- ::eden-specs/aggregate-id
                     entity-id :- ::eden-specs/entity-id
                     version :- ::eden-specs/version]
      :return ::links-map
      (ok {:links (query/links-by-target aggregate-id entity-id version)}))))

(def statement-routes
  (context "/statement" []
    :tags ["statement"]
    :coercion :spec

    (GET "/" []
      :summary "Return a specific statement by identifiers"
      :query-params [aggregate-id :- ::eden-specs/aggregate-id
                     entity-id :- ::eden-specs/entity-id
                     version :- ::eden-specs/version]
      :return ::statement-map
      (ok {:statement (query/exact-statement aggregate-id entity-id version)}))))

(def link-routes
  (context "/link" []
    :tags ["link"]
    :coercion :spec

    (GET "/" []
      :summary "Return a specific link by identifiers"
      :query-params [aggregate-id :- ::eden-specs/aggregate-id
                     entity-id :- ::eden-specs/entity-id
                     version :- ::eden-specs/version]
      :return ::link-map
      (ok {:statement (query/retrieve-link aggregate-id entity-id version)}))))

(def app
  (api {:coercion :spec
        :swagger
        {:ui "/swagger"
         :spec "/swagger.json"
         :data {:info {:title "EDEN Aggregator API"
                       :description "An API to request statements and links from the EDEN instance."}
                :tags [{:name "statements" :description "Retrieve Statements"}
                       {:name "links" :description "Retrieve Links"}
                       {:name "statement" :description "Retrieve single specific statement"}
                       {:name "link" :description "Retrieve single specific link"}]}}}

       statement-routes
       statements-routes
       link-routes
       links-routes

       (GET "/" []
         :summary "Test whether the api is online"
         :query-params []
         :return ::welcome-message
         (ok "Hello!"))

       (undocumented
        (compojure.route/not-found (not-found {:not "found"})))))

(comment
  (use 'ring.adapter.jetty)
  (run-jetty app {:port 8080})
  )
