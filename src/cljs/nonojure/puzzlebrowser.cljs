(ns nonojure.puzzlebrowser
  (:require
   [dommy.utils :as utils]
   [dommy.core :as dommy]
   [jayq.core :refer [ajax]])
  (:use-macros
   [dommy.macros :only [node sel sel1 deftemplate]]))

(defn ^:export showalert []
  (let [min-size (.-value (sel1 :#minSize))
        max-size (.-value (sel1 :#maxSize))
        order "asc"]
    (js/alert (str min-size " - " max-size " - " order))))

(defn create-template [width height]
  (repeat height (cons :p (repeat width "."))))

(deftemplate nono-thumbnail [nono]
  (let [width (get nono "width")
        height (get nono "height")
        rating (get nono "rating")
        puzzle-id (get nono "id")]
    [:a {:href (str "/api/nonograms/" puzzle-id)}
     [:p (str width "x" height " - " rating)]
     #_(create-template width height)]))

#_(defn create-thumbnails [nonos]
  (doseq [nono nonos]
    (dommy/append! (sel1 :body) (nono-thumbnail (js->clj nono)))))

(defn create-thumbnails [nonos]
  (dommy/append! (sel1 :body)
                 [:ul
                  (for [nono nonos]
                    [:li (nono-thumbnail (js->clj nono))])]))

(defn ^:export init []
  (let [url "/api/nonograms?filter=size&value=1-10&sort=rating&order=asc"]
    (ajax url {:success create-thumbnails
               :error #(js/alert "Error")})))
