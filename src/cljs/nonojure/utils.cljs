(ns nonojure.utils
  (:require [clojure.string :refer [join]]
            [dommy.core :as dommy])
  (:import goog.debug.Logger
           goog.debug.Console)
  (:require-macros [dommy.macros :refer [sel1]]))

(defn show-ajax-indicator []
  (dommy/add-class! (sel1 :#ajax-indicator) "visible"))

(defn hide-ajax-indicator []
  (dommy/remove-class! (sel1 :#ajax-indicator) "visible"))

(defn- ajax-callback [on-success evt]
  (let [xhr (.-target evt)
        json-data (.getResponseJson xhr)
        clj-data (js->clj json-data :keywordize-keys true)]
    (when on-success
     (on-success clj-data))
    (hide-ajax-indicator)))

(defn ajax [url & [on-success-fn method]]
  (show-ajax-indicator)
  (goog.net.XhrIo.send url (partial ajax-callback on-success-fn)
                       (if (keyword? method) (name method) method)))

(def logger (goog.debug.Logger/getLogger ""))

(def console (goog.debug.Console.))

(.setCapturing console true)

(defn log [& msgs]
  (->> (map #(if (coll? %) (pr-str %) %) msgs)
       (join " ")
       (.info logger)))
