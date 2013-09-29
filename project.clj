(defproject nonojure "0.0.1-SNAPSHOT"
  :description "Nonogram puzzles"
  :source-paths ["src/clj"]
  :plugins [[lein-cljsbuild "0.3.3"]]
  :dependencies [[org.clojure/clojure "1.5.1"]
                 [org.clojure/clojurescript "0.0-1909"]

                 [com.taoensso/timbre "2.6.1"]
                 [http-kit "2.1.10"]
                 [ring/ring-json "0.2.0"]
                 [compojure "1.1.5"]
                 [com.novemberain/monger "1.5.0"]

                 [jayq "2.4.0"]
                 [prismatic/dommy "0.1.1"]
                 [rm-hull/monet "0.1.8"]]
  :main nonojure.runner
  :aot [nonojure.runner]
  :cljsbuild {:builds
              [{:source-paths ["src/cljs"],
                :compiler {:output-dir "resources/public/js/",
                           :output-to "resources/public/js/main.js",
                           :pretty-print true,
                           :optimizations :simple}}]})
