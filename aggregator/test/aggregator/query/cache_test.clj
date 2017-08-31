(ns aggregator.query.cache-test
  (:require [aggregator.query.cache :as cache]
            [clojure.test :refer :all]))

(deftest deliver-missing
  (is (= (cache/retrieve "whatever_nonexistentsaklfhjjklshfSUDIOAH")
         :missing)))

(deftest hit-stuff
  (cache/cache-miss "foo" "bar")
  (cache/cache-miss "bar" "baz")
  (cache/cache-miss "boom" "bap")
  (cache/cache-miss "alex" "wegi")
  (is (= (cache/retrieve "boom")
         "bap"))
  (is (= (cache/get-cached-statements)
         {"foo" "bar" "bar" "baz" "boom" "bap"})))
