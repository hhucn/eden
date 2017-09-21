(ns aggregator.config)

(def whitelist
  #{"foo.bar"
    "bar.baz"
    "großeweiteweltderdiskussionen.de"})

(def aggregate-name
  "hhu.de") 

;; -----------------------------------------------------------------------------
;; Broker-Configuration

(def subscribe-to
  #{"broker"})

(def blacklist
  #{"evil.com"})
