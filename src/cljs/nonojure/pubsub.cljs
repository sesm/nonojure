(ns nonojure.pubsub
  (:import goog.pubsub.PubSub)
  (:require [nonojure.utils :refer [log]]))

(def bus (goog.pubsub.PubSub.))

(defn subscribe [topic handler]
  (.subscribe bus (name topic) handler))

(defn subscribe-once [topic handler]
  (.subscribeOnce bus (name topic) handler))

(defn unsubscribe [topic handler]
  (.unsubscribe bus (name topic) handler))

(defn publish
  ([topic data]
     (log "publish" topic)
     (.publish bus (name topic) data))
  ([topic]
     (log "publish" topic)
     (.publish bus (name topic))))
