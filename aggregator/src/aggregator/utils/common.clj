(ns aggregator.utils.common
  "Common functions, which can be used in several namespaces."
  (:require [clojure.spec.alpha :as s]
            [clojure.data.json :as json]
            [taoensso.timbre :as log]))

(def es-special-characters
  "These are the special characters from elasticsearch and their fitting escaped
  versions in Clojure. They are used to escape query strings when querying the
  database."
  {\+ "\\*", \- "\\-", \& "\\&", \| "\\|", \! "\\!",
   \( "\\(", \) "\\)", \{ "\\{", \} "\\}",
   \[ "\\[", \] "\\]", \^ "\\^", \" "\\\"",
   \~ "\\~", \* "\\*", \? "\\?", \: "\\:"})


;; -----------------------------------------------------------------------------

(defn valid?
  "Verify that data conforms to spec. Calls clojure.spec/explain-str to show a
  useful error message. Prints output to logs and returns a Boolean."
  [spec data]
  (if (s/valid? spec data)
    true
    (do (log/debug (s/explain-str spec data))
        false)))

(defn json->edn
  "Try to parse payload. Return EDN if payload is json. Else return String as
  provided by postgres."
  [payload]
  (try
    (json/read-str payload :key-fn keyword)
    (catch Exception e
      payload)))

(defn uuid
  "Returns a random UUID.
  Wrapper around java.util.UUID/randomUUID."
  [] (java.util.UUID/randomUUID))

(defn- return-map
  "Construct map containing information for the caller."
  [status message data]
  (let [m {:status status :message message}]
    (if-not (nil? data) (merge {:data data} m) m)))

(defn return-error
  "Sometimes you want to return an error message. This function packs it into a
  map."
  ([message] (return-error message nil))
  ([message data]
   (return-map :error message data)))

(defn return-ok
  "Return ok and a message."
  ([message] (return-ok message nil))
  ([message data] (return-map :ok message data)))


(defn time-now-str
  "Returns the current time as an Linux Epoch Time String."
  []
  (str (.getEpochSecond (java.time.Instant/now))))

(defn complete-reference
  "Takes a reference and a referer and delivers the complete statement where parts are missing."
  [reference referer]
  (let [uri (java.net.URI/create referer)
        path (.getPath uri)
        host (.getHost uri)
        port (.getPort uri)
        full-host (if (= -1 port) host (str host ":" port))]
    (merge {:host full-host
            :path path}
           reference)))

(defn complete-multiple-references
  [references referer]
  (map #(complete-reference % referer) references))

(defn build-additionals
  [additionals referer]
  (if (contains? additionals :references)
    (assoc additionals :references
           (complete-multiple-references (:references additionals) referer))
    additionals))

;; -----------------------------------------------------------------------------
;; Specs

(s/fdef json->edn
        :args (s/cat :json string?)
        :ret map?)

(s/fdef uuid
        :ret uuid?)

;; Return-maps
(s/def ::return-map-args
  (s/or :msg-only (s/cat :message ::message)
        :with-data (s/cat :message ::message :data ::data)))

(s/def ::status keyword?)
(s/def ::message string?)
(s/def ::data (s/or :data map? :no-data nil?))
(s/def ::return-map (s/keys :req-un [::status ::message]
                            :opt-un [::data]))

(s/fdef return-map
        :args (s/cat :status ::status :message ::message :data ::data)
        :ret ::return-map)

(s/fdef return-error
        :args ::return-map-args
        :ret ::return-map
        :fn #(and (= (-> % :args :message) (-> % :ret :message))
                  (= (-> % :ret :status) :error)))

(s/fdef return-ok
        :args ::return-map-args
        :ret ::return-map
        :fn #(and (= (-> % :args second :message) (-> % :ret :message))
                  (= (-> % :ret :status) :ok)))
