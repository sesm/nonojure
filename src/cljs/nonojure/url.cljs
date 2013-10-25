(ns nonojure.url
  (:import goog.history.Html5History
           goog.history.EventType
           goog.Uri)
  (:require [nonojure.pubsub :refer [publish]]
            [nonojure.utils :refer [log]]
            goog.events))

; By default google closure's url changes via history api keep query part.
; So if you call (go "hello?world=1") several times you'll get addres like "hello?world=1?world=1?world=1"
; So implement own transformer based on this
; https://github.com/steida/este-library/blob/master/este/history/tokentransformer.coffee
(deftype QueryAgnosticTransformer []
  Object
  (createUrl [this token path-prefix location]
    (str path-prefix token))
  (retrieveToken [this path-prefix location]
    (str (.substr (.-pathname location) (count path-prefix)) (.-search location))))

(def history (goog.history.Html5History. js/window (QueryAgnosticTransformer.)))

(defn go [url]
  (.setToken history url))

(defn go-overwrite-history [url]
  (.replaceToken history url))

(defn- parse-query [query]
  (into {} (for [key (.getKeys query)]
             [(keyword key) (.get query key)])))

(defn- parse-url [url]
  (let [uri (.parse goog.Uri url)]
    {:path (.getPath uri)
     :query (parse-query (.getQueryData uri))}))

(defn- url-changed [evt]
  (let [url (.-token evt)]
    (publish :url-changed (parse-url url))))

(defn ^:export init []
  ; Don't start all urls with  /#/
  (.setUseFragment history false)

  (.listen goog.events history (.-NAVIGATE goog.history.EventType) url-changed)

  (.setEnabled history true))

