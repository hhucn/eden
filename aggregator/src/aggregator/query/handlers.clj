(ns aggregator.query.handlers
  (:require [clj-time.core :as time]
            [clj-time.coerce :as tc]
            [clojure.data.json :as json]
            [argapi.cache :as cache]))

;; This file contains utility functions triggered by the different REST API calls.

(defn- add-timestamp [data]
  (assoc data :time (tc/to-date (time/now))))

;; ----------------------------------
;; Response

;; POST

;; GET
(defn args []
  {:status :ok
   :data (cache/get-cached-statements)})
