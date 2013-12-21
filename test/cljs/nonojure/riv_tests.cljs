; Reinvent the wheel mini framework to allow "async" tests.
(ns nonojure.riv-tests
  (:require [nonojure.utils :refer [log]]))

(def tests (atom []))

(def summary (atom {}))

(def running-tests (atom 0))

(def on-finish (atom nil))

(declare run-next-test)

(defn test-started []
  (swap! running-tests inc))

(defn test-finished []
  (swap! running-tests dec)
  (run-next-test))

(defn do-report [{:keys [type expected actual]}]
  (when-not (= type :pass)
    (log type)
    (log "expected" expected)
    (log "actual" actual))
  (swap! summary update-in [:asserts type] inc))

(defn run-test [[ns name test-fn]]
  (swap! summary update-in [:tests] inc)
  (log "Running" name "from" ns)
  (test-started)
  (test-fn)
  (test-finished))

(defn run-next-test []
  (when (zero? @running-tests)
    (if (empty? @tests)
      (do (log @summary)
          (when @on-finish
            (@on-finish)))
      (let [test (first @tests)]
        (swap! tests rest)
        (run-test test)))))

(defn register-test! [ns name test-fn]
  (swap! tests conj [ns name test-fn]))

(defn run-tests [on-finish-fn]
  (reset! on-finish on-finish-fn)
  (reset! summary {:tests 0
                   :asserts {:pass 0
                             :fail 0
                             :error 0}})
  (reset! running-tests 0)
  (log "started")
  (run-next-test))
