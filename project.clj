(defproject nonojure "0.0.1-SNAPSHOT"
  :description "Nonogram puzzles"
  :source-paths ["src/clj"]

  :plugins [[lein-cljsbuild "1.0.0"]
            [org.clojars.nbeloglazov/lein-garden "0.1.0-SNAPSHOT"]
            [com.cemerick/clojurescript.test "0.2.1"]
            [lein-midje "3.1.1"]]

  :dependencies [[org.clojure/clojure "1.5.1"]
                 [org.clojure/clojurescript "0.0-2014"]
                 [org.clojure/core.async "0.1.267.0-0d7780-alpha"]

                 [com.taoensso/timbre "2.6.1"]
                 [http-kit "2.1.10"]
                 [ring/ring-json "0.2.0"]
                 [cheshire "5.0.2"]
                 [compojure "1.1.6"]
                 [com.novemberain/monger "1.6.0"]
                 [org.clojure/tools.reader "0.7.10"]
                 [org.clojure/core.cache "0.6.3"]

                 [midje "1.5.1"]

                 [prismatic/dommy "0.1.2"]
                 [rm-hull/monet "0.1.9"]]
  :main nonojure.runner
  :aot [nonojure.runner]
  :cljsbuild {:builds
              [{:id "dev"
                :source-paths ["src/cljs"]
                :compiler {:output-to "resources/public/js/main.js"
                           :pretty-print true
                           :optimizations :whitespace
                           :print-input-delimiter true}}

               {:id "prod"
                :source-paths ["src/cljs"]
                :compiler {:output-to "resources/public/js/main.js"
                           :pretty-print false
                           :optimizations :advanced
                           :print-input-delimiter false
                           :externs ["externs.js"]}}
               {:id "test"
                :source-paths ["src/cljs" "test/cljs"]
                :compiler {:output-to "target/cljs/testable.js"
                           :optimizations :whitespace
                           :pretty-print true}}]

              :crossovers [nonojure.shared]
              :test-commands {"unit-tests" ["phantomjs" "test/runner.js"
                                            "target/cljs/testable.js"]}}

  :garden {:source-path "src/garden"
           :output-path "resources/public/css"})
