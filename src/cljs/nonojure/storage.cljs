(ns nonojure.storage
  (:require [nonojure.utils :refer [log ajax]]
            [nonojure.shared.endec :refer [encode-board decode-board]]
            [clojure.string :refer [join]]))

(defprotocol Storage
  (load-progress [storage ids callback])
  (save-puzzle-progress [storage id progress callback])
  (mark-puzzle-solved [storage id solution callback])
  (save-preferences [storage preferences callback])
  (load-preferences [storage callback])
  (load-short-progress [storage callback]))

(def pref-key "preferences")

(defn- to-str [data]
  (.stringify js/JSON (clj->js data)))

(defn- from-str [str]
  (js->clj (.parse js/JSON str) :keywordize-keys true))

(defn- get-item [storage key]
  (from-str (.getItem storage key)))

(defn- set-item [storage key data]
  (.setItem storage key (to-str data)))

(defn- encode-boards [progress]
  (reduce (fn [progress type]
            (if-let [board (progress type)]
              (assoc progress
                type (encode-board board))
              progress))
          progress
          [:current-state :solution]))

(defn- decode-boards [progress]
  (reduce (fn [progress type]
            (if-let [board (progress type)]
              (assoc progress
                type (decode-board board))
              progress))
          progress
          [:current-state :solution]))

(defn- update-progress [storage id progress status]
  (let [key (str "puzzle-" (name id))
        old-item (or (get-item storage key) {})
        status (if (= (:status old-item) "solved") :solved status)
        new-item (merge old-item
                        (encode-boards progress)
                        {:status status})]
    (set-item storage key new-item)))

(defn- get-short-progress [storage]
  (letfn [(puzzle-key? [key]
            (zero? (.indexOf key "puzzle-")))
          (get-short-item [key]
            (let [id (subs key 7)]
              [id (:status (get-item storage key))]))]
    (->> (.-length storage)
         range
         (map #(.key storage %))
         (filter puzzle-key?)
         (map get-short-item)
         (into {}))))

(defn- safe-call [callback result]
  ((or callback identity) result))

(extend-protocol Storage

  js/Storage
  (load-progress [storage ids callback]
    (let [load-single-puzzle (fn [key]
                               (if-let [item (get-item storage key)]
                                 (decode-boards item)
                                 nil))
          items (into {} (for [id ids
                               :let [key (str "puzzle-" id)
                                     item (load-single-puzzle key)]
                               :when item]
                           [id item]))]
     (safe-call callback items)))
  (save-puzzle-progress [storage id progress callback]
    (safe-call callback (update-progress storage id {:current-state progress} :in-progress)))
  (mark-puzzle-solved [storage id solution callback]
    (safe-call callback (update-progress storage id
                                         {:current-state nil
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
    (safe-call callback nil))
  (load-short-progress [storage callback]
    (safe-call callback (get-short-progress storage))))

(deftype ServerStorage []
  Storage
  (load-progress [storage ids callback]
    (let [url (str "/api/user/get-progress?ids=" (join "," ids))
          decode-callback (fn [progress]
                            (when callback
                              (->> (for [[id progress] progress]
                                     [(name id) (decode-boards progress)])
                                   (into {})
                                   callback)))]
      (ajax url decode-callback)))
  (save-puzzle-progress [storage id progress callback]
    (ajax "/api/user/save-puzzle-progress" callback :POST
          (encode-boards {:puzzle-id id
                          :current-state progress})))
  (mark-puzzle-solved [storage id solution callback]
    (ajax "/api/user/mark-puzzle-solved" callback :POST
          (encode-boards {:puzzle-id id
                          :solution solution})))
  (save-preferences [storage preferences callback]
    (ajax "/api/user/save-preferences" callback :POST preferences))
  (load-preferences [storage callback]
    (ajax "/api/user/get-preferences" callback))
  (load-short-progress [storage callback]
    (ajax "/api/user/get-short-progress-all-puzzles" callback)))

(def storage (atom window/localStorage))

