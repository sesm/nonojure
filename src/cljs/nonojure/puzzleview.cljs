(ns nonojure.puzzleview
  (:require
   [nonojure.navigation :refer [show-view set-url-for-view]]
   [nonojure.dialog :as dialog]
   [nonojure.utils :refer [ajax log range-inc]]
   [nonojure.storage :as stg]
   [nonojure.pubsub :refer [subscribe publish]]
   [nonojure.puzzle-widget :as pw]
   [dommy.core :as dommy :refer [listen! attr]]
   [cljs.core.async :refer [put! <! >! chan close!]])
  (:use-macros
   [dommy.macros :only [sel sel1]]
   [cljs.core.async.macros :only [go-loop]]))


(defn init-board [width height]
  (->> (repeat width :empty)
       vec
       (repeat height)
       vec))

(defn change-cell-in-board [board x y new-state]
  (assoc-in board [y x] new-state))

(defn update-region-of-board [board [from-x from-y] [to-x to-y] new-state]
  (let [cells-to-change (for [x (range-inc from-x to-x)
                              y (range-inc from-y to-y)]
                          [y x])
        new-board (reduce #(assoc-in %1 %2 new-state) board cells-to-change)]
    new-board))

(defn valid-rows-cols
  "Returns indices of valid rows and cols.
Also adds :valid? bool value to map indicating whether everyting is correct."
  [{:keys [board puzzle]}]
  (let [row-to-numbers (fn [row]
                         (->> (partition-by identity row)
                              (filter #(= :filled (first %)))
                              (map count)))
        left (map row-to-numbers board)
        top (->> (apply map vector board)
                 (map row-to-numbers))
        filter-equals (fn [a b]
                        (for [ind (range (count a))
                              :when (= (nth a ind) (nth b ind))]
                          ind))
        rows (filter-equals left (:left puzzle))
        cols (filter-equals top (:top puzzle))]
    {:rows rows
     :cols cols
     :valid? (and (= (count rows) (count left))
                  (= (count cols) (count top)))}))

(defn rate-puzzle [id difficulty]
  (let [url (str "/api/rate/" id "?difficulty=" difficulty)]
    (ajax url nil :POST)))

(defn show-solved-div [id]
  (let [div [:div#solved
             [:p.solved-caption "you solved it!"]
             [:p.invitation "it was"]
             [:div.choices
              [:p {:data-value 1} "easy"]
              [:p {:data-value 2} "medium"]
              [:p {:data-value 3} "hard"]]]
        dlg (dialog/create div)]
    (listen! [(sel1 dlg :#solved) :.choices :p] :click
      (fn [evt]
        (let [el (.-target evt)]
          (rate-puzzle id (attr el :data-value))
          (dialog/close dlg))))))

(defn check-solution [state]
  (let [id (get-in state [:puzzle :id])
        {:keys [cols rows valid?]} (valid-rows-cols state)]
    (pw/highlight-solved-rows-cols! (:widget state) rows cols)
    (if (and (not (:solved? state))
             valid?)
      (do (show-solved-div id)
          (stg/mark-puzzle-solved (:storage state) id (:board state) nil)
          (assoc state :solved? true))
      state)))

(defn new-cell-style-after-click [widget x y button]
  (case [(pw/cell-state widget x y) button]
    [:filled :left] :empty
    [:filled :right] :crossed
    [:crossed :left] :filled
    [:crossed :right] :empty
    [:empty :left] :filled
    [:empty :right] :crossed))

(defn save-state [{:keys [puzzle storage board] :as state}]
  (stg/save-puzzle-progress storage (:id puzzle) board nil)
  state)

(defn handle-mouse-down-on-cell [state [x y button]]
  (let [widget (:widget state)
        style (new-cell-style-after-click widget x y button)]
    (pw/change-region! widget [x y] [x y] (constantly style))
    (assoc state
      :fill-style style
      :base-cell [x y]
      :last-cell [x y])))

(defn handle-mouse-enter-on-cell [{:keys [fill-style base-cell last-cell drag-type widget board] :as state}
                                  [x y]]
  (pw/highlight-current-row-col! widget y x)
  (if-not fill-style
    state
    (if (= :rect drag-type)
      (do
        (pw/change-region! widget base-cell last-cell (fn [x y] (get-in state [:board y x])))
        (pw/change-region! widget base-cell [x y] (constantly fill-style))
        (assoc state :last-cell [x y]))
      (do
        (pw/change-region! widget [x y] [x y] (constantly fill-style))
        (assoc state
          :last-cell [x y]
          :board (change-cell-in-board board x y fill-style))))))

(defn stop-dragging [{:keys [fill-style base-cell last-cell board drag-type puzzle storage] :as state}]
  (if base-cell
    (let [new-board (if (and fill-style (= :rect drag-type))
                    (update-region-of-board board base-cell last-cell fill-style)
                    board)
        new-state (-> state
                      (assoc :board new-board)
                      (update-in [:history] #(cons board %))
                      (dissoc :fill-style :last-cell :base-cell))]
      (save-state new-state)
      new-state)
    state))

(defn undo-step [{:keys [history widget] :as state}]
  (if-let [prev (first history)]
    (do (pw/change-whole-board! widget prev)
        (-> state
            (assoc :board prev
                   :history (rest history))
            save-state
            check-solution))
    state))

(defn cancel-dragging [{:keys [fill-style base-cell last-cell board widget] :as state}]
  (when fill-style
    (pw/change-region! widget base-cell last-cell (fn [x y] (get-in board [y x]))))
  (dissoc state :fill-style :last-cell :base-cell))

(defn handle-number-click [state coord]
  (pw/toggle-number! (:widget state) coord))

(defn apply-progress [state progress]
  (let [progress (progress (get-in state [:puzzle :id]))
        state (if (= (keyword (:status progress)) :solved) (assoc state :solved? true) state)]
    (if-let [saved-board (or (:auto progress) (:solution progress))]
      (do (pw/change-whole-board! (:widget state) saved-board)
          (assoc state :board saved-board))
      state)))

(defn create-state [storage root puzzle solved?]
  {:fill-style nil
   :base-cell nil
   :last-cell nil
   :drag-type :rect
   :puzzle puzzle
   :widget root
   :storage storage
   :history []
   :solved? solved?
   :board (init-board (count (:top puzzle)) (count (:left puzzle)))})

(defn clear-state [{:keys [storage widget puzzle board history solved?]}]
  (-> (create-state storage widget puzzle solved?)
      (assoc :history (cons board history))))

(defn show [view nono event-chan]
  (log "Load puzzle")
  (pw/load-puzzle! view nono)
  (let [storage window/localStorage]
    (put! event-chan [:new-state (create-state storage view nono false)])
    (stg/load-progress storage [(:id nono)] #(put! event-chan [:progress-loaded %]))))

(defn load-puzzle [id widget event-chan]
  (pw/unload-puzzle! widget)
  (let [url (str "/api/nonograms/" id)]
    (ajax url #(do (show widget % event-chan)
                   (.scrollTo js/window 0 0)))))

(defn handle-puzzle-events [root event-chan]
  (go-loop
   [[event-type data] (<! event-chan)
    state {:widget root}]
   (when-not (nil? event-type) ; nil - channel is closed
     (let [new-state (case event-type
                       :mousedown-cell (handle-mouse-down-on-cell state data)
                       :mouseenter-cell (handle-mouse-enter-on-cell state data)
                       :mouseup-cell (check-solution (stop-dragging state))
                       :mouseleave-board (cancel-dragging state)
                       :progress-loaded (check-solution (apply-progress state data))
                       :number-click (do (handle-number-click state data) state)
                       :clear (let [cleared-state (clear-state state)]
                                (pw/clear-puzzle! (:widget state))
                                (save-state cleared-state))
                       :mouseleave-drawing-board (do (pw/highlight-current-row-col! (:widget state) -1 -1)
                                                     state)
                       :new-state (do (pw/subscribe (:widget data) event-chan)
                                      data)
                       :puzzle-requested (do (when-not (= (get-in state [:puzzle :id]) data)
                                               (load-puzzle data (:widget state) event-chan))
                                             (show-view :puzzle)
                                             state)
                       :undo (undo-step state)
                       (do (log "Unknown event:" event-type) state))]
       (recur (<! event-chan) new-state)))))

(defn add-no-puzzle-message [view]
  (pw/set-no-puzzle-message! view)
  (show-view :puzzle))

(defn url-changed [url event-chan view]
  (when (re-matches #"nonogram(/[a-zA-Z0-9]*)?" (:path url))
    (set-url-for-view :puzzle (:path url))
    (if-let [id (re-find #"[a-zA-Z0-9]+" (subs (:path url) (count "nonogram")))]
      (put! event-chan [:puzzle-requested id])
      (add-no-puzzle-message view))))

(defn start-async-loop [view]
  (let [event-chan (chan 5)]
    (handle-puzzle-events view event-chan)
    (subscribe :url-changed #(url-changed % event-chan view))))

(defn ^:export init [view]
  (pw/init! view)
  (start-async-loop view))
