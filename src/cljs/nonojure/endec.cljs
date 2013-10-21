(ns nonojure.endec)

(defn range-char [from to-inclusive]
  (map char (range (int from) (inc (int to-inclusive)))))

(def int->digit "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789()*+,-./:;<=>?@[]^_")

(def digit->int (into {} (map-indexed #(vector %2 %1) int->digit)))

(def int->cell-state [:empty :filled :crossed])

(def cell-state->int {:empty 0
                      :filled 1
                      :crossed 2})


(defn encode-digit [states]
  (->> (reverse states)
       (map cell-state->int)
       (reduce #(+ (* %1 3) %2) 0)
       (.charAt int->digit)))


(defn encode-row [row]
  (->> (partition-all 4 row)
       (map encode-digit)
       (apply str)))

(defn encode-board [board]
  (map encode-row board))

(defn decode-digit [digit]
  (loop [states []
         digit (digit->int digit)]
    (if (= (count states) 4)
      states
      (let [state (int->cell-state (mod digit 3))]
        (recur (conj states state)
               (quot digit 3))))))

(defn decode-row [row length]
  (vec (take length (mapcat decode-digit row))))

(defn decode-board [board width]
  (mapv #(decode-row % width) board))

(defn random-board [n]
  (repeatedly n #(repeatedly n (fn [] (int->cell-state (rand-int 3))))))

#_(let [n 20
      board (random-board n)
      _ (println board)
      encoded (encode-board board)
      _ (println encoded)
      decoded (decode-board encoded n)
      _ (println decoded)]
  (println (= board decoded)))

