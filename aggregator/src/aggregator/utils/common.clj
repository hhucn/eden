(ns aggregator.utils.common
  "Common functions, which can be used in several namespaces."
  (:require [clojure.spec.alpha :as s]
            [clojure.data.json :as json]
            [taoensso.timbre :as log]
            [aggregator.specs]))

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

(defn- return-map
  "Construct map containing information for the caller."
  [status message] {:status status :message message})

(defn return-error
  "Sometimes you want to return an error message. This function packs it into a
  map."
  [message] (return-map :error message))

(defn return-ok
  "Return ok and a message."
  [message] (return-map :ok message))

;; -----------------------------------------------------------------------------
;; Specs

(s/fdef json->edn
        :args (s/cat :json string?)
        :ret map?)

(s/fdef uuid
        :ret uuid?)

(s/fdef return-map
        :args (s/cat :status ::gspecs/status :message ::gspecs/message)
        :ret ::gspecs/error
        :fn #(and (= (-> % :args :message) (-> % :ret :message))
                  (= (-> % :args :status) (-> % :ret :status))))

(s/fdef return-error
        :args (s/cat :message ::gspecs/message)
        :ret ::gspecs/error
        :fn #(= (-> % :args :message) (-> % :ret :message)))

(s/fdef return-ok
        :args (s/cat :message ::gspecs/message)
        :ret ::gspecs/error
        :fn #(= (-> % :args :message) (-> % :ret :message)))
