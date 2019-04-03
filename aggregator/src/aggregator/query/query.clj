(ns aggregator.query.query
  (:require [aggregator.query.cache :as cache]
            [aggregator.query.db :as db]
            [aggregator.query.update :as up]
            [aggregator.query.utils :as utils]
            [aggregator.broker.subscriber :as sub]
            [aggregator.config :as config]
            [clj-http.client :as client]
            [clojure.string :as str]
            [clojure.set :as cset]
            [taoensso.timbre :as log]))

(defn get-data
  "Get data from a remote aggregator."
  ([request-url]
   (get-data request-url {}))
  ([request-url query-params]
   (try
     (:body (client/get request-url {:as :json
                                     :query-params query-params}))
     (catch Exception e
       {}))))

(defn subscribe-to-queue
  "Uses the broker module to subscribe to a queue for updates. Sanitizes the host
  if a port is appended. Example: example.com:8888 is treated as example.com."
  [queue host]
  (let [cleaned-host (first (str/split host #":"))]
    ;; Quick Fix Solution for experiment. Change in next big update
    ;; Then send the queue data with the statements and links
    (if (str/starts-with? cleaned-host "dbas.")
      (sub/subscribe queue {:host (str/replace cleaned-host #"dbas\." "broker.")})
      (sub/subscribe queue {:host cleaned-host}))))

(defn exact-statement
  "Return the exact statement from cache or db"
  ([{:keys [aggregate-id entity-id version]}]
   (exact-statement aggregate-id entity-id version))
  ([aggregate-id entity-id version]
   (let [cached-statement (cache/retrieve (utils/build-cache-pattern aggregate-id entity-id version))]
     (if (and (not= cached-statement :missing)
              (= (get-in cached-statement [:identifier :version]) version))
       cached-statement
       (when-let [maybe-statement (db/exact-statement aggregate-id entity-id version)]
         (do (cache/cache-miss (utils/build-cache-pattern maybe-statement) maybe-statement)
             (log/debug "[query] Found exact statement in DB")
             maybe-statement))))))

(defn local-undercuts
  "Retrieve all links from the db that undercut the link passed as argument."
  [aggregate-id entity-id]
  (db/get-undercuts aggregate-id entity-id))

(defn links-by-target
  "Retrieve all local links in db pointing to the target statement."
  [aggregate-id entity-id version]
  (db/links-by-target aggregate-id entity-id version))

(defn retrieve-remote
  "Try to retrieve a statement from a remote aggregator. The host part is treated as the webhost."
  ([uri]
   (let [split-uri (str/split uri #"/")]
     (retrieve-remote (first split-uri) (second split-uri))))
  ([aggregate-id entity-id]
   (let [request-url (str config/protocol aggregate-id "/statements/by-id")
         result-data (:statements (get-data request-url {:aggregate-id aggregate-id
                                                         :entity-id entity-id}))]
     (subscribe-to-queue "statements" aggregate-id)
     (subscribe-to-queue "links" aggregate-id)
     (doseq [statement result-data] (up/update-statement statement))
     result-data)))

(defn retrieve-exact-statement
  "Retrieves an exact statement from cache / db / a remote aggregator."
  [aggregate entity version]
  (if-let [local-statement (exact-statement aggregate entity version)]
    local-statement
    (let [request-url (str config/protocol aggregate "/statement")
          result-data (client/get request-url {:as :json
                                               :query-params {:aggregate-id aggregate
                                                              :entity-id entity
                                                              :version version}})
          result-body (:body result-data)]
      (when-not (= 404 (:status result-data))
        (up/update-statement (:statement result-body))))))

(defn retrieve-undercuts
  "Retrieve a (possibly remote) list of undercuts. The argument is the link being undercut."
  [aggregate-id entity-id]
  (if-let [possible-undercuts (local-undercuts aggregate-id entity-id)]
    possible-undercuts
    (let [request-url (str config/protocol aggregate-id "/links/undercuts/")
          results (get-data request-url {:aggregate-id aggregate-id
                                         :entity-id entity-id})]
      (doseq [link results] (up/update-link link))
      results)))

(defn links-to
  "Retrieve all links pointing to provided statement. (From the statements aggregator)"
  [aggregate-id entity-id version]
  (if-let [possible-links (links-by-target aggregate-id entity-id version)]
    possible-links
    (let [request-url (str config/protocol aggregate-id "/links/to/")
          results (get-data request-url {:aggregate-id aggregate-id
                                         :entity-id entity-id
                                         :version version})]
      (log/debug (format "Pulled %s downstream links for %s/%s/%s"
                         (count results)
                         aggregate-id entity-id version))
      (doseq [link results] (up/update-link link))
      results)))

(defn check-db
  "Check the database for an entity and update cache after item is found."
  ([uri]
   (check-db uri {}))
  ([uri {:keys [opts]}]
   (let [possible-entity (db/statements-by-uri uri)]
     (if (= possible-entity :missing)
       (if (some #(= % :no-remote) opts)
         []
         (retrieve-remote uri))
       (cache/cache-miss uri possible-entity)))))

(defn tiered-retrieval
  "Check whether the Cache contains the desired entity. If not delegate to DB and remote acquisition."
  ([aggregate-id entity-id]
   (tiered-retrieval aggregate-id entity-id {}))
  ([aggregate-id entity-id options]
   (let [uri (str aggregate-id "/" entity-id)
         cached-entity (cache/retrieve uri)]
     (if (= cached-entity :missing)
       (check-db uri options)
       cached-entity))))

(defn retrieve-link
  "Retrieve a link from cache or db. Returns empty vector if no such link can be found."
  [aggregate-id entity-id version]
  (let [link-uri (str aggregate-id "/" entity-id "/" version)
        cached-link (cache/retrieve-link link-uri)]
    (if (= cached-link :missing)
      (when-let [db-result (db/exact-link aggregate-id entity-id version)]
        (cache/cache-miss-link link-uri db-result))
      cached-link)))


(defn starter-set
  "Retrieve a set of starting arguments, which can be used by remote aggregators to bootstrap the connection. This particular implementation just takes a random set of arguments from the cache or database."
  []
  (db/random-statements 100))

(defn remote-starter-set
  "Retrieve remote starter sets and put them into the cache and db."
  ([]
   (doseq [host config/whitelist] (remote-starter-set host)))
  ([aggregator]
   (let [results (:statements (get-data (str config/protocol aggregator "/statements/starter-set")))]
     (doseq [stmt results] (up/update-statement stmt)))))

(defn all-local-statements
  "Retrieve all locally saved statements belonging to the aggregator."
  []
  (db/all-statements))

(defn all-remote-statements
  "Retrieve all statements from a remote aggregator."
  ([]
   (doseq [aggregator config/whitelist]
     (when (not= aggregator config/aggregate-name)
       (all-remote-statements aggregator))))
  ([aggregator]
   (let [results (:statements (get-data (str config/protocol aggregator "/statements")))]
     (doseq [statement results]
       (up/update-statement statement)))))

(defn all-local-links
  "Retrieve all locally saved statements belonging to the aggregator."
  []
  (db/all-local-links))

(defn all-known-links
  "Retrieve all known links."
  []
  (db/all-links))

(defn all-remote-links
  "Retrieve all links from a remote aggregator."
  ([]
   (doseq [aggregator config/whitelist]
     (when (not= aggregator config/aggregate-name)
       (all-remote-links aggregator))))
  ([aggregator]
   (let [results (:links (get-data (str config/protocol aggregator "/links")))]
     (doseq [link results]
       (up/update-link link)))))

(defn statements-contain
  "Retrieve all statements where the content.text contains the `query`"
  [query]
  (db/statements-contain query))

(defn custom-statement
  "Retrieve a statement with a custom field containg a specific search-term."
  [field search-term]
  (db/custom-statement-search field search-term))

(defn by-author-content
  "Retrieve statements by specific author and narrow them down by conent."
  [author content]
  (let [posts (if (empty? content)
                (db/statements)
                (db/statements-contain content))]
    (filter #(= (get-in % [:content :author :name]) author) posts)))

(defn statements-by-reference-text
  [text]
  (db/statements-by-reference-text text))

(defn statements-by-reference-host
  [host]
  (db/statements-by-reference-host host))

(defn- build-ref-plus
  [{:keys [references content]}]
  (set (map (fn [ref] {:reference ref
                      :author (:author content)})
            references)))

(defn references-by-location
  "Return all references (without the statement) that have a certain host and path"
  [host path]
  (let [matches (db/statements-by-reference-location host path)
        references (map build-ref-plus matches)]
    (reduce cset/union #{} references)))

(defn all-statements
  "Return all statements inside the db."
  []
  (db/statements))

(defn all-references
  "Return all references."
  []
  (->> (db/statements)
       (map :references)
       (remove nil?)
       (remove empty?)
       (reduce #(cset/union %1 (set (flatten %2))) #{})))

(defn- argument-from-link
  "Build the argument from the link. Only handles statements as premise and conclusion."
  [link]
  (let [premise (exact-statement (:source link))
        conclusion (exact-statement (:destination link))]
    (when (and premise conclusion)
      {:link link
       :premise premise
       :conclusion conclusion})))

(defn all-arguments
  "Return all arguments from the DB."
  []
  (let [all-links (all-known-links)
        correct-links (filter #(#{:attack :support} (:type %)) all-links)]
    (map argument-from-link correct-links)))

(defn arguments-by-author
  "Return all arguments created by authors with given name."
  [author-name]
  (let [all-links (all-known-links)
        author-links (filter #(= author-name (get-in % [:author :name])) all-links)
        correct-links (filter #(#{:attack :support} (:type %)) author-links)]
    (map argument-from-link correct-links)))

(defn- match-references
  [argument text host path]
  (let [references (get-in argument [:premise :references])
        filtered-texts (filter #(= text (:text %)) references)
        filtered-hosts (if (empty? host)
                         filtered-texts
                         (filter #(= host (:host %)) references))
        filtered-paths (if (empty? path)
                         filtered-hosts
                         (filter #(= path (:path %)) references))]
    (seq filtered-paths)))

(defn arguments-by-reference
  "Return all arguments where a premise matches a given reference text (and possibly host and path)"
  [text host path]
  (filter #(match-references % text host path) (all-arguments)))
