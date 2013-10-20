(ns nonojure.navigation
  (:require
   [dommy.core :as dommy
               :refer [append!]]
   [nonojure.utils :refer [log]])
  (:use-macros
   [dommy.macros :only [sel sel1 deftemplate]]))

(def on-show-callbacks (atom {}))

(defn on-show [view callback]
  (swap! on-show-callbacks assoc view callback))

(defn add-view [view tab-text]
  (let [view (name view)
        tab [:div {:id (str view "-tab")
                   :class "tab-button"
                   :data-view-id view}
             tab-text]
        view-div [:div {:id view
                        :class "hidden"}]]
    (append! (sel1 :#navigation) tab)
    (append! (sel1 :#views) view-div)))

(defn- hide-view [tab]
  (let [view (->> (dommy/attr tab :data-view-id)
                  (str "#")
                  sel1)]
    (dommy/remove-class! tab "active")
    (dommy/add-class! view "hidden")))

(defn show-view [view]
  (when-let [active-tab (sel1 :.tab-button.active)]
    (hide-view active-tab))
  (let [view (if (keyword? view) (name view) (str view))]
    (dommy/add-class! (sel1 (str "#" view "-tab")) "active")
    (dommy/remove-class! (sel1 (str "#" view)) "hidden"))
  (when-let [callback (@on-show-callbacks view)]
    (callback)))

(defn- on-tab-click [evt]
  (let [view (.-target evt)]
    (when-not (dommy/has-class? view "active")
      (show-view (keyword (dommy/attr view :data-view-id))))))


(defn ^:export init []
  (add-view :browser "browse")
  (add-view :puzzle "current")
  (show-view :browser)
  (dommy/listen! [(sel1 :#navigation) :.tab-button] :click on-tab-click))
