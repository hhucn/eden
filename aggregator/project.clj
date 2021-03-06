(defproject aggregator "11"
  :description "The aggregator module for the EDEN framework. The aggregator coordinates the internal and external dataflows of an EDEN instance."
  :url "http://example.com/FIXME"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[de.hhu.cn/postgres-listener "0.2.1"]
                 [org.clojure/clojure "1.10.0"]
                 [org.clojure/data.json "0.2.6"]
                 [org.clojure/core.cache "0.7.2"]
                 [org.clojure/core.async "0.4.490"]
                 [org.postgresql/postgresql "42.2.5"]
                 [com.taoensso/timbre "4.10.0"]  ;; logging lib
                 [cc.qbits/spandex "0.6.4"] ;; query-lib for elasticsearch
                 [compojure "1.6.1"]
                 [ring/ring-defaults "0.3.2"]
                 [ring/ring-json "0.4.0"]
                 [ring/ring-mock "0.3.2"]
                 [ring/ring-jetty-adapter "1.7.1"]
                 [ring-cors "0.1.13"]
                 [clj-http "3.9.1"]
                 [codox-theme-rdash "0.1.2"]
                 [metosin/compojure-api "2.0.0-alpha28"]
                 [metosin/spec-tools "0.8.3"]
                 [cheshire "5.8.1"]]

  :profiles {:dev {:dependencies [[org.clojure/test.check "0.10.0-alpha3"]]
                   :plugins [[cider/cider-nrepl "0.17.0-SNAPSHOT"]
                             [refactor-nrepl "2.4.0-SNAPSHOT"]]}
             :uberjar {:aot :all}}

  :plugins [[lein-kibit "0.1.6"]
            [lein-ancient "0.6.15"]
            [lein-ring "0.12.5"]
            [lein-cloverage "1.0.13"]
            [lein-codox "0.10.4"]]

  :ring {:handler aggregator.api.routes/app
         :init aggregator.core/-main
         :port 8888
         :nrepl {:start? true
                 :port 7777
                 :host "0.0.0.0"}}

  :main ^:skip-aot aggregator.core
  :target-path "target/%s"

  :local-repo ".m2/repo"

  :codox {:metadata {:doc/format :markdown}
          :themes [:rdash]})
