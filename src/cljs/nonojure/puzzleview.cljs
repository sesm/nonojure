(ns nonojure.puzzleview
  (:require
   [nonojure.navigation :refer [show-view]]
   [nonojure.dialog :as dialog]
   [nonojure.utils :refer [ajax log]]
   [nonojure.storage :as stg]
   [dommy.utils :as utils]
   [dommy.core :as dommy :refer [listen! attr append!]]
   [cljs.core.async :refer [put! <! >! chan]])
  (:use-macros
   [dommy.macros :only [node sel sel1]]
   [cljs.core.async.macros :only [go-loop]]))

(defn range-inc [a b]
  (range (min a b) (inc (max a b))))

(defn attr-int [cell attribute]
  (js/parseInt (attr cell attribute) 10))

;;; Board functions

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

(defn pad [nums beg end value]
  (vec (concat (repeat beg value)
               nums
               (repeat end value))))

(defn add-class-templ [el class]
  (update-in el [1 :class] str class))

(defn update-last [seq fn & args]
  (concat (butlast seq)
          [(apply fn (last seq) args)]))

(defn add-thick-class
  "Adds thick-left and thick-right classes to every 5th element
  Basically it add thick-left to 0, 5, 10 element and thick-right to last one."
  [tds]
  (let [tds (map-indexed
             (fn [ind value]
               (if (zero? (rem ind 5))
                 (add-class-templ value " thick-left")
                 value))
             tds)]
    (update-last tds add-class-templ " thick-right")))

(defn create-row [nums offset row-num row-length]
  "Takes a vector of row numbers, offset (assumed to be longer then row numbers vector),
  row number and row length. Returns template for nodes construction"
  (let [num-tds (map-indexed
                 (fn [ind n]
                   [:td {:class (str "num r" row-num)
                         :data-coord (str "l" ind "x" row-num)} n])
                 nums)
        pad-length (- offset (count nums))
        empty-cell [:td {:class (str "nothing r" row-num)}]
        num-tds-left (pad num-tds pad-length 0 empty-cell)
        num-tds-right(pad num-tds 0 pad-length empty-cell)
        cells (for [c (range row-length)]
                [:td {:class (str "cell c" c " r" row-num)
                      :data-x c
                      :data-y row-num}])]
    (-> [(keyword (str "tr.row.row" row-num)) {:class ""}]
      (into num-tds-left)
      (into (add-thick-class cells))
      (into num-tds-right))))

(defn create-header [nums offset]
  "Takes a vector of column numbers and offset. Returns template for table header."
  (let [col-num (count nums)
        longest (apply max (map count nums))
        padded  (map #(pad % (- longest (count %)) 0 nil) nums)]
    (into []
      (for [row (range longest)]
        (let [nums-col (map #(nth % row) padded)
              tds  (map-indexed
                    #(if %2 [:td {:class (str "num c" %1)
                                  :data-coord (str "t" %1 "x"
                                                   (->> (nth nums %1) count (- longest) (- row)))}
                             %2]
                         [:td {:class (str "nothing c" %1)}])
                        nums-col)]
        (into [:tr {:class (cond (zero? row) "first"
                                 (= row (dec longest)) "last"
                                 :default "")}]
              (-> (concat [[:td.hide.has-right]]
                          (add-thick-class tds)
                          [[:td.hide.has-left]])
                  (pad (dec offset) (dec offset) [:td.hide]))))))))

