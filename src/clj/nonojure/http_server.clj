(ns nonojure.http-server
  (:require
   [compojure.core :refer :all]
   [compojure.handler :as chandler]
   [compojure.route :as croute]
   [org.httpkit.server :as httpkit]
   [ring.util.response :refer [file-response]]
   [cheshire.core :as json]
   [taoensso.timbre :as timbre
    :refer (trace debug info warn error fatal spy with-log-level)]
   [nonojure.lifecycle :as lifecycle]))

(defroutes app-routes
  (GET "/" [] (file-response "resources/landing.html"))
  (croute/resources "/static")
  (croute/not-found "Nothing to see here, move along"))

(defn wrap-logging [handler]
  (fn [req]
    (let [resp (handler req)]
      (debug (str "reg: " (:uri req) " " (:status resp)))
      resp)))

(def app
  (-> (chandler/site #'app-routes)
      (wrap-logging)))

(defrecord HttpNode [config handler stopper]
  lifecycle/Node
  (start [s]
    (let [stopper (httpkit/run-server app config)]
      (assoc s :stopper stopper :handler app)))
  (stop [s]
    ((:stopper s))
    (assoc s :stopper nil)))

(defn prepare [config]
  {:node (->HttpNode config nil nil)
   :children []})
