(ns aggregator.utils.db-test
  (:require [aggregator.utils.db :as db]
            [clojure.test :refer :all]))


(deftest statement-uri-retrieval-test
  (is (= (:version (first (db/statement-by-uri "hhu.de/34")))
         1))
  (is (= (:aggregate_id (first (db/statement-by-uri "hhu.de/P12")))
         "hhu.de"))
  (is (= (:entity_id (first (db/statement-by-uri "hhu.de/P13")))
         "P13"))
  (is (= (:content (first (db/statement-by-uri "hhu.de/P22")))
         "the city is planing a new park in the upcoming month"))
  (is (= (:author (first (db/statement-by-uri "hhu.de/7")))
         "Bolek")))
