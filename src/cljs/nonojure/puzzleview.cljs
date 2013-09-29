(ns nonojure.puzzleview
  (:require
   [dommy.utils :as utils]
   [dommy.core :as dommy]
   [jayq.util :refer [log]])
  (:use-macros
   [dommy.macros :only [node sel sel1]]))

;(def test-data {:left [[] [1 2] [2] [4]]
;                :top [[2] [1 1] [3] [2]]})
;
;(def test-result
;  [:table#table.puzzle-table-non {:width 100 :height 100 :id "puzzle-table"}
;  [:tr [:td]        [:td]        [:td]             [:td#.num 1]      [:td]             [:td]]
;  [:tr [:td]        [:td]        [:td#.num 2]      [:td#.num 1]      [:td#.num 3]      [:td#.num 2]]
;  [:tr [:td]        [:td]        [:td#.cell.c0.r0] [:td#.cell.c1.r0] [:td#.cell.c2.r0] [:td#.cell.c3.r0]]
;  [:tr [:td#.num 1] [:td#.num 2] [:td#.cell.c0.r1] [:td#.cell.c1.r1] [:td#.cell.c2.r1] [:td#.cell.c3.r1]]
;  [:tr [:td]        [:td#.num 2] [:td#.cell.c0.r2] [:td#.cell.c1.r2] [:td#.cell.c2.r2] [:td#.cell.c3.r2]]
;  [:tr [:td]        [:td#.num 4] [:td#.cell.c0.r3] [:td#.cell.c1.r3] [:td#.cell.c2.r3] [:td#.cell.c3.r3]]])

(def data {:left [[3 1] [3] [2] [1 1] [1 1]]
           :top [[1 1] [1 3] [2] [1 2] [2]]})

(defn pad [nums beg end value]
  (-> []
      (into (repeat beg value))
      (into nums)
      (into (repeat end value))))

(defn pad-nils
  "Given a sequence of numbers adds desired number of nils in the beginning, and to the end"
  ([nums nil-num]
     (pad-nils nums nil-num 0))
  ([nums nil-beg nil-end]
     (pad nums nil-beg nil-end nil)))

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
  (let [ all-nums (pad-nils nums (- offset (count nums)))
         num-tds (map #(if % [:td.num.num-not-clicked %] [:td.nothing]) all-nums)
         cells (for [c (range row-length)]
                 [:td {:class (str "cell c" c " r" row-num " cell-not-clicked")}])]
    (-> [:tr {:class ""}]
        (into num-tds)
        (into (add-thick-class cells))
        (into (reverse num-tds)))))

(defn create-header [nums offset]
  "Takes a vector of column numbers and offset. Returns template for table header."
  (let [col-num (count nums)
        longest (apply max (map count nums))
        padded  (map #(pad-nils % (- longest (count %))) nums)]
    (into []
      (for [row (range longest)]
        (let [nums-col (map #(nth % row) padded)
              tds  (map #(if % [:td {:class "num num-not-clicked"} %]
                               [:td {:class "nothing"}])
                        nums-col)]
        (into [:tr {:class (cond (zero? row) "first"
                                 (= row (dec longest)) "last"
                                 :default "")}]
              (-> (concat [[:td.hide.has-right]]
                          (add-thick-class tds)
                          [[:td.hide.has-left]])
                  (pad (dec offset) (dec offset) [:td.hide]))))))))

(defn create-bottom [nums offset]
  (let [data (map #(add-class-templ % " footer")
                  (create-header nums offset))]
    data
    )
  )


(defn create-template [data]
  "Create a template for puzzle based on description"
  (let [ top-nums (get data :top)
         width (count top-nums)
         left-nums (get data :left)
         offset (apply max (map count left-nums))
         header (create-header top-nums offset)
         bottom (create-bottom top-nums offset)
         rows (for [r (range width)
                    :let [row (create-row (nth left-nums r) offset r width)]]
                (if (zero? (rem r 5))
                  (add-class-templ row " thick-top")
                  row))]
    (-> [:table#table.puzzle-table-non {:id "puzzle-table"}]
      (into header)
      (into (update-last rows add-class-templ " thick-bottom"))
      (into bottom))))

(defn cell-click [evt node]
  "Change the color when cell is clicked"
  (let [button (.-which evt)
        not-cl (dommy/has-class? node "cell-not-clicked")
        cl     (dommy/has-class? node "cell-clicked")
        r-cl   (dommy/has-class? node "cell-rightclicked")]
    (cond
      (and not-cl (= 1 button)) (do (dommy/remove-class! node "cell-not-clicked")
                                    (dommy/add-class! node "cell-clicked"))
      (and not-cl (= 3 button)) (do (dommy/remove-class! node "cell-not-clicked")
                                    (dommy/add-class! node "cell-rightclicked"))
      (and cl     (= 1 button)) (do (dommy/remove-class! node "cell-clicked")
                                    (dommy/add-class! node "cell-not-clicked"))
      (and cl     (= 3 button)) (do (dommy/remove-class! node "cell-clicked")
                                    (dommy/add-class! node "cell-rightclicked"))
      (and r-cl   (= 1 button)) (do (dommy/remove-class! node "cell-rightclicked")
                                    (dommy/add-class! node "cell-clicked"))
      (and r-cl   (= 3 button)) (do (dommy/remove-class! node "cell-rightclicked")
                                    (dommy/add-class! node "cell-not-clicked"))
      :else false)))


(defn num-click [evt node]
  (let [button (.-which evt)
        not-cl (dommy/has-class? node "num-not-clicked")
        cl     (dommy/has-class? node "num-clicked")]
    (cond
      (and not-cl (or (= 1 button) (= 3 button)))
                                (do (dommy/remove-class! node "num-not-clicked")
                                    (dommy/add-class! node "num-clicked"))
      (and cl (or (= 1 button) (= 3 button)))
                                (do (dommy/remove-class! node "num-clicked")
                                    (dommy/add-class! node "num-not-clicked"))
      :else false)))

(defn add-handlers []
  (let [cells (sel ".cell")
        nums (sel ".num")]
      (doseq [cell cells]
        (set! (.-onmousedown cell) #(cell-click % cell))
        (set! (.-oncontextmenu cell) (fn [evt] false)))
      (doseq [num nums]
        (set! (.-onmousedown num) #(num-click % num))
        (set! (.-oncontextmenu num) (fn [evt] false)))

    ))

(defn show [nono]
  (do
    (dommy/replace! (sel1 :#puzzle-table) (create-template nono))
    (add-handlers)))

(defn ^:export init []
  (when (and js/document
             (aget js/document "getElementById"))
    (dommy/prepend! (sel1 :#puzzle-view) (create-template (nonojure.random/generate-puzzle 10 10)))
    (add-handlers)))
