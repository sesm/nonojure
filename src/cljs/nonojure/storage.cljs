(ns nonojure.storage
  (:require [nonojure.utils :refer [log]]
            [nonojure.endec :refer [encode-board decode-board]]))

(defprotocol Storage
  (load-progress [storage ids callback])
  (save-puzzle-progress [storage id progress callback])
  (mark-puzzle-solved [storage id solution callback])
  (save-preferences [storage preferences callback])
  (load-preferences [storage callback]))

(def pref-key "preferences")

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
  (load-progress [storage ids callback]
    (let [load-single-puzzle (fn [id]
                               (if-let [item (get-item storage (keyword id))]
                                 (decode-boards item)
                                 nil))
          items (into {} (for [id ids
                               :let [item (load-single-puzzle id)]
                               :when item]
                           [id item]))]
     (safe-call callback items)))
  (save-puzzle-progress [storage id progress callback]
    (safe-call callback (update-progress storage (keyword id) {:auto progress} :in-progress)))
  (mark-puzzle-solved [storage id solution callback]
    (safe-call callback (update-progress storage (keyword id)
                                         {:auto nil
                                          :solution solution}
                                         :solved)))
  (save-preferences [storage preferences callback]
    (safe-call callback (set-item storage pref-key preferences)))
  (load-preferences [storage callback]
    (safe-call callback (get-item storage pref-key)))

  nil
  (load-progress [storage ids callback]
    (safe-call callback {}))
  (save-puzzle-progress [storage id progress callback]
    (safe-call callback nil))
  (mark-puzzle-solved [storage id solution callback]
    (safe-call callback nil))
  (save-preferences [storage preferences callback]
    (safe-call callback nil))
  (load-preferences [storage callback]
    (safe-call callback nil)))
