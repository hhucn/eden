(ns aggregator.utils.common-test
  (:require [clojure.test :refer [deftest are is]]
            [clojure.spec.alpha :as s]
            [aggregator.utils.common :as lib]
            [aggregator.specs]))

(alias 'gspecs 'aggregator.specs)

(def statement (second (last (s/exercise ::gspecs/statement))))

(deftest valid?-test
  (are [x y] (= x y)
    true (lib/valid? string? "")
    false (lib/valid? string? :foo)
    false (lib/valid? string? nil)
    true (lib/valid? ::gspecs/statement statement)))
