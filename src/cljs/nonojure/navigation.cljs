(ns nonojure.navigation
  (:require
   [dommy.core :as dommy
               :refer [append!]]
   [nonojure.utils :refer [log]]
   [nonojure.url :refer [go]])
  (:use-macros
   [dommy.macros :only [sel sel1 deftemplate]]))

(def urls (atom {}))

(defn set-url-for-view [view url]
  (swap! urls assoc view url))

(defn add-view [view tab-text url]
  (let [view (name view)
        tab [:div {:id (str view "-tab")
                   :class "tab-button"
                   :data-view-id view}
             tab-text]
        view-div [:div {:id view
                        :class "hidden"}]]
    (append! (sel1 :#navigation) tab)
    (append! (sel1 :#views) view-div))
  (set-url-for-view view url))

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
    (dommy/remove-class! (sel1 (str "#" view)) "hidden")))

(defn- on-tab-click [evt]
  (let [tab (.-target evt)
        view (keyword (dommy/attr tab :data-view-id))]
    (when-not (dommy/has-class? tab "active")
      (when-let [url (@urls view)]
        (go url)
        (show-view view)))))

(defn add-ajax-indicator []
  (append! (sel1 :#navigation) [:img#ajax-indicator
                                {:src "/static/img/ajax-loader.gif"}]))

(defn ^:export init []
  (add-view :browser "browse" "browse")
  (add-view :puzzle "current" "nonogram")
  (add-ajax-indicator)
  (dommy/listen! [(sel1 :#navigation) :.tab-button] :click on-tab-click))
