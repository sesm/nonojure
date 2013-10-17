(ns nonojure.utils
  (:require [clojure.string :refer [join]]))

(defn- ajax-callback [on-success evt]
  (let [xhr (.-target evt)
        json (.getResponseJson xhr)]
    (when on-success
     (on-success json))))

(defn ajax [url & [on-success-fn method]]
  (goog.net.XhrIo.send url (partial ajax-callback on-success-fn)
                       (if (keyword? method) (name method) method)))

(defn log [& msgs]
  (letfn [(->clj [obj]
            (if (coll? obj)
              (clj->js obj)
              (str obj)))]
    (when js/console
      (let [args (apply array (map ->clj msgs))]
       (.apply (.-log js/console) js/console args)))))

