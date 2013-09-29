(ns nonojure.puzzlebrowser
  (:require
   [dommy.utils :as utils]
   [dommy.core :as dommy]
   [jayq.core :refer [ajax]]
   [monet.canvas :as c])
  (:use-macros
   [dommy.macros :only [node sel sel1 deftemplate]]))

(def num-cols 5)

(defn ^:export showalert []
  (let [min-size (js/parseInt (.-value (sel1 :#minSize)))
        max-size (js/parseInt (.-value (sel1 :#maxSize)))
        order (->> (sel ".order")
                   (filter #(.-checked %))
                   first
                   .-value)]
    (dommy/remove! (sel1 :table))
    (retrieve-thumbnails {:min-size min-size :max-size max-size :order order})))

(def cell-size 6)

(defn draw-grid [thumbnail nono]
  (let [ctx (c/get-context (sel1 thumbnail :canvas) :2d)
        width (nono "width")
        height (nono "height")]
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
    thumbnail))

(deftemplate nono-thumbnail [nono]
  (let [scale 20
        width (get nono "width")
        height (get nono "height")
        rating (get nono "rating")
        puzzle-id (get nono "id")]
    [:div.thumbnail
     [:div.canvas-holder-outer
      [:div.canvas-holder-inner
       [:canvas {:width (* (inc width) cell-size)
                 :height (* (inc height) cell-size)}]]]

     #_[:img {:src "/static/img/grid.svg"
              :width (* scale width)
              :height (* scale height)}]

     [:div.description
      [:p.size (str width "×" height)]
      [:p.rating (if (zero? rating)
                   "not rated"
                   (str rating " (" (nono "times-rated") ")"))]]]))

(defn create-thumbnails [holder nonos]
  (let [cells (for [nono nonos]
                (let [nono (js->clj nono)]
                  (-> nono nono-thumbnail (draw-grid nono))))
        padded-cells (concat cells (repeat (dec num-cols) [:td ""]))
        rows (partition num-cols padded-cells)
        contents (for [row rows] [:tr row])
        table [:table#table.puzzle-browser{:id "puzzle-browser" :border 1} contents]]
    (dommy/append! holder cells)))

(defn retrieve-thumbnails [{:keys [min-size max-size order]
                            :or {min-size 1 max-size 100 order :asc}}
                           holder]
  (let [url (str "/api/nonograms?filter=size&value=" min-size "-" max-size "&sort=rating&order=" order)]
    (ajax url {:success #(create-thumbnails holder %)
               :error #(js/alert "Error")})))

(defn ^:export init []
  (retrieve-thumbnails {:min-size 1 :max-size 100 :order :asc}
                       (sel1 :#browser)))
