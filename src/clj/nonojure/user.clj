(ns nonojure.user
  (:require [clojure.core.cache :as ch]
            [ring.middleware.session :refer [wrap-session]]
            [ring.middleware.json :refer [wrap-json-response]]
            [ring.util.response :refer [response]]
            [nonojure.config :refer [config]]
            [cheshire.core :refer [parse-string]]
            [org.httpkit.server :as httpkit]
            [org.httpkit.client :refer [post]]))


(defn wrap-restricted [handler]
  (fn [req]
    (if-let [email (get-in req [:session :email])]
      (handler (assoc req :email email))
      {:status 401
       :body {:result :fail
              :message "not authenticated"}})))

(defn verify-persona-login [assertion]
  (-> (post "https://verifier.login.persona.org/verify"
            {:form-params {:assertion assertion
                           :audience (get-in config [:web :persona-audience])}})
      deref
      :body
      (parse-string true)))

(defn login [req]
  (let [assertion (get-in req [:body :assertion])
        resp (verify-persona-login assertion)]
    (if (= (:status resp) "okay")
      (let [email (:email resp)]
        (-> {:email email
             :result :ok}
            response
            (assoc :session {:email email})))
      {:status 401
       :body {:result :fail
              :message "invalid assertion"}})))

(defn logout [req]
  (assoc (response {:result :ok})
    :session nil))

(defn status [req]
  (response {:result :ok
             :email (get-in req [:session :email])}))


