(ns nonojure.db
  (:require [monger
             [core :as mg]
             [collection :as mc]
             [query :as mq]
             [operators :refer [$gte $lte $or]]]
            [nonojure.random :refer [generate-puzzle]]
            [taoensso.timbre :refer [error warn]])
  (:import [org.bson.types ObjectId]))

(def nono-coll "nonograms")

(defn connect []
  (try (mg/connect!)
       (mg/set-db! (mg/get-db "nonojure"))
       (catch Exception e
         (error (str "Couldn't connect to mongo " e)))))

(defn insert-nonogram [nonogram]
  (let [id (ObjectId.)
        width (count (:top nonogram))
        height (count (:left nonogram))
        nonogram (assoc nonogram
                   :_id id
                   :size (max width height)
                   :width width
                   :height height
                   :difficulty 0
                   :times-rated 0)]
    (mc/insert-and-return nono-coll nonogram)))

(defn- find-clause [field [min max]]
  (if field
    {field {$gte min $lte max}}
    {}))

(defn- sort-clause [field order]
  (if field
    (array-map field (if (= order :asc) 1 -1)
               :_id 1)
    {:_id 1}))

(defn- prepare-nono-for-client [nono]
  (-> nono
      (dissoc :_id :size)
      (assoc :id (str (:_id nono)))))

(defn find-nonograms [{:keys [filter-field
                              filter-value
                              sort-field
                              sort-order]}]
  (->> (mq/with-collection nono-coll
         (mq/find (find-clause filter-field filter-value))
         (mq/fields [:_id :width :height :difficulty :times-rated])
         (mq/sort (sort-clause sort-field sort-order)))
       (map prepare-nono-for-client)))

(defn find-nonogram-by-id [id]
  (when-let [nono (mc/find-one-as-map nono-coll {:_id (ObjectId. id)})]
    (prepare-nono-for-client nono)))

(defn update-difficulty [id difficulty]
  (when-let [nono (mc/find-one-as-map nono-coll {:_id (ObjectId. id)})]
    (when (<= 1 difficulty 3)
      (let [times (:times-rated nono)
            new-difficulty (-> (:difficulty nono)
                           (* times)
                           (+ difficulty)
                           (/ (inc times)))
            new-nono (assoc nono
                       :difficulty new-difficulty
                       :times-rated (inc times))]
        (mc/update-by-id nono-coll (ObjectId. id) new-nono)
        (find-nonogram-by-id id)))))

(defn fill-db-with-random-puzzles []
  (doseq [width (range 5 31 5)
          height (range 5 31 5)]
    (insert-nonogram (generate-puzzle height width))))


#_(

   (mc/remove nono-coll)

   (fill-db-with-random-puzzles)

   )

