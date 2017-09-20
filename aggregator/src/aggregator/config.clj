(ns aggregator.config)

(def whitelist
  #{"foo.bar"
    "bar.baz"
    "großeweiteweltderdiskussionen.de"})

(def self-hostnames
  #{"hhu.de"})
(comment "All hostnames the provider can has.")

;; -----------------------------------------------------------------------------
;; Broker-Configuration

(def subscribe-to
  #{"broker"})

(def blacklist
  #{"evil.com"})
