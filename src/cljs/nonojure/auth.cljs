(ns nonojure.auth
  (:require [dommy.core :as dommy]
            [cljs.core.async :refer [chan put! <! >!]]
            [nonojure.utils :refer [log ajax draw-grid]]
            [nonojure.pubsub :refer [publish]]
            [nonojure.dialog :as dialog]
            [nonojure.storage-synchronization :refer [synchronize-storages]]
            [nonojure.storage :as stg])
  (:use-macros
   [dommy.macros :only [sel sel1]]
   [cljs.core.async.macros :only [go]]))

(def dialog-mutex (chan))

(def cell-size 12)

(defn select-puzzle [id a-prog b-prog]
  (let [ch (chan)]
  (go
    (let [value (<! dialog-mutex)
          a-state (:current-state a-prog)
          b-state (:current-state b-prog)
          width (count (first a-state))
          height (count a-state)
          canv-attrs {:width (* (inc width) cell-size)
                      :height (* (inc height) cell-size)}
          template [:div#synchronization-puzzle-select-dialog
                    [:p "Choose puzzle progress to keep on server."]
                    [:div.versions
                     [:div.local.progress
                      [:p "Local version"]
                      [:canvas canv-attrs]]
                     [:div.server.progress
                      [:p "Server version"]
                      [:canvas canv-attrs]]]]
          dlg (dialog/create template)]
      (doseq [[cls state result] [[:.local a-state :a]
                                  [:.server b-state :b]]]
        (draw-grid (sel1 dlg [cls :canvas]) width height state cell-size)
        (dommy/listen! [dlg cls] :click (fn []
                                          (put! ch result)
                                          (dialog/close dlg)
                                          (put! dialog-mutex :free))))))
  ch))

(defn synchronize-local-to-server-storages []
  (synchronize-storages stg/local-storage stg/server-storage
                        #(publish :logged-in) select-puzzle))

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
    (do (set-logged-in-status email)
        (synchronize-local-to-server-storages))
    (do (log "Error on server login")
        (logout))))

(defn on-server-logout [{:keys [result email]}]
  (if (= result "ok")
    (do (set-logged-out-status)
        (publish :logged-out))
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

(defn rand-line [n]
  (vec (repeatedly n #(rand-nth [:filled :crossed :empty]))))

(defn test-puzzle [n m]
  (vec (repeatedly n #(rand-line m))))

(defn ^:export init []
  (put! dialog-mutex :free)
  (ajax "/api/user/status" (fn [{:keys [result email]}]
                             (if (= result "ok")
                               (do (enable-persona email)
                                   (set-logged-in-status email)
                                   (synchronize-local-to-server-storages))
                               (enable-persona nil))))
  (dommy/listen! [(sel1 :.user-area) :#login] :click login)
  (dommy/listen! [(sel1 :.user-area) :#logout] :click logout))
