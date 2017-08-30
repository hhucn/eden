(defproject aggregator "0.1.0-SNAPSHOT"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.9.0-alpha19"]
                 [org.clojure/tools.logging "0.4.0"]
                 [org.clojure/data.json "0.2.6"]
                 [org.clojure/core.cache "0.6.5"]
                 [org.postgresql/postgresql "42.1.4"]
                 [com.novemberain/langohr "4.1.0"]
                 [korma "0.4.3"]
                 [compojure "1.6.0"]
                 [ring/ring-defaults "0.3.1"]
                 [ring/ring-json "0.4.0"]
                 [ring/ring-mock "0.3.1"]
                 [ring/ring-jetty-adapter "1.6.2"]
                 [clj-http "3.7.0"]]
  :profiles {:dev {:dependencies [[org.clojure/test.check "0.9.0"]]}
             :repl {:plugins [[cider/cider-nrepl "0.15.1-SNAPSHOT"]]}
             :uberjar {:aot :all}}

  :plugins [[lein-kibit "0.1.5"]
            [lein-ancient "0.6.10"]
            [lein-ring "0.9.7"]]

  :ring {:handler aggregator.query.routes/app}

  :main ^:skip-aot aggregator.core
  :target-path "target/%s")