(defn create-bottom [nums offset]
  "Takes a vector of column numbers and offset. Returns template for table bottom."
  (let [col-num (count nums)
        longest (apply max (map count nums))
        padded  (map #(pad % 0 (- longest (count %)) nil) nums)]
    (into []
      (for [row (range longest)]
        (let [nums-col (map #(nth % row) padded)
              tds  (map-indexed
                    #(if %2
                       [:td {:class (str "num c" %1)
                             :data-coord (str "t" %1 "x" row)} %2]
                       [:td {:class (str "nothing c" %1)}])
                     nums-col)]
          (into [:tr {:class (cond (zero? row) "first footer"
                               (= row (dec longest)) "last footer"
                               :default "footer")}]
            (-> (concat [[:td.hide.has-right]]
                  (add-thick-class tds)
                  [[:td.hide.has-left]])
              (pad (dec offset) (dec offset) [:td.hide]))))))))

(defn create-template [data]
  "Create a template for puzzle based on description"
  (let [ top-nums (get data :top)
         width (count top-nums)
         left-nums (get data :left)
         height (count left-nums)
         offset (apply max (map count left-nums))
         header (create-header top-nums offset)
         bottom (create-bottom top-nums offset)
         rows (for [r (range height)
                    :let [row (create-row (nth left-nums r) offset r width)]]
                (if (zero? (rem r 5))
                  (add-class-templ row " thick-top")
                  row))]
    (-> [:table#puzzle-table.number-text.medium-cells]
      (into header)
      (into (update-last rows add-class-templ " thick-bottom"))
      (into bottom))))

(defn extract-solution []
  "Extracts solution from the page"
  (let [rows (sel ".row")
        row-num (count rows)
        solution (for [r (range row-num)
                       :let [cells (sel (str ".r" r))]]
                   (mapv #(if (dommy/has-class? % "crossed") 1 0) cells))]
    (vec solution)))

(defn extract-numbers [row]
  "Given a sequence containing 0s and 1s returns numbers of consecutive ones,
  i.e. given [1 0 0 1 1] will return [1 2]"
  (->> row
    (partition-by identity)
    (filter #(= 1 (first %)))
    (map count)
    vec))

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
             [:p.solved-caption "solved!"]
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

(defn highlight-solved-rows-cols [rows cols]
  (doseq [el (sel :.solved.num)]
    (dommy/remove-class! el "solved"))
  (let [rows (map #(str ".num.r" %) rows)
        cols (map #(str ".num.c" %) cols)]
   (doseq [cls (concat rows cols)
           el (sel cls)]
     (dommy/add-class! el "solved"))))

(defn check-solution [state]
  (let [id (get-in state [:puzzle :id])
        {:keys [cols rows valid?]} (valid-rows-cols state)]
    (highlight-solved-rows-cols rows cols)
    (if (and (not (:solved? state))
             valid?)
      (do (show-solved-div id)
          (stg/mark-puzzle-solved (:storage state) id (:board state) nil)
          (assoc state :solved? true))
      state)))

(defn clear-puzzle []
  (doseq [class ["num-clicked" "filled" "crossed"]
          el (sel (str "." class))]
    (dommy/remove-class! el class))
  (highlight-solved-rows-cols [] [])
  (dommy/remove-class! (sel1 :#puzzle-table) "solved"))

(defn change-cell-style! [cell style]
  (case style
    :filled (do (dommy/add-class! cell "filled")
                (dommy/remove-class! cell "crossed"))
    :crossed (do (dommy/add-class! cell "crossed")
                 (dommy/remove-class! cell "filled"))
    :empty (do (dommy/remove-class! cell "filled")
               (dommy/remove-class! cell "crossed"))))

(defn update-cells-region-style! [[from-x from-y] [to-x to-y] style-fn]
  (doseq [x (range-inc from-x to-x)
          y (range-inc from-y to-y)
          :let [cell (sel1 (str ".r" y ".c" x))]]
        (change-cell-style! cell (style-fn x y))))


(defn highlight-row-col [row col]
  (doseq [el (sel [:#puzzle :.highlighted])]
    (dommy/remove-class! el "highlighted"))
  (doseq [el (sel [:#puzzle (str ".num.c" col)])]
    (dommy/add-class! el "highlighted"))
  (doseq [el (sel [:#puzzle (str ".num.r" row)])]
    (dommy/add-class! el "highlighted")))

(defn cell-style [cell]
  (cond (dommy/has-class? cell "filled") :filled
        (dommy/has-class? cell "crossed") :crossed
        :default :empty))

(defn new-cell-style-after-click [cell button]
  (case [(cell-style cell) button]
    [:filled :left] :empty
    [:filled :right] :crossed
    [:crossed :left] :filled
    [:crossed :right] :empty
    [:empty :left] :filled
    [:empty :right] :crossed))

(defn button [evt]
  (case (.-button evt)
    0 :left
    1 :middle
    2 :right
    :unknown))

(defn save-state [{:keys [puzzle storage board]}]
  (stg/save-puzzle-progress storage (:id puzzle) board nil))

(defn handle-mouse-down-on-cell [evt state]
  (let [cell (.-target evt)
        style (new-cell-style-after-click cell (button evt))
        x (attr-int cell :data-x)
        y (attr-int cell :data-y)]
    (change-cell-style! cell style)
    (assoc state
      :fill-style style
      :base-cell [x y]
      :last-cell [x y]
      :board (change-cell-in-board (:board state) x y style))))

(defn handle-mouse-enter-on-cell [evt state]
  (let [cell (.-target evt)
        x (attr-int cell :data-x)
        y (attr-int cell :data-y)]
    (highlight-row-col y x)
    (if-not (:fill-style state)
      state
      (let [{:keys [fill-style base-cell last-cell drag-type]} state]
        (if (= :rect drag-type)
          (do
            (update-cells-region-style! base-cell last-cell (fn [x y] (get-in state [:board y x])))
            (update-cells-region-style! base-cell [x y] (constantly fill-style))
            (assoc state :last-cell [x y]))
          (do
            (change-cell-style! cell fill-style)
            (assoc state
              :last-cell [x y]
              :board (change-cell-in-board (:board state) x y fill-style))))))))

(defn stop-dragging [{:keys [fill-style base-cell last-cell board drag-type puzzle storage] :as state}]
  (let [new-board (if (and fill-style (= :rect drag-type))
                    (update-region-of-board board base-cell last-cell fill-style)
                    board)
        new-state (-> state
                      (assoc :board new-board)
                      (dissoc :fill-style :last-cell :base-cell))]
    (save-state new-state)
    new-state))

(defn cancel-dragging [{:keys [fill-style base-cell last-cell board] :as state}]
  (when fill-style
    (update-cells-region-style! base-cell last-cell (fn [x y] (get-in board [y x]))))
  (dissoc state :fill-style :last-cell :base-cell))

(defn disable-context-menu [evt]
  (.preventDefault evt)
  false)

(defn handle-number-click [evt]
  (let [cell (.-target evt)
        coord (attr cell :data-coord)
        query (str "td[data-coord='" coord "']")]
    (doseq [el (sel query)]
      (dommy/toggle-class! el "num-clicked"))))

(defn apply-progress [progress state]
  (let [state (if (:solved? progress) (assoc state :solved? true) state)]
   (if-let [saved-board (:auto progress)]
     (let [width (count (first saved-board))
           height (count saved-board)]
       (update-cells-region-style! [0 0] [(dec width) (dec height)] (fn [x y] (get-in saved-board [y x])))
       (assoc state :board saved-board))
     state)))

(defn add-handlers [event-chan]
  (let [add-event (fn [event-type] #(put! event-chan [% event-type]))]
    (listen! [(sel1 :#puzzle-table) :.cell]
             :mousedown (add-event :mousedown-cell)
             :contextmenu disable-context-menu
             :mouseenter (add-event :mouseenter-cell))
    (listen! (sel1 :#puzzle-table)
             :mouseup (add-event :mouseup-cell)
             :mouseleave (add-event :mouseleave-board))
    (listen! [(sel1 :#puzzle-table) "td:not(.cell)"]
             :mouseenter (add-event :mouseleave-drawing-board))
    (listen! [(sel1 :#puzzle-table) :.num]
             :mousedown (add-event :number-click)
             :contextmenu disable-context-menu)
    (listen! (sel1 :#button-clear) :click (add-event :clear))))

(defn handle-cell-events [event-chan initial-state]
  (go-loop
   [[evt event-type] (<! event-chan)
    state initial-state]
   (let [new-state (case event-type
                     :mousedown-cell (handle-mouse-down-on-cell evt state)
                     :mouseenter-cell (handle-mouse-enter-on-cell evt state)
                     :mouseup-cell (check-solution (stop-dragging state))
                     :mouseleave-board (cancel-dragging state)
                     :progress-loaded (check-solution (apply-progress evt state))
                     :number-click (do (handle-number-click evt) state)
                     :clear (do (clear-puzzle) (save-state initial-state))
                     :mouseleave-drawing-board (do (highlight-row-col -1 -1) state)
                     (do (log "Unknown event:" event-type) state))]
     (recur (<! event-chan) new-state))))

(defn show [nono]
  (dommy/replace! (sel1 :#puzzle-table) (create-template nono))
  (when-let [solved (sel1 [:#puzzle :#solved])]
    (dommy/remove! solved))
  (let [event-chan (chan 5)
        storage window/localStorage
        id (:id nono)]
    (add-handlers event-chan)
    (handle-cell-events event-chan
                        {:fill-style nil
                         :base-cell nil
                         :last-cell nil
                         :drag-type :rect
                         :puzzle nono
                         :storage storage
                         :board (init-board (count (:top nono)) (count (:left nono)))})
    (stg/load-progress storage [id] #(put! event-chan [(% id) :progress-loaded])))
  (show-view :puzzle))

(defn ^:export init [el]
  (dommy/add-class! el "center")
  (append! el [:div.button-container
               [:p.button#button-clear "clear"]])
  (append! el [:div#puzzle-view.center [:div#puzzle-table]])
  (show {:left [[5] [5] [5] [5] [5]]
         :top [[5] [5] [5] [5] [5]]}))
