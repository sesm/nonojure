(ns nonojure.storage
  (:require [nonojure.utils :refer [log]]
            [nonojure.endec :refer [encode-board decode-board]]))

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

(defn encode-boards [progress]
  (reduce (fn [progress type]
            (if-let [board (progress type)]
              (assoc progress
                type (encode-board board)
                :width (count (first board)))
              progress))
          progress
          [:auto :solution]))

(defn decode-boards [progress]
  (let [width (:width progress)]
    (reduce (fn [progress type]
              (if-let [board (progress type)]
                (assoc progress
                  type (decode-board board width))
                progress))
            progress
            [:auto :solution])))

(defn- update-progress [storage id progress status]
  (let [old-item (or (get-item storage id) {})
        status (if (= (:status old-item) "solved") :solved status)
        new-item (merge old-item
                        (encode-boards progress)
                        {:status status})]
    (set-item storage id new-item)))

(defn- safe-call [callback result]
  ((or callback identity) result))

(extend-protocol Storage
  js/Storage
  (load-progress [this ids callback]
    (let [load-single-puzzle (fn [id]
                               (if-let [item (get-item this (keyword id))]
                                 (decode-boards item)
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
