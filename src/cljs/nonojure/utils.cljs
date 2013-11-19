(ns nonojure.utils
  (:require [clojure.string :refer [join]]
            [dommy.core :as dommy])
  (:import goog.debug.Logger
           goog.debug.Console)
  (:require-macros [dommy.macros :refer [sel1]]))

(defn range-inc [a b]
  (range (min a b) (inc (max a b))))

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

(defn ajax [url & [on-success-fn method data]]
  (show-ajax-indicator)
  (let [method (if (keyword? method) (name method) method)
        data (if-not (nil? data) (.stringify js/JSON (clj->js data)) nil)]
    (goog.net.XhrIo.send url (partial ajax-callback on-success-fn)
                         method data
                         (clj->js {:content-type "application/json"}))))

(def logger (goog.debug.Logger/getLogger ""))

(def console (goog.debug.Console.))

(.setCapturing console true)

(defn log [& msgs]
  (->> (map #(if (coll? %) (pr-str %) %) msgs)
       (join " ")
       (.info logger)))
