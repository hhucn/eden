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

(defn -main
  "Bootstrap everything needed for the provider."
  [& args]
  ;;(bootstrap-dgep-data)
  (pg-listener/start-listeners)
  (retriever/bootstrap)
  (println "Started all Services!")
  (log/debug "Main Bootstrap finished"))
