(ns nonojure.puzzlebrowser
  (:require
   [dommy.core :as dommy]
   [nonojure.utils :refer [ajax log]]
   [monet.canvas :as c]
   [clojure.string :refer [join]])
  (:use-macros
   [dommy.macros :only [sel sel1 deftemplate]]))

(declare retrieve-thumbnails)

(def num-cols 5)

(def root (atom nil))

(def difficulties {0 "not rated"
                   1 "easy"
                   2 "medium"
                   3 "hard"})

(def cell-size 6)

(defn draw-grid [thumbnail nono]
  (let [ctx (c/get-context (sel1 thumbnail :canvas) :2d)
        width (:width nono)
        height (:height nono)]
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
        width (:width nono)
        height (:height nono)
        difficulty (Math/round (:difficulty nono))
        puzzle-id (:id nono)]
    [:div.thumbnail
     {:data-id puzzle-id}
     [:div.canvas-holder-outer
      [:div.canvas-holder-inner
       [:canvas {:width (* (inc width) cell-size)
                 :height (* (inc height) cell-size)}]]]
     [:div.description
      [:p.size.number-text (str width "×" height)]
      [:p.difficulty (difficulties difficulty)]]]))

(defn create-thumbnails [nonos]
  (when-let [old (sel1 @root :#thumbnails)]
    (dommy/remove! old))
  (let [cells (for [nono nonos]
                (-> nono nono-thumbnail (draw-grid nono)))
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
    (log "Sending" url)
    (ajax url create-thumbnails)))

(defn reload-thumbnails []
  (let [selected (sel1 @root :.selected)
        clause {:filter (dommy/attr selected :data-filter)
                :value (dommy/attr selected :data-value)}]
    (retrieve-thumbnails clause)))


(deftemplate filtering []
  [:div.filtering
   [:div.size [:p.type "Size"]
    [:div.item [:a.all.selected "all"]]
    (for [value ["1-10" "11-20" "21-30"]]
      [:div.item
       [:a.number-text {:data-filter "size"
                        :data-value value}
        value]])]
   [:div.difficulty [:p.type "Difficulty"]
    [:div.item [:a.all "all"]]
    (for [value [1 2 3]]
      [:div.item
       [:a {:data-filter "difficulty"
            :data-value (str (- value 0.5) "-" (+ value 0.499))}
        (difficulties value)]] )]])

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

(defn add-thumbnail-listener []
  (dommy/listen! [@root :.thumbnail] :click
    (fn [event]
      (let [thumb (.-selectedTarget event)
            id (dommy/attr thumb :data-id)
            url (str "/api/nonograms/" id)]
        (ajax url #(do (nonojure.puzzleview/show %)
                       (.scrollTo js/window 0 0)))))))

(defn ^:export init [el]
  (reset! root el)
  (dommy/append! @root (add-filtering-listener (filtering)))
  (add-thumbnail-listener)
  (reload-thumbnails))
