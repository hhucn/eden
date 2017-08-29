(ns aggregator.query.query-test
  (:require [aggregator.specs]
            [clojure.spec.alpha :as s]
            [clojure.spec.test.alpha :as stest]
            [aggregator.query.query :as query]
            [clojure.test :refer :all]))

(alias 'gspecs 'aggregator.specs)

(s/exercise ::gspecs/statement)

;; Here be tests
