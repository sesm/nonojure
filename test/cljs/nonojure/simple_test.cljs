(ns nonojure.simple-test
  (:require-macros [nonojure.riv-tests :refer [deftest is are testing]])
  (:require [nonojure.riv-tests :refer [test-started test-finished]]))

(deftest hello-world
  (is (= 2 (+ 1 1))))

(deftest are-tests
  (are [a b c] (= c (+ a b))
       1 2 3
       2 3 5))

(deftest async-test
  (test-started)
  (testing "2 equals 3 or 2 = 2"
    (is (or (= 2 3)
            (= 2 2))))
  (test-finished))
