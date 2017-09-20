(ns aggregator.core
  (:require [aggregator.query.retriever :as retriever])
  (:gen-class))

(defn -main
  "Bootstrap everything needed for the provider."
  [& args]
  (retriever/bootstrap)
  (println "Started all Services!"))
