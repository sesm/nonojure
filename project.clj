(defproject nonojure "0.0.1-SNAPSHOT"
  :description "Nonogram puzzles"
  :source-paths ["src/clj"]

  :plugins [[lein-cljsbuild "1.0.0"]
            [org.clojars.nbeloglazov/lein-garden "0.1.0-SNAPSHOT"]
            [lein-midje "3.1.1"]]

  :dependencies [[org.clojure/clojure "1.5.1"]
                 [org.clojure/clojurescript "0.0-2014"]
                 [org.clojure/core.async "0.1.242.0-44b1e3-alpha"]

                 [com.taoensso/timbre "2.6.1"]
                 [http-kit "2.1.10"]
                 [ring/ring-json "0.2.0"]
                 [cheshire "5.0.2"]
                 [compojure "1.1.6"]
                 [com.novemberain/monger "1.5.0"]
                 [org.clojure/tools.reader "0.7.10"]
                 [org.clojure/core.cache "0.6.3"]

                 [midje "1.5.1"]

                 [prismatic/dommy "0.1.2"]
                 [rm-hull/monet "0.1.9"]]
  :main nonojure.runner
  :aot [nonojure.runner]
  :cljsbuild {:builds
              [{:id "dev"
                :source-paths ["src/cljs" "src/cljs-dev"]
                :compiler {:output-to "resources/public/js/main.js"
                           :pretty-print true
                           :optimizations :whitespace
                           :print-input-delimiter true}}
               {:id "prod"
                :source-paths ["src/cljs"]
                :compiler {:output-to "resources/public/js/main.js"
                           :pretty-print false
                           :optimizations :advanced
                           :print-input-delimiter false}}]}

  :garden {:source-path "src/garden"
           :output-path "resources/public/css"})
