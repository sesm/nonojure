(ns nonojure.puzzlebrowser
  (:require
   [dommy.core :as dommy]
   [nonojure.utils :refer [ajax log]]
   [nonojure.storage :as stg]
   [nonojure.navigation :as nav]
   [nonojure.pubsub :refer [subscribe publish]]
   [nonojure.url :refer [go-overwrite-history go]]
   [monet.canvas :as c]
   [clojure.string :refer [join]]
   [clojure.set :refer [map-invert]])
  (:use-macros
   [dommy.macros :only [sel sel1 deftemplate]]))

(declare retrieve-thumbnails)

(def root (atom nil))

(def difc-int->str {0 "not rated"
                    1 "easy"
                    2 "medium"
                    3 "hard"})

(def difc-str->int (map-invert difc-int->str))

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
      (c/stroke-width ctx 0.1)
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
      [:p.difficulty (difc-int->str difficulty)]]]))

(defn- remove-all-classes [root class]
  (doseq [el (sel root (str "." class))]
    (dommy/remove-class! el class)))

(defn apply-progress [progress]
  (remove-all-classes @root "in-progress")
  (remove-all-classes @root "solved")
  (doseq [[id progress] progress]
    (let [query (str ".thumbnail[data-id='" (name id) "']")]
      (when-let [thumbnail (sel1 @root query)]
        (dommy/add-class! thumbnail (:status progress))
        (when-let [state (or (:current-state progress) (:solution progress))]
          (draw-grid thumbnail (count (first state)) (count state) state))))))

(defn- reload-progress []
  (let [ids (map #(dommy/attr % :data-id) (sel @root :.thumbnail))]
   (stg/load-progress @stg/storage ids apply-progress)))

(defn create-thumbnails [nonos]
  (when-let [old (sel1 @root :#thumbnails)]
    (dommy/remove! old))
  (let [cells (for [nono nonos]
                (-> nono nono-thumbnail (draw-grid (:width nono) (:height nono) nil)))]
    (dommy/append! @root [:div#thumbnails cells]))
  (reload-progress))


(defn build-query-str [{:keys [filter value sort order]} for-api?]
  (let [value (if (and (= filter "difficulty") for-api? (not= value "all"))
                (let [num-val (difc-str->int value)]
                  (str (- num-val 0.5) "-" (+ num-val 0.499)))
                value)
        filter-clauses (if (or (not= value "all")
                               (not for-api?))
                         [["filter" filter]
                          ["value" value]]
                         [])
        sort-clauses (if sort [["sort" sort]
                              ["order" order]]
                        [])]
    (->> (concat filter-clauses sort-clauses)
         (map #(join "=" %))
         (join "&"))))

(defn retrieve-thumbnails [params]
  (let [query (build-query-str params true)
        url (str "/api/nonograms" (if (empty? query) "" "?") query)]
    (ajax url create-thumbnails)))

(defn get-clause [selected]
  {:filter (if selected
             (dommy/attr selected :data-filter)
             "size")
   :value (if selected
            (dommy/attr selected :data-value)
            "all")
   :sort "size"
   :order "asc"})

(defn get-browser-url [selected]
  (str "browse?" (build-query-str (get-clause selected) false)))

(defn selected-button [root]
  (sel1 root [:.filtering :.selected]))

(defn set-criteria-button-selected
  "Sets criteria (text button) on a screen selected according to query.
E.g. {:filter \"size\" :value \"1-10\"} will set button \"1-10\" selected.
Returns true if button was switched to selected mode and false button already selected"
  [root query]
  (let [{:keys [filter value]} query
        button (sel1 root [(str "." filter) (str "[data-value='" value "']")])]
    (if-not (dommy/has-class? button "selected")
      (do (when-let [selected (selected-button root)]
            (dommy/remove-class! selected "selected"))
          (dommy/add-class! button "selected")
          true)
      false)))


(deftemplate filtering []
  [:div.filtering
   [:div.size [:p.type "Size"]
    [:div.item
     [:a {:data-filter "size"
          :data-value "all"}
      "all"]]
    (for [value ["1-10" "11-20" "21-30" "31-40" "41-50"]]
      [:div.item
       [:a.number-text {:data-filter "size"
                        :data-value value}
        value]])]
   [:div.difficulty [:p.type "Difficulty"]
    (for [value ["all" "easy" "medium" "hard"]]
      [:div.item
       [:a {:data-filter "difficulty"
            :data-value value}
        value]])]])

(defn add-filtering-listener [filter-div]
  (dommy/listen! [filter-div :a] :click
    (fn [event]
      (let [a (.-selectedTarget event)]
        (go-overwrite-history (get-browser-url a)))))
  filter-div)

(defn add-thumbnail-listener [root]
  (dommy/listen! [root :.thumbnail] :click
    (fn [event]
      (let [thumb (.-selectedTarget event)
            id (dommy/attr thumb :data-id)]
        (go (str "nonogram/" id))))))

(defn url-changed [root url]
  (when (or(= (:path url) "browse")
           (= (:path url) ""))
    (if (empty? (:query url))
      (go-overwrite-history (get-browser-url (selected-button root)))
      (if (set-criteria-button-selected root (:query url))
        (do (retrieve-thumbnails (:query url))
            (nav/set-url-for-view :browser (get-browser-url (selected-button root))))
        (reload-progress)))
    (nav/show-view :browser)))

(defn reload-all []
  (-> @root selected-button get-clause retrieve-thumbnails))

(defn ^:export init [el]
  (reset! root el)
  (dommy/append! el (add-filtering-listener (filtering)))
  (add-thumbnail-listener @root)
  (subscribe :show-browser reload-progress)
  (subscribe :url-changed #(url-changed @root %))
  (subscribe :storage-changed reload-all))
