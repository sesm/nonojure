goog.provide('nonojure.api_examples');
goog.require('cljs.core');
goog.require('jayq.util');
goog.require('clojure.string');
goog.require('jayq.core');
goog.require('clojure.string');
goog.require('jayq.core');
goog.require('jayq.util');
goog.require('dommy.core');
nonojure.api_examples.requests = cljs.core.PersistentVector.fromArray([cljs.core.PersistentVector.fromArray(["Browse puzzles",new cljs.core.Keyword(null,"GET","GET",1013974696),"/api/nonograms?filter=size&value=1-10&sort=rating&order=asc"], true),cljs.core.PersistentVector.fromArray(["Get puzzle by id",new cljs.core.Keyword(null,"GET","GET",1013974696),"/api/nonograms/PUZZLE_ID"], true),cljs.core.PersistentVector.fromArray(["Rate puzzle",new cljs.core.Keyword(null,"POST","POST",1016366098),"/api/rate/PUZZLE_ID?rating=5"], true)], true);
nonojure.api_examples.request_node = (function request_node(title,method,example){var dom7381 = document.createElement("div");dom7381.className = "request";
dom7381.appendChild((function (){var dom7382 = document.createElement("p");dom7382.className = "title";
dom7382.appendChild(dommy.template.__GT_node_like.call(null,title));
return dom7382;
})());
dom7381.appendChild((function (){var dom7383 = document.createElement("div");dom7383.appendChild((function (){var dom7384 = document.createElement("p");dom7384.className = "method";
dom7384.appendChild(dommy.template.__GT_node_like.call(null,cljs.core.name.call(null,method)));
return dom7384;
})());
dom7383.appendChild((function (){var dom7385 = document.createElement("input");dom7385.className = "url";
if(cljs.core.truth_(example))
{dom7385.setAttribute("value",example);
} else
{}
return dom7385;
})());
dom7383.appendChild((function (){var dom7386 = document.createElement("button");dom7386.appendChild(document.createTextNode("Send"));
return dom7386;
})());
return dom7383;
})());
dom7381.appendChild((function (){var dom7387 = document.createElement("textarea");dom7387.className = "result";
if("readonly")
{dom7387.setAttribute("readonly","readonly");
} else
{}
return dom7387;
})());
return dom7381;
});
nonojure.api_examples.update_result = (function update_result(holder,data){dommy.core.set_value_BANG_.call(null,holder,JSON.stringify(data,null,"  "));
dommy.core.set_style_BANG_.call(null,holder,new cljs.core.Keyword(null,"height","height",4087841945),"0px");
var real_height = [cljs.core.str(holder.scrollHeight),cljs.core.str("px")].join('');return dommy.core.set_style_BANG_.call(null,holder,new cljs.core.Keyword(null,"height","height",4087841945),real_height);
});
nonojure.api_examples.send_request = (function send_request(event){var button = event.selectedTarget;var request_node = dommy.core.closest.call(null,button,new cljs.core.Keyword(null,".request",".request",674493971));var method = dommy.core.text.call(null,(dommy.utils.__GT_Array.call(null,dommy.template.__GT_node_like.call(null,request_node).getElementsByClassName("method"))[0]));var url = dommy.core.value.call(null,(dommy.utils.__GT_Array.call(null,dommy.template.__GT_node_like.call(null,request_node).getElementsByClassName("url"))[0]));var result_holder = (dommy.utils.__GT_Array.call(null,dommy.template.__GT_node_like.call(null,request_node).getElementsByClassName("result"))[0]);jayq.core.ajax.call(null,url,cljs.core.PersistentArrayMap.fromArray([new cljs.core.Keyword(null,"type","type",1017479852),method,new cljs.core.Keyword(null,"success","success",3441701749),(function (p1__7388_SHARP_){return nonojure.api_examples.update_result.call(null,result_holder,p1__7388_SHARP_);
}),new cljs.core.Keyword(null,"error","error",1110689146),(function (p1__7389_SHARP_){return nonojure.api_examples.update_result.call(null,result_holder,cljs.core.clj__GT_js.call(null,cljs.core.PersistentArrayMap.fromArray([new cljs.core.Keyword(null,"result","result",4374444943),"Bad request",new cljs.core.Keyword(null,"xhr","xhr",1014022900),p1__7389_SHARP_], true)));
})], true));
return jayq.util.log.call(null,[cljs.core.str(method),cljs.core.str(" "),cljs.core.str(url)].join(''));
});
nonojure.api_examples.init = (function init(){var seq__7394_7398 = cljs.core.seq.call(null,nonojure.api_examples.requests);var chunk__7395_7399 = null;var count__7396_7400 = 0;var i__7397_7401 = 0;while(true){
if((i__7397_7401 < count__7396_7400))
{var request_7402 = cljs.core._nth.call(null,chunk__7395_7399,i__7397_7401);dommy.core.append_BANG_.call(null,document.getElementById("content"),cljs.core.apply.call(null,nonojure.api_examples.request_node,request_7402));
{
var G__7403 = seq__7394_7398;
var G__7404 = chunk__7395_7399;
var G__7405 = count__7396_7400;
var G__7406 = (i__7397_7401 + 1);
seq__7394_7398 = G__7403;
chunk__7395_7399 = G__7404;
count__7396_7400 = G__7405;
i__7397_7401 = G__7406;
continue;
}
} else
{var temp__4092__auto___7407 = cljs.core.seq.call(null,seq__7394_7398);if(temp__4092__auto___7407)
{var seq__7394_7408__$1 = temp__4092__auto___7407;if(cljs.core.chunked_seq_QMARK_.call(null,seq__7394_7408__$1))
{var c__3599__auto___7409 = cljs.core.chunk_first.call(null,seq__7394_7408__$1);{
var G__7410 = cljs.core.chunk_rest.call(null,seq__7394_7408__$1);
var G__7411 = c__3599__auto___7409;
var G__7412 = cljs.core.count.call(null,c__3599__auto___7409);
var G__7413 = 0;
seq__7394_7398 = G__7410;
chunk__7395_7399 = G__7411;
count__7396_7400 = G__7412;
i__7397_7401 = G__7413;
continue;
}
} else
{var request_7414 = cljs.core.first.call(null,seq__7394_7408__$1);dommy.core.append_BANG_.call(null,document.getElementById("content"),cljs.core.apply.call(null,nonojure.api_examples.request_node,request_7414));
{
var G__7415 = cljs.core.next.call(null,seq__7394_7408__$1);
var G__7416 = null;
var G__7417 = 0;
var G__7418 = 0;
seq__7394_7398 = G__7415;
chunk__7395_7399 = G__7416;
count__7396_7400 = G__7417;
i__7397_7401 = G__7418;
continue;
}
}
} else
{}
}
break;
}
return dommy.core.listen_BANG_.call(null,cljs.core.PersistentVector.fromArray([document.getElementById("content"),new cljs.core.Keyword(null,"button","button",3931183780)], true),new cljs.core.Keyword(null,"click","click",1108654330),nonojure.api_examples.send_request);
});
goog.exportSymbol('nonojure.api_examples.init', nonojure.api_examples.init);
