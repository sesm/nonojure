(ns nonojure.riv-tests
  (:require [clojure.template :as temp]
            [cljs.analyzer :refer [*cljs-ns*]]))

(defn assert-predicate
  "Returns generic assertion code for any functional predicate.  The
  'expected' argument to 'report' will contains the original form, the
  'actual' argument will contain the form with all its sub-forms
  evaluated.  If the predicate returns false, the 'actual' form will
  be wrapped in (not...)."
  {:added "1.1"}
  [form]
  (let [args (rest form)
        pred (first form)]
    `(let [values# (list ~@args)
           result# (apply ~pred values#)]
       (if result#
         (nonojure.riv-tests/do-report {:type :pass,
                  :expected '~form, :actual (cons ~pred values#)})
         (nonojure.riv-tests/do-report {:type :fail,
                  :expected '~form, :actual (list '~'not (cons '~pred values#))}))
       result#)))

(defn assert-any
  "Returns generic assertion code for any test, including macros, Java
  method calls, or isolated symbols."
  [form]
  `(let [value# ~form]
     (if value#
       (nonojure.riv-tests/do-report {:type :pass
                :expected '~form, :actual value#})
       (nonojure.riv-tests/do-report {:type :fail
                :expected '~form, :actual value#}))
     value#))

(defmacro try-expr
  [form]
  `(try ~(assert-any form)
        (~'catch js/Error t#
          (nonojure.riv-tests/do-report {:type :error,
                      :expected '~form, :actual t#}))))

(defmacro is
  ([form] `(try-expr ~form)))

(defmacro are
  {:added "1.1"}
  [argv expr & args]
  (if (or
       ;; (are [] true) is meaningless but ok
       (and (empty? argv) (empty? args))
       ;; Catch wrong number of args
       (and (pos? (count argv))
            (pos? (count args))
            (zero? (mod (count args) (count argv)))))
    `(temp/do-template ~argv (is ~expr) ~@args)
    (throw (IllegalArgumentException. "The number of args doesn't match are's argv."))))

(defmacro deftest [name & body]
  `(do
     (defn ~name [] ~@body)
     (nonojure.riv-tests/register-test! '~*cljs-ns* '~name ~name)))

(defmacro testing [text & body]
  `(do
     (nonojure.utils/log ~text)
     ~@body))




