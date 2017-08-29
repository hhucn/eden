(ns aggregator.query.utils
  (:require [clojure.data.json :as json]))

(defn http-response->map
  [response]
  (-> response
      :body
      (json/read-str :key-fn keyword)))
