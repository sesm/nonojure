(ns nonojure.dialog
  (:require
   [nonojure.navigation :refer [show-view]]
   [dommy.utils :as utils]
   [dommy.core :as dommy])
  (:import goog.dom)
  (:use-macros
   [dommy.macros :only [deftemplate sel1]]))

(deftemplate dialog-template []
  [:div#dialog
   [:div.darkener]
   [:div.content-holder
    [:div.content]]])

(defn create [content]
  (let [dialog (dialog-template)]
    (dommy/append! (sel1 :body) dialog)
    (let [content-div (sel1 dialog :.content)]
     (dommy/append! content-div content)
     (let [child (sel1 dialog ".content > *") ;; horrible. Don't know how to get first
                                              ;; child of .content other way.
           width (str (.-offsetWidth child) "px")
           height (.-offsetHeight child)
           viewport-size (.getViewportSize goog.dom)
           top (str (/ (- (.-height viewport-size) height)
                       2)
                    "px")]
      (dommy/set-style! content-div :width width :top top)
;      (dommy/set-style! content-div :top top)
      ))
    dialog))

(defn close [dialog]
  (dommy/remove! dialog))
