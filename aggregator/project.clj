(defproject aggregator "0.1.0-SNAPSHOT"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.9.0-alpha20"]
                 [org.clojure/data.json "0.2.6"]
                 [org.clojure/core.cache "0.6.5"]
                 [org.clojure/core.async "0.3.443"]
                 [org.postgresql/postgresql "42.1.4"]
                 [com.novemberain/langohr "4.1.0"]
                 [com.taoensso/timbre "4.10.0"]  ;; logging lib
                 [korma "0.4.3"]
                 [compojure "1.6.0"]
                 [ring/ring-defaults "0.3.1"]
                 [ring/ring-json "0.4.0"]
                 [ring/ring-mock "0.3.1"]
                 [ring/ring-jetty-adapter "1.6.2"]
                 [clj-http "3.7.0"]]

  :profiles {:dev {:dependencies [[org.clojure/test.check "0.9.0"]]
                   :plugins [[cider/cider-nrepl "0.15.1"]
                             [refactor-nrepl "2.4.0-SNAPSHOT"]]}
             :uberjar {:aot :all}}

  :plugins [[lein-kibit "0.1.5"]
            [lein-ancient "0.6.10"]
            [lein-ring "0.9.7"]
            [lein-cloverage "1.0.9"]
            [nightlight/lein-nightlight "1.7.2"]]

  :ring {:handler aggregator.api.routes/app
         :port 8888
         :nrepl {:start? true
                 :port 7777
                 :host "0.0.0.0"}}

  :main ^:skip-aot aggregator.core
  :target-path "target/%s")
