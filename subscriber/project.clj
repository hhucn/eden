(defproject subscriber "0.1.0-SNAPSHOT"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.9.0-alpha17"]
                 [com.novemberain/langohr "4.1.0"]
                 [org.clojure/core.async "0.3.443"]]

  :profiles {:dev {:dependencies [[org.clojure/test.check "0.9.0"]]}
             :repl {:plugins [[cider/cider-nrepl "0.15.0-SNAPSHOT"]]}
             :uberjar {:aot :all}}

  :plugins [[lein-kibit "0.1.5"]
            [lein-ancient "0.6.10"]]

  :main ^:skip-aot subscriber.core
  :target-path "target/%s")
