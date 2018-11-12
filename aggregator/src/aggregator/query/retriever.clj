(ns aggregator.query.retriever
  (:require [aggregator.config :as config]
            [aggregator.query.query :as query]
            [aggregator.query.cache :as cache]
            [taoensso.timbre :as log]))

(defn whitelisted?
  "Return whether the aggregator of an entity is whitelisted."
  [link]
  (some #{(get-in link [:identifier :aggregate-id])} config/whitelist))

(defn- statement?
  "Is the argument a statement?"
  [entity]
  (when (contains? entity :content)
    true))

(defn lookup-related-breadth-first
  [entity]
  (loop [[head & rest] [entity]]
    ;; Sleep half a second to not hog many resources
    (Thread/sleep 500)
    (log/debug (format "Continuing background retrieval with head %s" head))
    (let [aggregate-id (get-in head [:identifier :aggregate-id])
          entity-id (get-in head [:identifier :entity-id])
          version (get-in head [:identifier :version])]
      (if (whitelisted? head)
        (if (statement? head)
          ;; Head is a statement - Continue with rest + all links to head
          (let [links-to (query/links-to aggregate-id entity-id version)
                continue-queue (concat rest links-to)]
            (some-> (seq continue-queue) recur))
          ;; Head is a link - Get all undercuts to it and the links source if possible
          (let [undercuts-to (query/retrieve-undercuts aggregate-id entity-id)
                source-aggregator (get-in head [:source :aggregate-id])
                source-entity-id (get-in head [:source :entity-id])
                source-version (get-in head [:source :version])
                ; Put the statement in a vector for easier concat
                source-statement [(query/retrieve-exact-statement source-aggregator
                                                                  source-entity-id
                                                                  source-version)]
                continue-queue (concat rest undercuts-to source-statement)]
            (some-> (seq continue-queue) recur)))
        ;; Aggregator not whitelisted - Recur with rest when there are elements left
        (some-> (seq rest) recur)))))

(defn automatic-retriever
  "Starts an automatic retriever that looks up statements and links related to things contained in the cache."
  []
  (future
    (loop [starter (rand-nth (vals (cache/get-cached-statements)))]
      (lookup-related-breadth-first starter)
      (log/debug "[retriever] sleeping")
      (Thread/sleep 60000)
      (log/debug "[retriever] Automatic search waking up.")
      (query/remote-starter-set)
      (recur (rand-nth (vals (cache/get-cached-statements)))))))

(defn bootstrap
  "Call this method when the aggregator starts. Pulls the whitelisted aggregators
  for a starting-set of arguments, puts them into the cache and then spins up
  the automatic retriever."
  []
  (log/debug "PULLING related starter-set")
  (query/all-remote-statements)
  (query/all-remote-links)
  (log/debug "Pulled a random starter set from whitelisted aggregators successfully.")
  (automatic-retriever))
