(ns nonojure.utils
  (:require [clojure.string :refer [join]])
  (:import goog.debug.Logger
           goog.debug.Console))

(defn- ajax-callback [on-success evt]
  (let [xhr (.-target evt)
        json-data (.getResponseJson xhr)
        clj-data (js->clj json-data)]
    (when on-success
     (on-success clj-data))))

(defn ajax [url & [on-success-fn method]]
  (goog.net.XhrIo.send url (partial ajax-callback on-success-fn)
                       (if (keyword? method) (name method) method)))

(def logger (goog.debug.Logger/getLogger ""))

(def console (goog.debug.Console.))

(.setCapturing console true)

(defn log [& msgs]
  (->> (map #(if (coll? %) (pr-str %) %) msgs)
       (join " ")
       (.info logger)))
