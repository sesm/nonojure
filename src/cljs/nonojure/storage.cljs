(ns nonojure.storage
  (:require [nonojure.utils :refer [log]]))

(defprotocol Storage
  (load-all-puzzles-progress [this callback])
  (load-puzzle-progress [this id callback])
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
  (let [all (or (get-item storage "progressAll") {})
        status (if (= (all id) "solved") :solved status)]
    (log "update progress" id all status (all id))
    (set-item storage "progressAll" (assoc all id status)))
  (set-item storage id
            (if-let [existing (get-item storage  id)]
              (merge existing progress)
              progress)))

(defn- keywordize-vals [board]
  (mapv #(mapv keyword %) board))

(defn- safe-call [callback result]
  ((or callback identity) result))

(extend-protocol Storage
  js/Storage
  (load-all-puzzles-progress [this callback]
    (safe-call callback (get-item this "progressAll")))
  (load-puzzle-progress [this id callback]
    (let [item (get-item this (keyword id))
          item (if (and item (:auto item))
                 (update-in item [:auto] keywordize-vals)
                 item)]
     (safe-call callback item)))
  (save-puzzle-progress [this id progress callback]
    (safe-call callback (update-progress this (keyword id) {:auto progress} :in-progress)))
  (mark-puzzle-solved [this id solution callback]
    (safe-call callback (update-progress this (keyword id)
                                         {:auto nil
                                          :solved? true}
                                         :solved)))
  nil
  (load-all-puzzles-progress [this callback]
    (safe-call callback {}))
  (load-puzzle-progress [this id callback]
    (safe-call callback nil))
  (save-puzzle-progress [this id progress callback]
    (safe-call callback nil))
  (mark-puzzle-solved [this id solution callback]
    (safe-call callback nil)))
