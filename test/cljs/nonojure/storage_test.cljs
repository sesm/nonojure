(ns nonojure.storage-test
  (:require-macros [nonojure.riv-tests
                    :refer [is deftest testing]])
  (:require [nonojure.storage :as stg]
            [nonojure.atom-storage :as atom-stg]
            nonojure.riv-tests))

(defn noop [])

(defn test-storage-implementation [create-storage]
  (testing "test puzzle operations"
    ; For sake of simplicity ignore fact that storage functions
    ; are callback oriented and suppose that they executes as soon as called.
    ; This is true for all implementations except ServerStorage.
    (let [storage (create-storage)
          save-progress #(stg/save-puzzle-progress storage %1 %2 noop)
          mark-solved #(stg/mark-puzzle-solved storage %1 %2 noop)
          assert-storage (fn [ids expected-short expected-full]
                           (stg/load-short-progress storage
                                                    #(is (= % expected-short)))
                           (stg/load-progress storage ids
                                              #(is (= % expected-full))))]
      (testing "initial state is empty"
        (assert-storage ["1" "2" "3"]
                        {}
                        {}))

      (testing "save progress for 2 puzzles"
        (save-progress "1" [[:empty]])
        (save-progress "2" [[:crossed]])

        (assert-storage ["1" "2" "3"]
                        {"1" "in-progress"
                         "2" "in-progress"}
                        {"1" {:status "in-progress"
                              :current-state [[:empty]]}
                         "2" {:status "in-progress"
                              :current-state [[:crossed]]}}))

      (testing "updating progress for 1st puzzle and solving 2nd puzzle"
        (save-progress "1" [[:crossed]])
        (mark-solved "2" [[:filled]])

        (assert-storage ["1" "2" "3"]
                        {"1" "in-progress"
                         "2" "solved"}
                        {"1" {:status "in-progress"
                              :current-state [[:crossed]]}
                         "2" {:status "solved"
                              :current-state nil
                              :solution [[:filled]]}}))

      (testing "solving 1st puzzle and start 2nd again"
        (mark-solved "1" [[:filled :filled]])
        (save-progress "2" [[:empty]])

        (assert-storage ["1" "2" "3"]
                        {"1" "solved"
                         "2" "solved"}
                        {"1" {:status "solved"
                              :current-state nil
                              :solution [[:filled :filled]]}
                         "2" {:status "solved"
                              :current-state [[:empty]]
                              :solution [[:filled]]}}))

      (testing "removing 1st and solving 2nd for second time"
        (stg/remove-puzzle-progress storage "1" noop)
        (mark-solved "2" [[:filled]])

        (assert-storage ["1" "2" "3"]
                        {"2" "solved"}
                        {"2" {:status "solved"
                              :current-state nil
                              :solution [[:filled]]}}))))

  (testing "preferences functions"
    (let [storage (create-storage)]
      (stg/load-preferences storage #(is (or (nil? %) (empty? %))))
      (stg/save-preferences storage {:color "red"
                                     :style "bold"} noop)
      (stg/load-preferences storage #(is (= % {:color "red"
                                               :style "bold"})))
      (stg/save-preferences storage {:color "blue"
                                     :size "BIG"} noop)
      (stg/load-preferences storage #(is (= % {:color "blue"
                                               :size "BIG"
                                               :style "bold"}))))))

(deftest test-atom-storage-impl
  (test-storage-implementation atom-stg/create-storage))
