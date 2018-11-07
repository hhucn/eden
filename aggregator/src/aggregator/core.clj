(ns aggregator.core
  (:require [aggregator.query.retriever :as retriever]
            [aggregator.query.update :as update]
            [aggregator.graphql.dbas-connector :as dbas-conn]
            [aggregator.broker.connector :as broker]
            [aggregator.utils.pg-listener :as pg-listener]
            [aggregator.config :as config]
            [aggregator.search.core :as search]
            [taoensso.timbre :as log])
  (:gen-class))


(defn- bootstrap-dgep-data
  "Get an initial pull on the chosen DGEPs data."
  []
  (doall (map update/update-statement (dbas-conn/get-statements)))
  (doall (map update/update-link (dbas-conn/get-links))))

(defn- load-test-data
  "Loads the testdata inside the db/entrypoint folder. Presumes an arguments.edn and a links.edn is present."
  []
  (doall (map update/update-statement (read-string (slurp "/code/db/entrypoint/arguments.edn"))))
  (doall (map update/update-link (read-string (slurp "/code/db/entrypoint/links.edn"))))
  (log/debug "Read all testdata"))

(defn load-config
  "Load variables from the config and use them correctly,
  by e.g. writing appropriate derivatives into the app-state or similar."
  []
  (swap! config/app-state assoc :known-aggregators config/whitelist))

(defn -main
  "Bootstrap everything needed for the provider."
  [& args]
  (load-config)
  (search/entrypoint)
  (when-not (broker/connected?)
    (broker/init-connection!))
  (broker/create-queue "statements")
  (broker/create-queue "links")
  (load-test-data)
  (bootstrap-dgep-data)
  (pg-listener/start-listeners)
  (retriever/bootstrap) ;; no initial pull needed due to dgep data bootstrap
  (println "Started all Services!")
  (log/debug "Main Bootstrap finished"))
