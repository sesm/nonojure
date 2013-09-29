(ns nonojure.puzzlebrowser
  (:require
   [dommy.utils :as utils]
   [dommy.core :as dommy]
   [jayq.core :refer [ajax]]
   [jayq.util :refer [log]]
   [monet.canvas :as c]
   [clojure.string :refer [join]])
  (:use-macros
   [dommy.macros :only [node sel sel1 deftemplate]]))

(def num-cols 5)

(def root (atom nil))

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

(defn create-thumbnails [nonos]
  (when-let [old (sel1 @root :#thumbnails)]
    (dommy/remove! old))
  (let [cells (for [nono nonos]
                (let [nono (js->clj nono)]
                  (-> nono nono-thumbnail (draw-grid nono))))
        padded-cells (concat cells (repeat (dec num-cols) [:td ""]))
        rows (partition num-cols padded-cells)
        contents (for [row rows] [:tr row])
        table [:table#table.puzzle-browser{:id "puzzle-browser" :border 1} contents]]
    (dommy/append! @root [:div#thumbnails cells])))

(defn retrieve-thumbnails [{:keys [filter value sort order]}]
  (let [filter-clauses (if filter [["filter" filter]
                                   ["value" value]]
                           [])
        sort-clauses (if sort [["sort" sort]
                              ["order" order]]
                        [])
        clauses-str (->> (concat filter-clauses sort-clauses)
                         (map #(join "=" %))
                         (join "&"))
        url (str "/api/nonograms" (if (empty? clauses-str) "" "?") clauses-str)]
    (log (str "Sending " url))
    (ajax url {:success #(create-thumbnails %)
               :error #(js/alert "Error")})))

(defn reload-thumbnails []
  (let [selected (sel1 @root :.selected)
        clause {:filter (dommy/attr selected :data-filter)
                :value (dommy/attr selected :data-value)}]
    (retrieve-thumbnails clause)))

(deftemplate filtering []
  [:div.filtering
   [:div.size [:p.type "Size"]
    [:a.all.selected {:href "#"} "all"]
    (for [value ["1-10" "11-20" "21-30"]]
      [:a {:href "#"
           :data-filter "size"
           :data-value value}
       value])]
   [:div.rating [:p.type "Rating"]
    [:a.all {:href "#"} "all"]
    (for [rating [1 2 3 4 5]]
      [:a {:href "#"
           :data-filter "rating"
           :data-value (str (- rating 0.5) "-" (+ rating 0.499))}
       rating])]])

(defn add-filtering-listener [filter-div]
  (dommy/listen! [filter-div :a] :click
    (fn [event]
      (let [a (.-selectedTarget event)]
        (when-not (dommy/has-class? a "selected")
          (-> (sel1 filter-div :.selected)
              (dommy/remove-class! "selected"))
          (dommy/add-class! a "selected")
          (reload-thumbnails )))))
  filter-div)

(defn ^:export init []
  (reset! root (sel1 :#browser))
  (dommy/append! @root (add-filtering-listener (filtering)))
  (reload-thumbnails))
