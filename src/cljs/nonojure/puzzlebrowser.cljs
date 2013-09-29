(ns nonojure.puzzlebrowser
  (:require
   [dommy.utils :as utils]
   [dommy.core :as dommy]
   [jayq.core :refer [ajax]])
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

(deftemplate nono-thumbnail [nono]
  (let [scale 20
        width (get nono "width")
        height (get nono "height")
        rating (get nono "rating")
        puzzle-id (get nono "id")]
    [:div {:align "center"}
     [:a {:href (str "/api/nonograms/" puzzle-id)}
      [:img {:src "/static/img/grid.svg"
             :width (* scale width)
             :height (* scale height)}]
      [:p (str width "x" height " - " rating)]]]))

(defn create-thumbnails [nonos]
  (let [cells (for [nono nonos] [:td (nono-thumbnail (js->clj nono))])
        padded-cells (concat cells (repeat (dec num-cols) [:td ""]))
        rows (partition num-cols padded-cells)
        contents (for [row rows] [:tr row])
        table [:table#table.puzzle-browser{:id "puzzle-browser" :border 1} contents]]
    (dommy/append! (sel1 :body) table)))

(defn retrieve-thumbnails [{:keys [min-size max-size order]
                            :or {min-size 1 max-size 100 order :asc}}]
  (let [url (str "/api/nonograms?filter=size&value=" min-size "-" max-size "&sort=rating&order=" order)]
    (ajax url {:success create-thumbnails
               :error #(js/alert "Error")})))

(defn ^:export init []
  (retrieve-thumbnails {:min-size 1 :max-size 100 :order :asc}))
