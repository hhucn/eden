(ns aggregator.config)

;; ------------------------
;; App State

(def app-state (atom {}))

;; ------------------------

(def whitelist
  #{"aggregator:8888" "aggregator_set2:8888"})

(def aggregate-name
  (System/getenv "AGGREGATOR_NAME"))

(def protocol
  "http://")

;; -----------------------------------------------------------------------------
;; Broker-Configuration

(def subscribe-to
  #{"broker"})

(def blacklist
  #{"evil.com"})
