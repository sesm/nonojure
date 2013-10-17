(ns nonojure.dialog
  (:require
   [nonojure.navigation :refer [show-view]]
   [dommy.utils :as utils]
   [dommy.core :as dommy]
   [jayq.util :refer [log]])
  (:use-macros
   [dommy.macros :only [deftemplate sel1]]))

(deftemplate dialog-template []
  [:div#dialog
   [:div.darkener]
   [:div.content-holder
    [:div.floater]
    [:div.content]]])

(defn create [content]
  (let [dialog (dialog-template)]
    (dommy/append! (sel1 :body) dialog)
    (let [content-div (sel1 dialog :.content)]
     (dommy/append! content-div content)
     (let [child (sel1 dialog ".content > *") ;; horrible. Don't know how to get first
                                              ;; child of .content other way.
           width (str (.-offsetWidth child) "px")]
      (dommy/set-style! content-div :width width)))
    dialog))

(defn close [dialog]
  (dommy/remove! dialog))
