(ns nonojure.random)

(defn- random-row [size]
  (let [k (rand-int (inc size))]
    (shuffle (concat (repeat k "x")
                     (repeat (- size k) " ")))))

(defn- row-to-numbers [row]
  (->> (partition-by identity row)
       (remove #(= " " (first %)))
       (map count)))

(defn generate-puzzle [n m]
  (let [rows (repeatedly n #(random-row m))
        left (map row-to-numbers rows)
        top (->> (apply map vector rows)
                 (map row-to-numbers))]
    {:left left
     :top top
     :solution (map #(apply str %) rows)}))


(defn test [n m]
  (let [puzzle (generate-puzzle n m)]
   (println (format "\nPuzzle %dx%d" n m))
   (doseq [row (:solution puzzle)] (println row))
   (println "left:" (:left puzzle))
   (println "top:" (:top puzzle))))

