(ns nonojure.storage-synchronization-test
  (:require [nonojure.storage-synchronization :refer [synchronize-storages collect]]
            [nonojure.storage :as stg]
            [nonojure.atom-storage :refer [create-storage]]
            [nonojure.utils :refer [log]]
            [nonojure.riv-tests :refer [test-started test-finished]]
            [cljs.core.async :refer [take! chan put!]])
  (:require-macros [nonojure.riv-tests
                    :refer [is deftest testing are]]
                   [cljs.core.async.macros :refer [go]]))


;;; Cases to test
;;; +---------+-----+-----+-----+---------+
;;; | ↓A \ B→ | emp | inp | slv | slv-inp |
;;; +---------+-----+-----+-----+---------+
;;; | emp     | -   | +   | +   | +       |
;;; | inp     | +   | ++  | +   | +       |
;;; | slv     | +   | +   | +   | +       |
;;; | slv-inp | +   | +   | +   | ++      |
;;; +---------+-----+-----+-----+---------+
;;; So total 17 cases. Cases with ++ are cases
;;; where we ask user and user has 2 option,
;;; we need to test both options for these cases.

(defn noop [])

(defn save-progress [storage id progress]
  (stg/save-puzzle-progress storage id progress noop))

(defn mark-solved [storage id solution]
  (stg/mark-puzzle-solved storage id solution noop))

;;; For simplicity let's use same boards for all:
;;; * in-progress in storage A
;;; * in-progress in storage B
;;; * solved in storage A
;;; * solved in storage B
(def inp-board-a [[:crossed]])
(def inp-board-b [[:filled]])
; For solutions :crossed and :empty values are equals as solution depends only on :filled.
; Storage synchronization should work correctly in this case.
; Use :crossed and :empty versions for a and b solutions respectively.
(def slv-board-a [[:crossed :filled]])
(def slv-board-b [[:empty :filled]])

(defn fill-storage-a [storage]
  (letfn [(save-progress [id]
            (stg/save-puzzle-progress storage id inp-board-a noop))
          (mark-solved [id]
            (stg/mark-puzzle-solved storage id slv-board-a noop))]
    (doseq [id ["inp->emp" "inp->inp_a" "inp->inp_b" "inp->slv" "inp->slv-inp"]]
      (save-progress id))
    (doseq [id ["slv->emp" "slv->inp" "slv->slv" "slv->slv-inp"]]
      (mark-solved id))
    (doseq [id ["slv-inp->emp" "slv-inp->inp" "slv-inp->slv" "slv-inp->slv-inp_a" "slv-inp->slv-inp_b"]]
      (mark-solved id)
      (save-progress id))))

(defn fill-storage-b [storage]
  (letfn [(save-progress [id]
            (stg/save-puzzle-progress storage id inp-board-b noop))
          (mark-solved [id]
            (stg/mark-puzzle-solved storage id slv-board-b noop))]
    (doseq [id ["emp->inp" "inp->inp_a" "inp->inp_b" "slv->inp" "slv-inp->inp"]]
      (save-progress id))
    (doseq [id ["emp->slv" "inp->slv" "slv->slv" "slv-inp->slv"]]
      (mark-solved id))
    (doseq [id ["emp->slv-inp" "inp->slv-inp" "slv->slv-inp" "slv-inp->slv-inp_a" "slv-inp->slv-inp_b"]]
      (mark-solved id)
      (save-progress id))))



(defn check-storage-b-puzzles [puzzles]
  (letfn [(conforms? [name expected-status expected-current-state expected-solution]
            (let [{:keys [status current-state solution]} (puzzles name)]
              (and (= status expected-status)
                   (= current-state expected-current-state)
                   (= solution expected-solution))))]
    (are [name status current-state solution] (conforms? name status current-state solution)
         "emp->inp"           "in-progress" inp-board-b nil
         "emp->slv"           "solved"      nil         slv-board-b
         "emp->slv-inp"       "solved"      inp-board-b slv-board-b
         "inp->emp"           "in-progress" inp-board-a nil
         "inp->inp_a"         "in-progress" inp-board-a nil
         "inp->inp_b"         "in-progress" inp-board-b nil
         "inp->slv"           "solved"      nil         slv-board-b
         "inp->slv-inp"       "solved"      inp-board-b slv-board-b
         "slv->emp"           "solved"      nil         slv-board-a
         "slv->inp"           "solved"      nil         slv-board-a
         "slv->slv"           "solved"      nil         slv-board-b
         "slv->slv-inp"       "solved"      inp-board-b slv-board-b
         "slv-inp->emp"       "solved"      inp-board-a slv-board-a
         "slv-inp->inp"       "solved"      inp-board-a slv-board-a
         "slv-inp->slv"       "solved"      inp-board-a slv-board-b
         "slv-inp->slv-inp_a" "solved"      inp-board-a slv-board-b
         "slv-inp->slv-inp_b" "solved"      inp-board-b slv-board-b))
  )

(defn ask-user [id _ _]
  (let [answers {"inp->inp_a" :a
                 "inp->inp_b" :b
                 "slv-inp->slv-inp_a" :a
                 "slv-inp->slv-inp_b" :b}]
    (go (answers id))))

(defn load-full-puzzles [storage callback]
  (stg/load-short-progress storage
    (fn [short]
      (stg/load-progress storage (keys short)
                         callback))))

(defn check-storage-a-puzzles [puzzles]
  (testing "check storage A final state. Should be empty"
    (is (empty? puzzles))
    (test-finished)))

(defn check-storages [storage-a storage-b]
  (test-started)
  (load-full-puzzles storage-b
    (fn [puzzles]
      (testing "check storage B final state"
        (check-storage-b-puzzles puzzles)
        (test-finished))))
  (test-started)
  (load-full-puzzles storage-a check-storage-a-puzzles))

(deftest test-collect
  (test-started)
  (let [ch (chan)]
    (doseq [i (range 20)]
      (put! ch i))
    (go (is (= [0 1 2 3 4]
               (<! (collect ch 5))))
        (test-finished))))


(deftest test-synchronize-storages
  (test-started)
  (let [storage-a (create-storage)
        storage-b (create-storage)]
    (fill-storage-a storage-a)
    (fill-storage-b storage-b)
    (synchronize-storages storage-a storage-b
      (fn []
        (check-storages storage-a storage-b)
        (test-finished))
      ask-user)
#_    (doseq [st [storage-a storage-b]]
      (stg/load-short-progress st
        (fn [prog]
          (log prog)
          (stg/load-progress st (keys prog) log))))))

