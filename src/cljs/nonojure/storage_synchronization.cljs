(ns nonojure.storage-synchronization
  (:require [nonojure.utils :refer [log ajax]]
            [nonojure.storage :as stg]
            [cljs.core.async :refer [put! <! >! chan close!] :as async])
  (:use-macros
   [cljs.core.async.macros :only [go go-loop]]))

;;; Algorithm for synchronization storage A to storage B.
;;; Current usecase: move on user login all progress from browser-based
;;; local storage to server storage. In this case A is local storage and
;;; B is server storage. But it shouldn't matter because we're going
;;; to use only Storage protocol functions.

;;; Steps
;;; 1. Get short progress from both storage A. We need to know
;;;    which puzzle progress we need to load from A and B and sync them.
;;; 2. Get full progress from A and B for puzzles by ids we got on step 1.
;;; 3. Iterate through all pairs of puzzle progress and for each progress do:
;;;    a. Compare progresses. Each progress can be in one of 4 states:
;;;      * empty (emp) - user havent't started solving puzzle in given storage
;;;      * in-progress (inp) - user started solving but haven't solved yet,
;;;      * solved (slv) - user solved puzzle (current-progress field is empty)
;;;      * solved, but started solving again (slv-inp) - user solved puzzle,
;;;        but decided to solve it again, current-progress field is not empty
;;;        because it contains current unsolved board state.
;;;
;;;    Based on pair types there may be 3 results:
;;;      * A - wins storage A, it means we need to move progress to storage B
;;;      * B - wins storage B, we don't need to do anything, because correct
;;;        progress already in B
;;;      * ? - we don't know which progress to choose. We need to ask user by
;;;        displaying both choices
;;;
;;;    Pair-result table:
;;;    +---------+-----+-----+-----+---------+
;;;    | ↓A \ B→ | emp | inp | slv | slv-inp |
;;;    +---------+-----+-----+-----+---------+
;;;    |   emp   |  B  |  B  |  B  |    B    |
;;;    |   inp   |  A  |  ?  |  B  |    B    |
;;;    |   slv   |  A  |  A  |  B  |    B    |
;;;    | slv-inp |  A  |  A  |  A  |    ?    |
;;;    +---------+-----+-----+-----+---------+
;;;
;;;    b. If result of comparison is A - move progress to B.
;;;    It may require following:
;;;      * A is inp - save-puzzle-progress on B
;;;      * A is slv - mark-puzzle-solved on B
;;;      * A is slv-inp - save-puzzle-progress and maybe mark-puzzle-solved on B
;;;    If result is B - do nothing.
;;;
;;;    c. If has more pair - go to next pair.
;;; 4. Load full progress for B again to verify stored correctly.
;;; 5. Remove all progress from A.
;;; 6. Synchronize preferences.
;;;   a. Load both prefs from A and B.
;;;   b. Merge maps so preferences from B override A.
;;;      For now B (server) preferences are always more important.
;;;   c. Save result to B.
;;; 7. ????
;;; 8. PROFIT!!!

(defn astg
  "Executes storage function returning channel that
will get function result. Storage function args should not
include callback as last argument. It will be added by this function."
  [storage-fn & args]
  (let [result (chan)
        callback #(put! result (if (nil? %) :nil %))
        args (concat args [callback])]
    (apply storage-fn args)
    result))

(defn collect
  "Returns new channel that will eventually contain collection of n elements from given channel.
It is used to 'block' some actions until we get certain amount of elements."
  [channel n]
  (async/reduce conj [] (async/take n channel)))

(defn progress-state [{:keys [status current-state]}]
  (cond (nil? status) :emp
        (and (= status "solved")
             (not (nil? current-state))) :slv-inp
        (= status "solved") :slv
        (= status "in-progress") :inp))

