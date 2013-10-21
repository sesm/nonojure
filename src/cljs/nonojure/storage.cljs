(ns nonojure.storage
  (:require [nonojure.utils :refer [log]]))

(defprotocol Storage
  (load-progress [this ids callback])
  (save-puzzle-progress [this id progress callback])
  (mark-puzzle-solved [this id solution callback]))

(defn to-str [data]
  (.stringify js/JSON (clj->js data)))

(defn from-str [str]
  (js->clj (.parse js/JSON str) :keywordize-keys true))

(defn get-item [storage key]
  (from-str (.getItem storage key)))

(defn set-item [storage key data]
  (.setItem storage key (to-str data)))

(defn- update-progress [storage id progress status]
  (let [old-item (or (get-item storage id) {})
        status (if (= (:status old-item) "solved") :solved status)]
    (set-item storage id (merge old-item progress {:status status}))))

(defn- keywordize-vals [board]
  (mapv #(mapv keyword %) board))

(defn- keywordize-boards [progress & boards]
  (reduce (fn [progress board]
            (if (progress board)
              (update-in progress [board] keywordize-vals)
              progress))
          progress
          boards))

(defn- safe-call [callback result]
  ((or callback identity) result))

(extend-protocol Storage
  js/Storage
  (load-progress [this ids callback]
    (let [load-single-puzzle (fn [id]
                               (if-let [item (get-item this (keyword id))]
                                 (keywordize-boards item :auto :solution)
                                 nil))
          items (into {} (for [id ids
                               :let [item (load-single-puzzle id)]
                               :when item]
                           [id item]))]
     (safe-call callback items)))
  (save-puzzle-progress [this id progress callback]
    (safe-call callback (update-progress this (keyword id) {:auto progress} :in-progress)))
  (mark-puzzle-solved [this id solution callback]
    (safe-call callback (update-progress this (keyword id)
                                         {:auto nil
                                          :solution solution}
                                         :solved)))
  nil
  (load-progress [this ids callback]
    (safe-call callback {}))
  (save-puzzle-progress [this id progress callback]
    (safe-call callback nil))
  (mark-puzzle-solved [this id solution callback]
    (safe-call callback nil)))
