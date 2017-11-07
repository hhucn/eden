(ns aggregator.core
  (:require [aggregator.query.retriever :as retriever]
            [aggregator.query.update :as update]
            [aggregator.graphql.dbas-connector :as dbas-conn]
            [aggregator.utils.pg-listener :as pg-listener]
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
  (doall (map update/update-statement (read-string (slurp "/db/entrypoint/arguments.edn"))))
  (doall (map update/update-link (read-string (slurp "/db/entrypoint/links.edn"))))
  (log/debug "Read all testdata"))

(defn -main
  "Bootstrap everything needed for the provider."
  [& args]
  (load-test-data)
  (bootstrap-dgep-data)
  (pg-listener/start-listeners)
  (retriever/bootstrap) ;; no initial pull needed due to dgep data bootstrap
  (println "Started all Services!")
  (log/debug "Main Bootstrap finished"))
