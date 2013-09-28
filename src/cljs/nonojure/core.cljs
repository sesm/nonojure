(ns nonojure.core
  (:require
   [dommy.utils :as utils]
   [dommy.core :as dommy])
  (:use-macros
   [dommy.macros :only [node sel sel1]]))

(def quotes  ["Hey. I could clear this sky in ten seconds flat"
              "Nopony knows! You know why? Because everypony who's ever come in...has never...come...OUT!"
              "See? I'd never leave my friends hanging!"
              "Time to take out the adorable trash"
              "It needs to be about 20% cooler"
              "I'm... hanging... with the... Wonderbolts!"
              "Danger's my middle name. Rainbow 'Danger' Dash."])

(defn click-handler [evt]
  (let [msg (rand-nth quotes)]
    (dommy/prepend! (sel1 :#somediv) [:div msg])))

(defn init []
  (dommy/listen! (sel1 :#rainbowdash) :click click-handler))

(def on-load
  (set! (.-onload js/window) init))


