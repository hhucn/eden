(ns aggregator.api.routes
  "Define and expose the routes for the REST API in this file."
  (:require [compojure.route]
            [compojure.api.sweet :refer [GET POST api context resource undocumented]]
            [aggregator.query.query :as query]
            [aggregator.utils.common :as utils]
            [aggregator.query.update :as update]
            [spec-tools.spec :as spec]
            [clojure.spec.alpha :as s]
            [aggregator.specs :as eden-specs]
            [ring.util.http-response :refer [ok not-found created]]
            [ring.middleware.cors :as ring-cors]))

(s/def ::welcome-message spec/string?)
(s/def ::statements (s/coll-of ::eden-specs/statement))
(s/def ::statements-map (s/keys :req-un [::statements]))
(s/def ::minimal-statement (s/keys :req-un [::eden-specs/content-string ::eden-specs/author]))
(s/def ::premise ::minimal-statement)
(s/def ::conclusion ::minimal-statement)
(s/def ::links (s/coll-of ::eden-specs/link))
(s/def ::links-map (s/keys :req-un [::links]))
(s/def ::statement-map (s/keys :req-un [::eden-specs/statement]))
(s/def ::link-map (s/keys :req-un [::eden-specs/link]))
(s/def ::premise-name ::eden-specs/identifier)
(s/def ::conclusion-name ::eden-specs/identifier)
(s/def ::link-name ::eden-specs/identifier)
(s/def ::new-argument (s/keys :req-un [::premise-name ::conclusion-name]
                              :opt-un [::link-name]))
(s/def ::link-type #{"support" "attack" "undercut"})
(s/def ::minimal-argument (s/keys :req-un [::premise ::conclusion ::link-type]))

(def argument-routes
  (context "/argument" []
           :tags ["argument"]
           :coercion :spec

           (POST "/" []
                 :summary "Add a new argument to the EDEN database."
                 :body [request-body ::minimal-argument]
                 :return ::new-argument
                 (created
                  (let [premise (:premise request-body)
                        conclusion (:conclusion request-body)
                        link-type (:link-type request-body)]
                    (update/add-argument premise conclusion link-type))))))

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
                                                  {:opts [:no-remote]})}))

    (GET "/custom" []
         :summary "Returns all statements matching the search term in a custom field"
         :query-params [custom-field :- spec/string?
                        search-term :- spec/string?]
         :return ::statements-map
         (ok {:statements (query/custom-statement custom-field search-term)}))))

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
                  (not-found nil)))

           (POST "/" []
                 :summary "Add a statement to the EDEN database"
                 :body [statement ::eden-specs/statement]
                 :return ::statement-map
                 (ok {:statement (update/update-statement (utils/json->edn statement))}))))

(defn wrap-link-type [handler]
  (fn [request]
    (if-let [type (-> request :body-params :type)]
      (handler (assoc-in request [:body-params :type] (keyword type)))
      (handler request))))

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
                  (not-found nil)))

           (POST "/" []
                 :summary "Add a link to the EDEN database"
                 :middleware [wrap-link-type]
                 :body [link ::eden-specs/link]
                 :return ::link-map
                 (ok {:link (update/update-link (utils/json->edn link))}))))

(def app
  (let [compojure-api-handler
        (api {:coercion :spec
              :swagger
              {:ui "/"
               :spec "/swagger.json"
               :data {:info {:title "EDEN Aggregator API"
                             :description "An API to request statements and links from the EDEN instance."}
                      :tags [{:name "statements" :description "Retrieve Statements"}
                             {:name "links" :description "Retrieve Links"}
                             {:name "statement" :description "Retrieve or add a single specific statement"}
                             {:name "link" :description "Retrieve or add a single specific link"}
                             {:name "argument" :description "Retrive or add whole arguments"}]}}}
             statement-routes
             statements-routes
             link-routes
             links-routes
             argument-routes
             (undocumented
              (compojure.route/not-found (not-found {:not "found"}))))]
    (ring-cors/wrap-cors
     compojure-api-handler
     :access-control-allow-origin #".*"
     :access-control-allow-methods [:get :put :post :delete])))

(comment
  (use 'ring.adapter.jetty)
  (run-jetty app {:port 8080})
  )
