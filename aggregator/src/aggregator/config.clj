(ns aggregator.config)

;; ------------------------
;; App State

(def app-state
  "Used to save global app-state."
  (atom {}))

;; ------------------------

(def whitelist
  "Allow automatic retrieval of statements and linksfrom all aggregators in the whitelist."
  #{"aggregator:8888" "aggregator_set2:8888"})

(def aggregate-name
  "Own name that is advertised."
  (System/getenv "AGGREGATOR_NAME"))

(def protocol
  "http://")

;; -----------------------------------------------------------------------------
;; Broker-Configuration

(def blacklist
  #{"evil.com"})
