(ns nonojure.core
  (:require
   [compojure.core :refer :all]
   [compojure.handler :as chandler]
   [compojure.route :as croute]
   [ring.middleware.json :refer [wrap-json-response]]
   [ring.util.response :refer [response]]
   [org.httpkit.server :as httpkit]
   [ring.util.response :refer [file-response]]
   [cheshire.core :as json]
   [nonojure.db :as db]
   [taoensso.timbre :as timbre
    :refer (trace debug info warn error fatal spy with-log-level)]))

(def config {:port 3000})

(defn- parse-filter-value [value]
  (if value
    (->> (clojure.string/split value #"-")
         (map #(Double/parseDouble %)))
    nil))

(defroutes api
  (GET "/nonograms" [filter value sort order]
       (response (db/find-nonograms {:filter-field (keyword filter)
                                     :filter-value (parse-filter-value value)
                                     :sort-field (keyword sort)
                                     :sort-order (keyword order)})))
  (GET "/nonograms/:id" [id]
       (response (db/find-nonogram-by-id id)))
  (POST "/rate/:id" [id rating]
        (response (db/update-rating id (Integer/parseInt rating)))))

(defn wrap-error [handler]
  (fn [req]
    (try (handler req)
         (catch Exception e
           {:status 500}))))

(defroutes app-routes
  (GET "/" [] (file-response "resources/landing.html"))
  (GET "/browse" [] (file-response "resources/browse.html"))
  (GET "/api-examples" [] (file-response "resources/api-examples.html"))
  (context "/api" [] (-> api
                         (wrap-error)
                         (wrap-json-response {:pretty true})))
  (croute/resources "/static")
  (croute/not-found "Nothing to see here, move along."))

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
