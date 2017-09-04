(ns aggregator.query.retriever
  (:require [aggregator.settings :as settings]))



(defn start-retriever
  "Starts a separate process, which uses cyclic retrieval of known related statements from aggregators that are whitelisted."
  []
  :build-list-of-foreign-arguments
  :build-list-of-arguments-to-retrieve
  :retrieve-them
  :repeat?)
