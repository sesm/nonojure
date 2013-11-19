(ns nonojure.auth
  (:require [dommy.core :as dommy]
            [nonojure.utils :refer [log ajax]])
  (:use-macros
   [dommy.macros :only [sel sel1]]))

(defn login []
  (.request (.-id window/navigator)))

(defn logout []
  (.logout (.-id window/navigator)))

(defn set-logged-in-status [email]
  (doto (sel1 :.user-area)
    (dommy/clear!)
    (dommy/append! [:p.email email] [:p#logout.button "logout"]))  )

(defn set-logged-out-status []
  (doto (sel1 :.user-area)
    (dommy/clear!)
    (dommy/append! [:p#login.button "login"])))

(defn on-server-login [{:keys [result email]}]
  (if (= result "ok")
    (set-logged-in-status email)
    (do (log "Error on server login")
        (logout))))

(defn on-server-logout [{:keys [result email]}]
  (if (= result "ok")
    (set-logged-out-status)
    (log "Error on server logout")))

(defn on-persona-login [assertion]
  (ajax "/api/user/login" on-server-login :POST {:assertion assertion}))

(defn on-persona-logout []
  (ajax "/api/user/logout" on-server-logout :POST))

(defn enable-persona [current-user]
  (.watch (.-id window/navigator)
          (clj->js {:loggedInUser current-user
                    :onlogin on-persona-login
                    :onlogout on-persona-logout})))

(defn ^:export init []
  (ajax "/api/user/status" (fn [{:keys [result email]}]
                             (if (= result "ok")
                               (do (enable-persona email)
                                   (set-logged-in-status email))
                               (enable-persona nil))))
  (dommy/listen! [(sel1 :.user-area) :#login] :click login)
  (dommy/listen! [(sel1 :.user-area) :#logout] :click logout))
