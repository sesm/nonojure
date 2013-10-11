(ns nonojure.config
  (:require [clojure.java.io :as io]
            [clojure.tools.reader.edn :as edn]))

(defn- read-config []
  (let [file (io/file "config.clj")]
    (if (.exists file)
      (edn/read-string {:eof {}} (slurp "config.clj"))
      {})))

(def default-config
  {:mongo {:host "localhost"
           :port 27017
           :username nil
           :password nil}
   :web {:port 3000}})

(def config (merge default-config (read-config)))
