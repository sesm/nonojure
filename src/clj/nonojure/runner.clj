(ns nonojure.runner
  (:require [nonojure.core :as core])
  (:gen-class))

(defn -main
  [& args]
  (core/start))
