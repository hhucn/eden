(ns aggregator.api.routes
  "Define and expose the routes for the REST API in this file."
  (:require [compojure.route]
            [compojure.api.sweet :refer [GET POST api context resource undocumented]]
            [compojure.api.exception :as ex]
            [aggregator.query.query :as query]
            [aggregator.utils.common :as utils]
            [aggregator.query.update :as update]
            [spec-tools.spec :as spec]
            [clojure.spec.alpha :as s]
            [aggregator.specs :as eden-specs]
            [ring.util.http-response :refer [ok not-found created]]
            [ring.middleware.cors :as ring-cors]
            [taoensso.timbre :as log]))

(s/def ::welcome-message spec/string?)
(s/def ::statements (s/coll-of ::eden-specs/statement))
(s/def ::statements-map (s/keys :req-un [::statements]))

(s/def ::reference (s/keys :req-un [::eden-specs/text]
                           :opt-un [::eden-specs/path ::eden-specs/host]))
(s/def ::references (s/coll-of ::reference))
(s/def ::statement (s/keys :req-un [::eden-specs/content ::eden-specs/identifier
                                    ::eden-specs/predecessors ::eden-specs/delete-flag]
                           :opt-un [::references]))


(s/def ::premise ::eden-specs/text)
(s/def ::conclusion ::eden-specs/text)

(s/def ::links (s/coll-of ::eden-specs/link))
(s/def ::links-map (s/keys :req-un [::links]))
(s/def ::statement-map (s/keys :req-un [::eden-specs/statement]))
(s/def ::link-map (s/keys :req-un [::eden-specs/link]))

(s/def ::premise-name ::eden-specs/identifier)
(s/def ::conclusion-name ::eden-specs/identifier)
(s/def ::link-name ::eden-specs/identifier)
(s/def ::new-argument (s/keys :req-un [::premise-name ::conclusion-name]
                              :opt-un [::link-name]))

(s/def ::author-id ::eden-specs/id)
(s/def ::link-type #{"support" "attack" "undercut"})

(s/def ::additional (s/keys :opt-un [::references]))
(s/def ::additional-premise (s/keys :opt-un [::references]))
(s/def ::additional-conclusion (s/keys :opt-un [::references]))
(s/def ::minimal-argument (s/keys :req-un [::premise ::conclusion ::link-type ::author-id]
                                  :opt-un [::additional-premise ::additional-conclusion]))

(s/def ::quick-statement-body (s/keys :req-un [::eden-specs/text ::author-id]
                                      :opt-un [::additional]))
(s/def ::quicklink-request (s/keys :req-un [::eden-specs/type ::eden-specs/source
                                            ::eden-specs/destination ::author-id]))

(s/def ::arguments (s/coll-of ::eden-specs/argument))
(s/def ::arguments-map (s/keys :req-un [::arguments]))

(def argument-routes
  (context "/argument" []
           :tags ["argument"]
           :coercion :spec

           (POST "/" request
             :summary "Add a new argument to the EDEN database."
             :body [request-body ::minimal-argument]
             :return ::new-argument
             (created
              "/argument"
              (let [referer (get-in request [:headers "referer"])
                    premise (:premise request-body)
                    conclusion (:conclusion request-body)
                    link-type (:link-type request-body)
                    author-id (:author-id request-body)
                    additional-p (utils/build-additionals (:additional-premise request-body) referer)
                    additional-c (utils/build-additionals (:additional-conclusion request-body) referer)]
                (update/add-argument premise conclusion link-type author-id additional-p additional-c))))))

(def arguments-routes
  (context "/arguments" []
           :tags ["arguments"]
           :coercion :spec

           (GET "/" []
                :summary "Returns all arguments"
                :query-params []
                :return ::arguments-map
                (ok {:arguments (query/all-arguments)}))

           (GET "/by-author" []
                :summary "Return all arguments by specific author"
                :query-params [author-name :- spec/string?]
                :return ::arguments-map
                (ok {:arguments (query/arguments-by-author author-name)}))))

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

    (GET "/by-reference-host" []
      :summary "Returns all statements matching the references host"
      :query-params [host :- spec/string?]
      :return ::statements-map
      (ok {:statements (query/statements-by-reference-host host)}))

    (GET "/by-reference-text" []
      :summary "Returns all statements matching the references text"
      :query-params [text :- spec/string?]
      :return ::statements-map
      (ok {:statements (query/statements-by-reference-text text)}))

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
         (ok {:links (query/all-known-links)}))

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

           (POST "/" request
             :summary "Add a statement to the EDEN database"
             :body [statement ::statement]
             :return ::statement-map
             (created
              "/statement"
              (let [referer (get-in request [:headers "referer"])
                    full-statement (utils/build-additionals statement referer)]
                {:statement (update/update-statement full-statement)})))

           (POST "/from-text" request
             :summary "Add a statement only providing text and author-id"
             :body [request-body ::quick-statement-body]
             :return ::statement-map
             (created
              "/statement"
              (let [referer (get-in request [:headers "referer"])
                    text (:text request-body)
                    author-id (:author-id request-body)
                    additional (utils/build-additionals (:additional request-body) referer)]
                {:statement (update/statement-from-text text author-id additional)})))))

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
                 (created
                  "/link"
                  {:link (update/update-link (utils/json->edn link))}))

           (POST "/shorthand" []
             :summary "Add a link via source, destination and author. Autogenerate rest."
             :middleware [wrap-link-type]
             :body [quicklink-request ::quicklink-request]
             :return ::link-map
             (created
              "/link"
              (let [source (:source quicklink-request)
                    destination (:destination quicklink-request)
                    author-id (:author-id quicklink-request)
                    link-type (:type quicklink-request)]
                {:link (update/quicklink link-type source destination author-id)})))))

(def app
  (let [compojure-api-handler
        (api {:coercion :spec
              :exceptions
              {:handlers
               {::ex/request-parsing (ex/with-logging ex/request-parsing-handler :info)
                ::ex/request-validation (ex/with-logging ex/request-validation-handler :error)
                ::ex/response-validation (ex/with-logging ex/response-validation-handler :error)}}
              :swagger
              {:ui "/"
               :spec "/swagger.json"
               :data {:info {:version 0.5
                             :title "EDEN Aggregator API"
                             :description "An API to request statements and links from the EDEN instance."}
                      :tags [{:name "statements" :description "Retrieve Statements"}
                             {:name "links" :description "Retrieve Links"}
                             {:name "statement" :description "Retrieve or add a single specific statement"}
                             {:name "link" :description "Retrieve or add a single specific link"}
                             {:name "argument" :description "Add whole arguments"}
                             {:name "arguments" :description "Retrieve argument objects"}]}}}
             statement-routes
             statements-routes
             link-routes
             links-routes
             argument-routes
             arguments-routes
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
