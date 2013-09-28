(ns nonojure.db
  (:require [monger
             [core :as mg]
             [collection :as mc]
             [query :as mq]
             [operators :refer [$gte $lte $or]]]
            [nonojure.random :refer [generate-puzzle]])
  (:import [org.bson.types ObjectId]))

(def nono-coll "nonograms")

(defn connect []
  (mg/connect!)
  (mg/set-db! (mg/get-db "nonojure")))

(defn insert-nonogram [nonogram]
  (let [id (ObjectId.)
        nonogram (assoc nonogram
                   :_id id
                   :width (count (:top nonogram))
                   :height (count (:left nonogram)))]
    (mc/insert-and-return nono-coll nonogram)))

(defn- find-clause [field [min max]]
  (case field
    :rating {:rating {$gte min $lte max}}
    :size {$or [{:width {$gte min}}
                {:height {$gte min}}]
           :width {$lte max}
           :height {$lte max}}
    {}))

(defn- sort-clause [field order]
  {field (if (= order :asc) 1 -1)})

(defn- prepare-nono-for-client [nono]
  (-> nono
      (dissoc :_id)
      (assoc :id (str (:_id nono)))))

(defn read-nonograms [{:keys [filter-field
                              filter-value
                              sort-field
                              sort-order]}]
  (->> (mq/with-collection nono-coll
         (mq/find (find-clause filter-field filter-value))
         (mq/sort (sort-clause sort-field sort-order)))
       (map prepare-nono-for-client)))

(defn fill-db-with-random-puzzles []
  (doseq [width (range 5 31 5)
          height (range 5 31 5)]
    (insert-nonogram (generate-puzzle height width))))


