(ns nonojure.core
  (:require
   [compojure.core :refer :all]
   [compojure.handler :as chandler]
   [compojure.route :as croute]
   [org.httpkit.server :as httpkit]
   [ring.util.response :refer [file-response]]
   [cheshire.core :as json]
   [nonojure.db :as db]
   [taoensso.timbre :as timbre
    :refer (trace debug info warn error fatal spy with-log-level)]))

(def config {:port 3000})

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

(defn start []
  (db/connect)
  (let [stop (httpkit/run-server app config)]
    (info (str "Started server on port " (:port config)))
    stop))


#_(
   (def server (start))

   (server))
