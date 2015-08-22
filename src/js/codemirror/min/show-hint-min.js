!function(t){"object"==typeof exports&&"object"==typeof module?t(require("../../lib/codemirror")):"function"==typeof define&&define.amd?define(["../../lib/codemirror"],t):t(CodeMirror)}(function(t){"use strict";function i(t,i){this.cm=t,this.options=this.buildOptions(i),this.widget=null,this.debounce=0,this.tick=0,this.startPos=this.cm.getCursor(),this.startLen=this.cm.getLine(this.startPos.line).length;var e=this;t.on("cursorActivity",this.activityFunc=function(){e.cursorActivity()})}function n(t){return"string"==typeof t?t:t.text}function o(t,i){function e(t,e){var o;o="string"!=typeof e?function(t){return e(t,i)}:n.hasOwnProperty(e)?n[e]:e,s[t]=o}var n={Up:function(){i.moveFocus(-1)},Down:function(){i.moveFocus(1)},PageUp:function(){i.moveFocus(-i.menuSize()+1,!0)},PageDown:function(){i.moveFocus(i.menuSize()-1,!0)},Home:function(){i.setFocus(0)},End:function(){i.setFocus(i.length-1)},Enter:i.pick,Tab:i.pick,Esc:i.close},o=t.options.customKeys,s=o?{}:n;if(o)for(var c in o)o.hasOwnProperty(c)&&e(c,o[c]);var h=t.options.extraKeys;if(h)for(var c in h)h.hasOwnProperty(c)&&e(c,h[c]);return s}function s(t,i){for(;i&&i!=t;){if("LI"===i.nodeName.toUpperCase()&&i.parentNode==t)return i;i=i.parentNode}}function c(i,c){this.completion=i,this.data=c,this.picked=!1;var l=this,a=i.cm,u=this.hints=document.createElement("ul");u.className="CodeMirror-hints",this.selectedHint=c.selectedHint||0;for(var f=c.list,d=0;d<f.length;++d){var p=u.appendChild(document.createElement("li")),m=f[d],g=h+(d!=this.selectedHint?"":" "+r);null!=m.className&&(g=m.className+" "+g),p.className=g,m.render?m.render(p,c,m):p.appendChild(document.createTextNode(m.displayText||n(m))),p.hintId=d}var v=a.cursorCoords(i.options.alignWithWord?c.from:null),y=v.left,w=v.bottom,k=!0;u.style.left=y+"px",u.style.top=w+"px";var H=window.innerWidth||Math.max(document.body.offsetWidth,document.documentElement.offsetWidth),C=window.innerHeight||Math.max(document.body.offsetHeight,document.documentElement.offsetHeight);(i.options.container||document.body).appendChild(u);var b=u.getBoundingClientRect(),A=b.bottom-C;if(A>0){var x=b.bottom-b.top,T=v.top-(v.bottom-b.top);if(T-x>0)u.style.top=(w=v.top-x)+"px",k=!1;else if(x>C){u.style.height=C-5+"px",u.style.top=(w=v.bottom-b.top)+"px";var M=a.getCursor();c.from.ch!=M.ch&&(v=a.cursorCoords(M),u.style.left=(y=v.left)+"px",b=u.getBoundingClientRect())}}var F=b.right-H;if(F>0&&(b.right-b.left>H&&(u.style.width=H-5+"px",F-=b.right-b.left-H),u.style.left=(y=v.left-F)+"px"),a.addKeyMap(this.keyMap=o(i,{moveFocus:function(t,i){l.changeActive(l.selectedHint+t,i)},setFocus:function(t){l.changeActive(t)},menuSize:function(){return l.screenAmount()},length:f.length,close:function(){i.close()},pick:function(){l.pick()},data:c})),i.options.closeOnUnfocus){var N;a.on("blur",this.onBlur=function(){N=setTimeout(function(){i.close()},100)}),a.on("focus",this.onFocus=function(){clearTimeout(N)})}var O=a.getScrollInfo();return a.on("scroll",this.onScroll=function(){var t=a.getScrollInfo(),e=a.getWrapperElement().getBoundingClientRect(),n=w+O.top-t.top,o=n-(window.pageYOffset||(document.documentElement||document.body).scrollTop);return k||(o+=u.offsetHeight),o<=e.top||o>=e.bottom?i.close():(u.style.top=n+"px",void(u.style.left=y+O.left-t.left+"px"))}),t.on(u,"dblclick",function(t){var i=s(u,t.target||t.srcElement);i&&null!=i.hintId&&(l.changeActive(i.hintId),l.pick())}),t.on(u,"click",function(t){var e=s(u,t.target||t.srcElement);e&&null!=e.hintId&&(t.preventDefault(),l.changeActive(e.hintId),i.options.completeOnSingleClick&&l.pick())}),t.on(u,"mousedown",function(){e.preventDefault(),setTimeout(function(){a.focus()},20)}),t.signal(c,"select",f[0],u.firstChild),!0}var h="CodeMirror-hint",r="CodeMirror-hint-active";t.showHint=function(t,i,e){if(!i)return t.showHint(e);e&&e.async&&(i.async=!0);var n={hint:i};if(e)for(var o in e)n[o]=e[o];return t.showHint(n)},t.defineExtension("showHint",function(e){if(!(this.listSelections().length>1||this.somethingSelected())){this.state.completionActive&&this.state.completionActive.close();var n=this.state.completionActive=new i(this,e);n.options.hint&&(t.signal(this,"startCompletion",this),n.update(!0))}});var l=window.requestAnimationFrame||function(t){return setTimeout(t,1e3/60)},a=window.cancelAnimationFrame||clearTimeout;i.prototype={close:function(){this.active()&&(this.cm.state.completionActive=null,this.tick=null,this.cm.off("cursorActivity",this.activityFunc),this.widget&&this.data&&t.signal(this.data,"close"),this.widget&&this.widget.close(),t.signal(this.cm,"endCompletion",this.cm))},active:function(){return this.cm.state.completionActive==this},pick:function(i,e){var o=i.list[e];o.hint?o.hint(this.cm,i,o):this.cm.replaceRange(n(o),o.from||i.from,o.to||i.to,"complete"),t.signal(i,"pick",o),this.close()},cursorActivity:function(){this.debounce&&(a(this.debounce),this.debounce=0);var t=this.cm.getCursor(),i=this.cm.getLine(t.line);if(t.line!=this.startPos.line||i.length-t.ch!=this.startLen-this.startPos.ch||t.ch<this.startPos.ch||this.cm.somethingSelected()||t.ch&&this.options.closeCharacters.test(i.charAt(t.ch-1)))this.close();else{var e=this;this.debounce=l(function(){e.update()}),this.widget&&this.widget.disable()}},update:function(t){if(null!=this.tick)if(this.options.hint.async){var i=++this.tick,e=this;this.options.hint(this.cm,function(n){e.tick==i&&e.finishUpdate(n,t)},this.options)}else this.finishUpdate(this.options.hint(this.cm,this.options),t)},finishUpdate:function(i,e){this.data&&t.signal(this.data,"update"),i&&this.data&&t.cmpPos(i.from,this.data.from)&&(i=null),this.data=i;var n=this.widget&&this.widget.picked||e&&this.options.completeSingle;this.widget&&this.widget.close(),i&&i.list.length&&(n&&1==i.list.length?this.pick(i,0):(this.widget=new c(this,i),t.signal(i,"shown")))},buildOptions:function(t){var i=this.cm.options.hintOptions,e={};for(var n in u)e[n]=u[n];if(i)for(var n in i)void 0!==i[n]&&(e[n]=i[n]);if(t)for(var n in t)void 0!==t[n]&&(e[n]=t[n]);return e}},c.prototype={close:function(){if(this.completion.widget==this){this.completion.widget=null,this.hints.parentNode.removeChild(this.hints),this.completion.cm.removeKeyMap(this.keyMap);var t=this.completion.cm;this.completion.options.closeOnUnfocus&&(t.off("blur",this.onBlur),t.off("focus",this.onFocus)),t.off("scroll",this.onScroll)}},disable:function(){this.completion.cm.removeKeyMap(this.keyMap);var t=this;this.keyMap={Enter:function(){t.picked=!0}},this.completion.cm.addKeyMap(this.keyMap)},pick:function(){this.completion.pick(this.data,this.selectedHint)},changeActive:function(i,e){if(i>=this.data.list.length?i=e?this.data.list.length-1:0:0>i&&(i=e?0:this.data.list.length-1),this.selectedHint!=i){var n=this.hints.childNodes[this.selectedHint];n.className=n.className.replace(" "+r,""),n=this.hints.childNodes[this.selectedHint=i],n.className+=" "+r,n.offsetTop<this.hints.scrollTop?this.hints.scrollTop=n.offsetTop-3:n.offsetTop+n.offsetHeight>this.hints.scrollTop+this.hints.clientHeight&&(this.hints.scrollTop=n.offsetTop+n.offsetHeight-this.hints.clientHeight+3),t.signal(this.data,"select",this.data.list[this.selectedHint],n)}},screenAmount:function(){return Math.floor(this.hints.clientHeight/this.hints.firstChild.offsetHeight)||1}},t.registerHelper("hint","auto",function(i,e){var n=i.getHelpers(i.getCursor(),"hint"),o;if(n.length)for(var s=0;s<n.length;s++){var c=n[s](i,e);if(c&&c.list.length)return c}else if(o=i.getHelper(i.getCursor(),"hintWords")){if(o)return t.hint.fromList(i,{words:o})}else if(t.hint.anyword)return t.hint.anyword(i,e)}),t.registerHelper("hint","fromList",function(i,e){var n=i.getCursor(),o=i.getTokenAt(n),s=t.Pos(n.line,o.end);if(o.string&&/\w/.test(o.string[o.string.length-1]))var c=o.string,h=t.Pos(n.line,o.start);else var c="",h=s;for(var r=[],l=0;l<e.words.length;l++){var a=e.words[l];a.slice(0,c.length)==c&&r.push(a)}return r.length?{list:r,from:h,to:s}:void 0}),t.commands.autocomplete=t.showHint;var u={hint:t.hint.auto,completeSingle:!0,alignWithWord:!0,closeCharacters:/[\s()\[\]{};:>,]/,closeOnUnfocus:!0,completeOnSingleClick:!1,container:null,customKeys:null,extraKeys:null};t.defineOption("hintOptions",null)});