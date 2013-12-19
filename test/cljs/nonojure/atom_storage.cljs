(ns nonojure.atom-storage
  (:require [nonojure.utils :refer [log]]
            [nonojure.storage :as stg]))

(defn create-storage []
  (atom {:puzzles {}
         :preferences {}}))

(extend-protocol nonojure.storage/Storage
  cljs.core/Atom
  (load-progress [storage ids callback]
    (-> (:puzzles @storage)
        (select-keys ids)
        (callback)))
  (save-puzzle-progress [storage id progress callback]
    (if-let [puzzle (get-in @storage [:puzzles id])]
      (swap! storage assoc-in [:puzzles id :current-state] progress)
      (swap! storage assoc-in [:puzzles id] {:status "in-progress"
                                             :current-state progress}))
    (callback :ok))
  (remove-puzzle-progress [storage id callback]
    (swap! storage (fn [storage]
                     (update-in storage [:puzzles] dissoc id)))
    (callback :ok))
  (mark-puzzle-solved [storage id solution callback]
    (swap! storage assoc-in [:puzzles id] {:status "solved"
                                           :current-state nil
                                           :solution solution})
    (callback :ok))
  (save-preferences [storage preferences callback]
    (swap! storage update-in [:preferences] merge preferences)
    (callback :ok))
  (load-preferences [storage callback]
    (callback (:preferences @storage)))
  (load-short-progress [storage callback]
    (callback
     (into {}
           (for [[id progress] (:puzzles @storage)]
             [id (:status progress)]))))
)
