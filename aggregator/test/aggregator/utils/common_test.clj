(ns aggregator.utils.common-test
  (:require [clojure.test :refer [deftest are is]]
            [clojure.spec.alpha :as s]
            [clojure.data.json :as json]
            [aggregator.utils.common :as lib]
            [aggregator.specs :as gspecs]))

(def statement (second (last (s/exercise ::gspecs/statement))))

(deftest valid?-test
  (are [x y] (= x y)
    true (lib/valid? string? "")
    false (lib/valid? string? :foo)
    false (lib/valid? string? nil)))

(deftest json->edn
  (are [x y] (= x y)
    nil (lib/json->edn (json/write-str nil))
    "iamgroot" (lib/json->edn (json/write-str :iamgroot))
    {:foo "bar"} (lib/json->edn (json/write-str {:foo :bar}))
    "{\"invalid\"" (lib/json->edn "{\"invalid\"")))

(deftest uuid
  (is (s/valid? uuid? (lib/uuid))))

(deftest return-error
  (are [x y] (= x y)
    [:status :message] (keys (lib/return-error "Message"))
    {:status :error :message "Message" :data {:foo :bar}} (lib/return-error "Message" {:foo :bar})))

(deftest return-ok
  (are [x y] (= x y)
    [:status :message] (keys (lib/return-ok "Message"))
    {:status :ok :message "Message" :data {:foo :bar}} (lib/return-ok "Message" {:foo :bar})))
