(ns nonojure.storage
  (:require [jayq.util :refer [log]]))

(defprotocol Storage
  (load-progress [this])
  (load-progress-for-puzzle [this id])
  (save-auto! [this id progress])
  (save-manual! [this id progress])
  (solved! [this id solution]))

(defn to-str [data]
  (.stringify js/JSON (clj->js data)))

(defn from-str [str]
  (js->clj (.parse js/JSON str) :keywordize-keys true))

(defn get-item [storage key]
  (from-str (.getItem storage key)))

(defn set-item [storage key data]
  (.setItem storage key (to-str data)))

(defn- update-progress [storage id progress status]
  (let [all (or (get-item storage "progressAll") {})]
    (set-item storage "progressAll" (assoc all id status)))
  (set-item storage id
            (if-let [existing (get-item storage  id)]
              (merge existing progress)
              progress)))

(extend-protocol Storage
  js/Storage
  (load-progress [this]
    (get-item this "progressAll"))
  (load-progress-for-puzzle [this id]
    (get-item this id))
  (save-auto! [this id progress]
    (update-progress this id {:auto progress} :in-progress))
  (save-manual! [this id progress]
    (update-progress this id {:manual progress} :in-progress))
  (solved! [this id solution]
    (update-progress this id {:solution solution} :solved)))

(defn ^:export init []
  (let [log #(log (clj->js %))
        storage window/localStorage]
    (log (load-progress storage))

    (log "Save auto mar")
    (save-auto! storage "mar" {:left [[1] [2]] :top [[1]]})
    (log (load-progress storage))
    (log (load-progress-for-puzzle storage "mar"))

    (log "Save auto asf")
    (save-auto! storage "asf" {:left [[1] [2]] :top [[1]]})
    (log (load-progress storage))
    (log (load-progress-for-puzzle storage "asf"))

    (log "Save manual mar")
    (save-manual! storage "mar" {:left [[1] [2]] :top [[1]]})
    (log (load-progress storage))
    (log (load-progress-for-puzzle storage "mar"))

    (log "Solving asf")
    (solved! storage "asf" {:left [] :right []})
    (log (load-progress storage))
    (log (load-progress-for-puzzle storage "asf"))

    ))