(defn save-to-storage-b
  "Save puzzle progress to storage B. Calls callback when done."
  [id a-state b-state a-prog storage-b callback]
  (let [save-progress #(stg/save-puzzle-progress storage-b id (:current-state a-prog) %)
        mark-solved #(stg/mark-puzzle-solved storage-b id (:solution a-prog) %)]
    (cond (= a-state :inp) (save-progress callback)
          (= a-state :slv) (mark-solved callback)
          (and (= a-state :slv-inp)
               (or (= b-state :slv)
                   (= b-state :slv-inp))) (save-progress callback)
          (= a-state :slv-inp) (mark-solved #(save-progress callback)))))

(def a-wins-states
  #{[:inp :emp]
    [:slv :emp]
    [:slv :inp]
    [:slv-inp :emp]
    [:slv-inp :inp]
    [:slv-inp :slv]})

(def b-wins-states
  #{[:emp :emp]
    [:emp :inp]
    [:emp :slv]
    [:emp :slv-inp]
    [:inp :slv]
    [:inp :slv-inp]
    [:slv :slv]
    [:slv :slv-inp]})

(def ask-user-states
  #{[:inp :inp]
    [:slv-inp :slv-inp]})

(defn synchronize-puzzle
  "Synchronizes versions of given puzzle. Returns channel that will contain single element:
vector of 2 elements - id and result progress for this puzzle."
  [id a-prog b-prog storage-b ask-user-callback]
  (go
   (let [a-state (progress-state a-prog)
         b-state (progress-state b-prog)
         winner (condp contains? [a-state b-state]
                  a-wins-states :a
                  b-wins-states :b
                  ask-user-states (<! (ask-user-callback id a-prog b-prog)))
         result (chan)]
     (if (= winner :a)
       (let [requests-to-b (save-to-storage-b id a-state b-state a-prog storage-b
                             #(do
                                (log "Got response for puzzle" id)
                                (put! result id)))]
         (<! (collect result requests-to-b))
         [id a-prog])
       [id b-prog]))))

(defn clear-storage
  "Clears storage by removing all puzzle progress from it."
  [storage ids]
  (let [deleted-chan (chan)]
    (doseq [id ids]
      (log "Remove" id "from storage")
      ;(put! deleted-chan id)
      (stg/remove-puzzle-progress storage id #(put! deleted-chan id))
     )
    (go (collect deleted-chan (count ids)))))

(defn synchronize-all-puzzles
  "Synchronizes given puzzles (step 3). Returns channel that will get single element -
expected state of progress in storage B. This state should be used to verify
that B has correct state afterall"
  [a b storage-b ask-user-callback]
  (log "Started all sync")
  (go (let [result (->> (keys a)
                        (map #(synchronize-puzzle
                               % (a %) (b %) storage-b ask-user-callback))
                        async/merge
                        (async/into {}))]
        (<! result))))

(defn synchronize-storages [storage-a storage-b finish-callback ask-user-callback]
  (log "Started synchronization")
  (go
   (log "Started synchronization")
   (let [; Get short progress (step 1) and extract ids.
         ids (keys (<! (astg stg/load-short-progress storage-a)))
         _ (log "ids" ids)

         ; Load full progress (step 2).
         a (<! (astg stg/load-progress storage-a ids))
         b (<! (astg stg/load-progress storage-b ids))

         ; Synchronize puzzles (step 3).
         expected-b-progress (<! (synchronize-all-puzzles a b storage-b ask-user-callback))

         ; Get real progress from storage B (step 4).
         real-b-progress (<! (astg stg/load-progress storage-b ids))]
     (log "Verifying")
     (if (= real-b-progress expected-b-progress)
       (do (<! (clear-storage storage-a ids))
           (finish-callback))
       (do (log "Bad synchronization :(((((")
           (doseq [key (keys expected-b-progress)]
             (when (not= (real-b-progress key) (expected-b-progress key))
               (log key (expected-b-progress key) (real-b-progress key))))
;           (log "expected" expected-b-progress)
;           (log "actual" real-b-progress)
           )))))

