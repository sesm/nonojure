(ns nonojure.config
  (:require [clojure.java.io :as io]
            [clojure.tools.reader.edn :as edn]))

(defn- read-config []
  (let [file (io/file "config.clj")]
    (if (.exists file)
      (edn/read-string {:eof {}} (slurp "config.clj"))
      {})))

(defn- deep-merge [& maps]
  (if (every? map? maps)
    (apply merge-with deep-merge maps)
    (last maps)))

(def default-config
  {:mongo {:host "localhost"
           :port 27017
           :username nil
           :password nil}
   :web {:port 3000
         :persona-audience "http://localhost:3000"
         ; session cookie lives for 1 month
         :cookie-max-age (* 60 60 24 30)}})

(def config (deep-merge default-config (read-config)))
