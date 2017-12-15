(ns aggregator.config)

(def whitelist
  #{"aggregator:8888" "aggregator_set2:8888"})

(def aggregate-name
  (System/getenv "AGGREGATOR-NAME"))


;; -----------------------------------------------------------------------------
;; Broker-Configuration

(def subscribe-to
  #{"broker"})

(def blacklist
  #{"evil.com"})
