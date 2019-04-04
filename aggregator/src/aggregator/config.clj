(ns aggregator.config)

;; ------------------------
;; App State

(def app-state
  "Used to save global app-state."
  (atom {}))

;; ------------------------

(def whitelist
  "Allow automatic retrieval from all aggregators in the whitelist."
  #{"aggregator:8888" "aggregator_set2:8888"})

(def remote-brokers
  "Define known brokers which are always subscribed if possible for queues statements and links."
  #{["localhost" 5672]})

(def aggregate-name
  "Own name that is advertised."
  (System/getenv "AGGREGATOR_NAME"))

(def protocol
  "http://")

;; -----------------------------------------------------------------------------
;; Broker-Configuration

(def subscribe-to
  #{"broker"})

(def blacklist
  #{"evil.com"})
