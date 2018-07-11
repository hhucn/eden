(ns aggregator.query.utils
  (:require [clojure.data.json :as json]
            [clojure.walk :as walk]
            [clojure.string :as str]))

(defn http-response->map
  [response]
  (-> response
      :body
      (json/read-str :key-fn keyword)))

(defn underscore-keys
  [input]
  (let [hyphen->underscore #(-> % str (str/replace "-" "_") (subs 1) keyword)
        transform-map (fn [form]
                        (if (map? form)
                          (reduce-kv (fn [acc k v] (assoc acc (hyphen->underscore k) v)) {} form)
                          form))]
    (walk/postwalk transform-map input)))

(defn build-cache-pattern
  "Returns the appropriate and current cache URI pattern from entity."
  ([entity]
   (let [id (:identifier entity)]
     (str (:aggregate-id id) "/" (:entity-id id) "/" (:version id))))
  ([aggregate-id entity-id version]
   (str aggregate-id "/" entity-id "/" version)))
