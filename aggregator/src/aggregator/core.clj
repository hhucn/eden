(ns aggregator.core
  (:require [aggregator.query.retriever :as retriever]
            [taoensso.timbre :as log])
  (:gen-class))

(defn -main
  "Bootstrap everything needed for the provider."
  [& args]
  (retriever/bootstrap)
  (println "Started all Services!")
  (log/debug "Main Bootstrap finished"))
