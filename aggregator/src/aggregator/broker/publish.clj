(ns aggregator.broker.publish
  (:require [clojure.spec.alpha :as s]
            [clojure.data.json :as json]
            [langohr.basic :as lb]
            [aggregator.broker.connector :as connector]
            [aggregator.utils.common :as lib]
            [aggregator.broker.config :as bconf]
            [aggregator.specs :as gspecs]
            [taoensso.timbre :as log]))

(defn- publish
  "Create queue for entity and publish it on this queue."
  [payload routing-key entity-type]
  (let [ch (connector/open-channel)]
    (try
      (lb/publish ch bconf/exchange routing-key (json/write-str payload)
                  {:content-type "application/json" :type (name entity-type)})
      (catch Throwable t (.printStackTrace t))
      (finally (connector/close-channel ch)))))

(defn publish-statement
  "Put a statement to the correct queue. Statement must conform spec to be
  published."
  [statement]
  (when (lib/valid? ::gspecs/statement statement)
    (log/debug (format "About to publish the following statement: %s" statement))
    (publish statement "statements" :statement)
    (lib/return-ok "Statement published")))

(defn publish-link
  "Put a link to the correct queues. Link must conform spec to be published."
  [link]
  (when (lib/valid? ::gspecs/link link)
    (publish link "links" :link)
    (lib/return-ok "Link published.")))


;; -----------------------------------------------------------------------------
;; Specs

(s/fdef publish-statement
        :args (s/cat :statement ::gspecs/statement))
(s/fdef publish-link
        :args (s/cat :link ::gspecs/link))


;; -----------------------------------------------------------------------------
;; Testing-Area

(comment
  (def statement (ffirst (s/exercise ::gspecs/statement)))

  (connector/init-connection!)
  (connector/close-connection!)
  (doall
   (map connector/create-queue
        ["welt.de" "zeit.de" "faz.net" "iamgro.ot" "iamgro.ot" "nobo.dy"]))
  (publish-statement statement)
  )
