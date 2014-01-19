(ns nonojure.utils
  (:require [clojure.string :refer [join]]
            [dommy.core :as dommy]
            [monet.canvas :as c])
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
                         (clj->js {:Content-Type "application/json"}))))

(def logger (goog.debug.Logger/getLogger ""))

(def console (goog.debug.Console.))

(.setCapturing console true)

(defn log [& msgs]
  (->> (map #(if (coll? %) (pr-str %) %) msgs)
       (join " ")
       (.info logger)))

(defn draw-grid [canvas width height board-state cell-size]
  (let [ctx (c/get-context canvas :2d)]
    (c/clear-rect ctx {:x 0
                       :y 0
                       :w (* width cell-size)
                       :h (* height cell-size)} )
    (c/translate ctx (/ cell-size 2) (/ cell-size 2))
    (c/stroke-width ctx 0.3)
    (doseq [x (range 0 width)
            y (range 0 height)]
      (c/stroke-rect ctx {:x (* x cell-size)
                          :y (* y cell-size)
                          :w cell-size
                          :h cell-size}))
    (c/stroke-width ctx 0.5)
    (c/stroke-rect ctx {:x 0 :y 0
                        :w (* width cell-size)
                        :h (* height cell-size)})
    (doseq [x (range 0 width 5)
            y (range 0 height 5)]
      (c/stroke-rect ctx {:x (* x cell-size)
                          :y (* y cell-size)
                          :w (* 5 cell-size)
                          :h (* 5 cell-size)}))
    (when board-state
      (c/stroke-width ctx 0.1)
      (doseq [x (range width)
              y (range height)
                :when (= :filled (get-in board-state [y x]))]
        (c/fill-rect ctx {:x (* x cell-size)
                          :y (* y cell-size)
                          :w cell-size
                          :h cell-size})))
    (c/translate ctx (/ cell-size -2) (/ cell-size -2))))
