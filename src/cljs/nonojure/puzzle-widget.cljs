(ns nonojure.puzzle-widget
  (:require [dommy.utils :as utils]
            [dommy.core :as dommy :refer [listen! attr append!]]
            [nonojure.utils :refer [log range-inc]]
            [cljs.core.async :refer [put!]])
  (:use-macros
   [dommy.macros :only [node sel sel1]]))


(defprotocol PuzzleWidget
  (cell-state [widget x y]
    "Get state of cell [x y]. Possible values :empty, :filled, :crossed")

  (change-region! [widget from to state-fn]
    "Visually changes state of region of cells.
  from - one corner of the region in form [x y]
  to - opposite corner in form [x y]
  state-fn - function, takes x and y and returns new state for the cell")

  (change-style! [widget style]
    "Changes global style preferences for widget like fill style, crossed style.
  style - map of style preferences")

  (change-whole-board! [widget board]
    "Visually changes whole board setting it to given one.")

  (clear-puzzle! [widget]
    "Visually reset state of all cells to :empty and remove highlightings of numbers.")

  (highlight-current-row-col! [widget row col]
    "Highlights specified row and column.")

  (highlight-solved-rows-cols! [widget rows cols]
    "Highlight in numbers in solved rows and columns.
  rows and cols - collection of rows/cols as numbers")

  (init! [widget]
    "Initiate widget.")

  (load-puzzle! [widget puzzle]
    "Remove old puzzle and draw new one.")

  (set-no-puzzle-message! [widget]
    "Show message when no puzzle is selected.")

  (subscribe [widget channel]
    "User communicates with widget and generates user events. Widget user events are sent
  to core.async channel in form [event-type data]. Events:
  [:mousedown-cell [x y button]] - user pressed mouse button on a cell
  [:mouseenter-cell [x y button]] - user's cursor entered a cell
  [:mouseup-cell nil] - user released mouse button
  [:mouseleave-board [x y button]] - user's cursor left widget
  [:mouseleave-drawing-board nil] - user's cursor left puzzle (table with cells and numbers
  [:number-click coord] - user clicked on a number
  [:clear nil] - user clicked on 'clear' button
  [:undo nil] - user clicked on 'undo' button
  [:style-changed style-map] - user changed style (filled or cross style)")

  (toggle-number! [widget coord]
    "Change state (highlights or dehighlights) given number.")

  (unload-puzzle! [widget]
    "Remove current puzzle from widget leaving it empty."))

(defn attr-int [cell attribute]
  (js/parseInt (attr cell attribute) 10))

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

(defn change-cell-style! [cell style]
  (case style
    :filled (do (dommy/add-class! cell "filled")
                (dommy/remove-class! cell "crossed"))
    :crossed (do (dommy/add-class! cell "crossed")
                 (dommy/remove-class! cell "filled"))
    :empty (do (dommy/remove-class! cell "filled")
               (dommy/remove-class! cell "crossed"))))

(defn disable-context-menu [evt]
  (.preventDefault evt)
  false)

(defn button [evt]
  (case (.-button evt)
    0 :left
    1 :middle
    2 :right
    :unknown))

(def filled-styles [:filled-blot :filled-square])
(def crossed-styles [:crossed-cross :crossed-dot])

(defn next-style-map [styles]
  (->> (partition 2 1 styles styles)
       (map vec)
       (into {})))

(defn get-style [widget]
  (letfn [(find-current [classes]
            (first (filter #(dommy/has-class? widget %) classes)))]
    {:filled-style (find-current filled-styles)
     :crossed-style (find-current crossed-styles)}))

(extend-protocol PuzzleWidget

  js/HTMLDivElement
  (cell-state [widget x y]
    (let [cell (sel1 widget (str ".cell.c" x ".r" y))]
      (cond (dommy/has-class? cell "filled") :filled
            (dommy/has-class? cell "crossed") :crossed
            :default :empty)))

  (change-region! [widget [from-x from-y] [to-x to-y] state-fn]
    (doseq [x (range-inc from-x to-x)
            y (range-inc from-y to-y)
            :let [cell (sel1 widget (str ".r" y ".c" x))]]
      (change-cell-style! cell (state-fn x y))))

  (change-style! [widget style]
    (apply dommy/remove-class! widget (concat filled-styles crossed-styles))
    (dommy/add-class! widget
                      (style :filled-style (first filled-styles))
                      (style :crossed-style (first crossed-styles))))

  (change-whole-board! [widget board]
    (let [width (count (first board))
          height (count board)]
      (change-region! widget [0 0] [(dec width) (dec height)] (fn [x y] (get-in board [y x])))))

  (clear-puzzle! [widget]
    (doseq [class ["num-clicked" "filled" "crossed"]
            el (sel widget [:#puzzle-table (str "." class)])]
      (dommy/remove-class! el class))
    (highlight-solved-rows-cols! widget [] []))

  (highlight-current-row-col! [widget row col]
    (doseq [el (sel widget [:#puzzle :.highlighted])]
      (dommy/remove-class! el "highlighted"))
    (doseq [el (sel widget [:#puzzle (str ".num.c" col)])]
      (dommy/add-class! el "highlighted"))
    (doseq [el (sel widget [:#puzzle (str ".num.r" row)])]
      (dommy/add-class! el "highlighted")))

  (highlight-solved-rows-cols! [widget rows cols]
    (doseq [el (sel widget :.solved.num)]
      (dommy/remove-class! el "solved"))
    (let [rows (map #(str ".num.r" %) rows)
          cols (map #(str ".num.c" %) cols)]
      (doseq [cls (concat rows cols)
              el (sel widget cls)]
        (dommy/add-class! el "solved"))))

  (init! [widget]
    (dommy/add-class! widget :center))

  (load-puzzle! [widget puzzle]
    (doto widget
      unload-puzzle!
      (append! [:div.button-container
                [:p.button#button-clear "clear"]
                [:p.button#button-undo "undo"]
                [:p.button.image.filled
                 {:title "change fill style"}]
                [:p.button.image.crossed
                 {:title "change space style"}]])
      (append! [:div#puzzle-view.center (create-template puzzle)])))

  (set-no-puzzle-message! [widget]
    (doto widget
      (dommy/clear!)
      (dommy/append! [:div.no-puzzle "Please select nonogram in \"browse\" tab."])))

  (subscribe [widget channel]
    (letfn [(add-event [event-type transformer]
              (fn [browser-event]
                (put! channel [event-type (transformer browser-event)])))
            (cell-info [evt]
              (let [cell (.-target evt)]
                [(attr-int cell :data-x)
                 (attr-int cell :data-y)
                 (button evt)]))
            (number-coord [evt]
              (let [cell (.-target evt)]
                (attr cell :data-coord)))
            (add-change-style [key possible-styles]
              #(let [new-style (update-in (get-style widget) [key] (next-style-map possible-styles))]
                 (put! channel [:change-style new-style])))
            (to-nil [_] nil)]

      (let [table (sel1 widget :#puzzle-table)]
        (listen! [table :.cell]
                 :mousedown (add-event :mousedown-cell cell-info)
                 :contextmenu disable-context-menu
                 :mouseenter (add-event :mouseenter-cell cell-info))
        (listen! table
                 :mouseleave (add-event :mouseleave-board to-nil)
                 :mouseup (add-event :mouseup-cell to-nil))
        (listen! [table "td:not(.cell)"]
                 :mouseenter (add-event :mouseleave-drawing-board to-nil))
        (listen! [table :.num]
                 :mousedown (add-event :number-click number-coord)
                 :contextmenu disable-context-menu)
        (listen! (sel1 widget :#button-clear) :click (add-event :clear to-nil))
        (listen! (sel1 widget :#button-undo) :click (add-event :undo to-nil))
        (listen! (sel1 widget :.button.crossed) :click
                 (add-change-style :crossed-style crossed-styles))
        (listen! (sel1 widget :.button.filled) :click
                 (add-change-style :filled-style filled-styles)))))

  (toggle-number! [widget coord]
    (let [query (str "td[data-coord='" coord "']")]
      (doseq [el (sel widget query)]
        (dommy/toggle-class! el "num-clicked"))))

  (unload-puzzle! [widget]
    (dommy/clear! widget)))
