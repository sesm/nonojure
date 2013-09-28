goog.provide('nonojure.core');
goog.require('cljs.core');
goog.require('dommy.core');
goog.require('dommy.utils');
nonojure.core.quotes = cljs.core.PersistentVector.fromArray(["Hey. I could clear this sky in ten seconds flat","Nopony knows! You know why? Because everypony who's ever come in...has never...come...OUT!","See? I'd never leave my friends hanging!","Time to take out the adorable trash","It needs to be about 20% cooler","I'm... hanging... with the... Wonderbolts!","Danger's my middle name. Rainbow 'Danger' Dash."], true);
nonojure.core.click_handler = (function click_handler(evt){
var msg = cljs.core.rand_nth.call(null,nonojure.core.quotes);
return dommy.core.prepend_BANG_.call(null,document.getElementById("somediv"),cljs.core.PersistentVector.fromArray([new cljs.core.Keyword(null,"div","div",1014003715),msg], true));
});
nonojure.core.init = (function init(){
return dommy.core.listen_BANG_.call(null,document.getElementById("rainbowdash"),new cljs.core.Keyword(null,"click","click",1108654330),nonojure.core.click_handler);
});
nonojure.core.on_load = window.onload = nonojure.core.init;
