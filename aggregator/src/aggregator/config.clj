(ns aggregator.config)

(def whitelist
  ["foo.bar"
   "bar.baz"
   "großeweiteweltderdiskussionen.de"])


;; -----------------------------------------------------------------------------
;; Broker-Configuration

(def subscribe-to
  #{"broker"})

(def blacklist
  #{"evil.com"})
