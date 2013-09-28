(ns nonojure.puzzleview
  (:require
   [dommy.utils :as utils]
   [dommy.core :as dommy])
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

;(def data {:left [[3 1] [3] [2] [1 1] [1 1]]
;           :top [[1 1] [1 3] [2] [1 2] [2]]})

(defn pad-nils [nums length]
  "Given a sequence of nils and desired length, adds nils to the beginning
  to reach desired length"
  (let [ nil-num (- length (count nums))
         nils (take nil-num (cycle [nil]))]
    (if (>= 0 nil-num)
      nums
      (into (vec nils) nums))))

(defn create-row [nums offset row-num row-length]
  "Takes a vector of row numbers, offset (assumed to be longer then row numbers vector),
  row number and row length. Returns template for nodes construction"
  (let [ all-nums (pad-nils nums offset)
         num-tds (map #(if % [:td#.num %] [:td]) all-nums)
         cells (for [c (range row-length)] [(keyword (str "td#.cell.c" c ".r" row-num))])]
    (-> [:tr]
      (into num-tds)
      (into cells))))

(defn create-header [nums offset]
  "Takes a vector of column numbers and offset. Returns template for table header."
  (let [col-num (count nums)
        longest (apply max (map count nums))
        padded  (map #(pad-nils % longest) nums)]
    (into []
      (for [row (range longest)]
        (let [nums-col (map #(nth % row) padded)
              padded (pad-nils nums-col (+ offset col-num))
              tds (map #(if % [:td#.num %] [:td]) padded)]
        (into [:tr] tds))))))

(defn create-template [data]
  "Create a template for puzzle based on description"
  (let [ top-nums (get data :top)
         width (count top-nums)
         left-nums (get data :left)
         offset (apply max (map count left-nums))
         header (create-header top-nums offset)
         rows (for [r (range width)]
                (create-row (nth left-nums r)
                            offset
                            r
                            width))]
    (-> [:table#table.puzzle-table-non {:width 100 :height 100 :id "puzzle-table"}]
      (into header)
      (into rows))))

(defn cell-click-handler [evt node]
  (if (dommy/has-class? node "cell-clicked")
      (do
        (dommy/remove-class! node "cell-clicked")
        (dommy/add-class! node "cell-not-clicked"))
      (do
        (dommy/remove-class! node "cell-not-clicked")
        (dommy/add-class! node "cell-clicked"))))

(defn ^:export init []
  (when (and js/document
             (aget js/document "getElementById"))
    (dommy/prepend! (sel1 :#puzzle-view) (create-template (nonojure.random/generate-puzzle 10 10)))
    (let [cells (sel ".cell")]
      (doseq [cell cells] (set! (.-onmousedown cell) #(cell-click-handler % cell))))))
