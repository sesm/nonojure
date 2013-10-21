(ns nonojure.puzzlebrowser
  (:require
   [dommy.core :as dommy]
   [nonojure.utils :refer [ajax log]]
   [nonojure.storage :as stg]
   [nonojure.navigation :as nav]
   [monet.canvas :as c]
   [clojure.string :refer [join]])
  (:use-macros
   [dommy.macros :only [sel sel1 deftemplate]]))

(declare retrieve-thumbnails)

(def root (atom nil))

(def difficulties {0 "not rated"
                   1 "easy"
                   2 "medium"
                   3 "hard"})

(def cell-size 4)

(defn draw-grid [thumbnail width height board-state]
  (let [ctx (c/get-context (sel1 thumbnail :canvas) :2d)]
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
      (doseq [x (range width)
              y (range height)
                :when (= :filled (get-in board-state [y x]))]
        (c/fill-rect ctx {:x (* x cell-size)
                          :y (* y cell-size)
                          :w cell-size
                          :h cell-size})))
    (c/translate ctx (/ cell-size -2) (/ cell-size -2))

    thumbnail))

(deftemplate nono-thumbnail [nono]
  (let [width (:width nono)
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

(defn- remove-all-classes [root class]
  (doseq [el (sel root (str "." class))]
    (dommy/remove-class! el class)))

(defn apply-progress [progress]
  (log progress)
  (remove-all-classes @root "in-progress")
  (remove-all-classes @root "solved")
  (doseq [[id progress] progress]
    (let [query (str ".thumbnail[data-id='" (name id) "']")]
      (when-let [thumbnail (sel1 @root query)]
        (dommy/add-class! thumbnail (:status progress))
        (when-let [state (or (:auto progress) (:solution progress))]
          (draw-grid thumbnail (count (first state)) (count state) state))))))

(defn- reload-progress []
  (let [ids (map #(dommy/attr % :data-id) (sel @root :.thumbnail))]
   (stg/load-progress window/localStorage ids apply-progress)))

(defn create-thumbnails [nonos]
  (when-let [old (sel1 @root :#thumbnails)]
    (dommy/remove! old))
  (let [cells (for [nono nonos]
                (-> nono nono-thumbnail (draw-grid (:width nono) (:height nono) nil)))]
    (dommy/append! @root [:div#thumbnails cells]))
  (reload-progress))

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
                :value (dommy/attr selected :data-value)
                :sort "size"
                :order "asc"}]
    (retrieve-thumbnails clause)))


(deftemplate filtering []
  [:div.filtering
   [:div.size [:p.type "Size"]
    [:div.item [:a.all.selected "all"]]
    (for [value ["1-10" "11-20" "21-30" "31-40" "41-50"]]
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
  (reload-thumbnails)
  (nav/on-show :browser reload-progress))
