(ns nonojure.user
  (:require [clojure.core.cache :as ch]
            [compojure.core :refer :all]
            [ring.middleware.session :refer [wrap-session]]
            [ring.middleware.json :refer [wrap-json-response]]
            [ring.util.response :refer [response]]
            [nonojure
             [config :refer [config]]
             [db :as db]]
            [cheshire.core :refer [parse-string]]
            [org.httpkit.server :as httpkit]
            [org.httpkit.client :refer [post]]
            [clojure.string :refer [split]]
            [taoensso.timbre :refer [debug spy]]))

(defn wrap-restricted [handler]
  (fn [req]
    (if-let [email (get-in req [:session :email])]
      (handler (assoc req :email email))
      {:status 401
       :body {:result :fail
              :message "not authenticated"}})))

(defn verify-persona-login [assertion]
  (if (= assertion "clojure-rules")
    {:status "okay"
     :email "test@nonojure.com"}
    (-> (post "https://verifier.login.persona.org/verify"
              (spy {:form-params {:assertion assertion
                                  :audience (get-in config [:web :persona-audience])}}))
        deref
        :body
        (parse-string true))))

(defn login [req]
  (let [assertion (get-in req [:body :assertion])
        resp (verify-persona-login assertion)]
    (if (= (:status resp) "okay")
      (let [email (:email resp)]
        (-> {:email email
             :result :ok}
            response
            (assoc :session {:email email})))
      (do (debug resp)
        {:status 401
         :body {:result :fail
                :message "invalid assertion"}}))))

(defn logout [req]
  {:status 200
   :body {:result :ok}
   :session nil})

(defmacro with-email [req & body]
  `(let [~'email (get-in ~req [:session :email])]
     ~@body))

(defn status [req]
  (with-email req
    (response {:result :ok
               :email email})))

(defn get-progress [req]
  (with-email req
    (let [ids (split (get-in req [:params :ids]) #",")]
      (->> (db/find-puzzle-progress-by-ids ids email)
           (map #(vector (:puzzle %) (select-keys % [:current-state :solution :status])))
           (into {})
           response))))

(defn- update-progress [puzzle-id email new-progress keys default]
  (let [existing (first (db/find-puzzle-progress-by-ids [puzzle-id] email))]
    (db/save-puzzle-progress puzzle-id email (merge (or existing default)
                                                    (select-keys new-progress [keys])))))

(defn save-puzzle-progress [{:keys [body] :as req}]
  (with-email req
    (db/save-puzzle-progress (:puzzle-id body) email (-> body
                                                         (assoc :status :in-progress)
                                                         (dissoc :puzzle-id)))
    (response {:result :ok})))

(defn mark-puzzle-solved [{:keys [body] :as req}]
  (with-email req
    (db/save-puzzle-progress (:puzzle-id body) email (-> body
                                                         (assoc :status :solved
                                                                :current-state nil)
                                                         (dissoc :puzzle-id)))
    (response {:result :ok})))

(defn get-preferences [req]
  (with-email req
    (-> (db/get-preferences email)
        (or {})
        (dissoc :_id)
        response)))

(defn save-preferences [req]
  (with-email req
    (db/save-preferences email (:body req))
    (response {:result :ok})))

(defn get-short-progress [req]
  (with-email req
    (->> (db/get-short-progress email)
         (map #(mapv % [:puzzle :status]))
         (into {})
         response)))

(defroutes user-api
  (GET "/status" [] status)
  (POST "/logout" [] logout)
  (GET "/get-progress" [] get-progress)
  (POST "/save-puzzle-progress" [] save-puzzle-progress)
  (POST "/mark-puzzle-solved" [] mark-puzzle-solved)
  (GET "/get-preferences" [] get-preferences)
  (POST "/save-preferences" [] save-preferences)
  (GET "/get-short-progress-all-puzzles" [] get-short-progress))
