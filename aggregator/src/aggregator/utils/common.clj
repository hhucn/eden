(ns aggregator.utils.common
  "Common functions, which can be used in several namespaces."
  (:require [clojure.spec.alpha :as s]
            [clojure.data.json :as json]
            [taoensso.timbre :as log]
            [aggregator.specs]
            [clojure.spec.test.alpha :as stest]))

(alias 'gspecs 'aggregator.specs)

(defn valid?
  "Verify that data conforms to spec. Calls clojure.spec/explain-str to show a
  useful error message. Prints output to logs and returns a boolean."
  [spec data]
  (if (s/valid? spec data)
    true
    (do (log/debug (s/explain-str spec data))
        false)))

(defn json->edn
  "Try to parse payload. Return EDN if payload is json. Else return
   string as provided by postgres."
  [payload]
  (try
    (json/read-str payload :key-fn keyword)
    (catch Exception e
      payload)))

(defn uuid [] (java.util.UUID/randomUUID))

(defn return-error
  "Sometimes you want to return an error message. This function packs it into a
  map."
  [message]
  {:status :error
   :message message})

;; -----------------------------------------------------------------------------
;; Specs

(s/fdef json->edn
        :args (s/cat :json string?)
        :ret map?)

(s/fdef uuid
        :ret uuid?)

(s/fdef return-error
        :args (s/cat :message ::gspecs/message)
        :ret ::gspecs/error
        :fn #(= (-> % :args :message) (-> % :ret :message)))
