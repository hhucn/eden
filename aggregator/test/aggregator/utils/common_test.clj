(ns aggregator.utils.common-test
  (:require [clojure.test :refer [deftest are is]]
            [clojure.spec.alpha :as s]
            [clojure.data.json :as json]
            [aggregator.utils.common :as lib]
            [aggregator.utils.testing :as tlib]
            [aggregator.specs]))

(alias 'gspecs 'aggregator.specs)

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

(deftest return-map
  (tlib/check' 'aggregator.utils.common/return-map))

(deftest return-error
  (tlib/check' 'aggregator.utils.common/return-error))

(deftest return-ok
  (tlib/check' 'aggregator.utils.common/return-ok))
