(ns aggregator.api.routes
  "Define and expose the routes for the REST API in this file."
  (:require [compojure.route]
            [compojure.api.sweet :refer [GET api context resource undocumented]]
            [aggregator.query.query :as query]
            [spec-tools.spec :as spec]
            [clojure.spec.alpha :as s]
            [aggregator.specs :as eden-specs]
            [ring.util.http-response :refer [ok not-found]]
            [ring.middleware.cors :as ring-cors]))

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

    (GET "/contain" []
         :summary "Returns all statements matching `search-string`"
         :query-params [search-string :- spec/string?]
         :return ::statements-map
         (ok {:statements (query/statements-contain search-string)}))

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
                (if-let [statement (query/exact-statement aggregate-id entity-id version)]
                  (ok {:statement statement})
                  (not-found nil)))))

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
                (if-let [link (query/retrieve-link aggregate-id entity-id version)]
                  (ok {:link link})
                  (not-found nil)))))

(def ^:private hello-route
  (GET "/" []
    :summary "Test whether the api is online"
    :query-params []
    :return ::welcome-message
    (ok "Hello!")))

(def app
  (->
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
        hello-route

        (undocumented
         (compojure.route/not-found (not-found {:not "found"}))))
   (ring-cors/wrap-cors :access-control-allow-origin #".*"
                        :access-control-allow-methods [:get :put :post :delete])))

(comment
  (use 'ring.adapter.jetty)
  (run-jetty app {:port 8080})
  )
