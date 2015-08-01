(ns cells.cell-helpers
  (:require-macros [cljs.core.async.macros :refer [go go-loop]]
                   [reagent.ratom :refer [run!]])
  (:require [reagent.core :as r :refer [cursor]]
            [goog.net.XhrIo :as xhr]
            [cljs.core.async :refer [put! chan <! >!]]
            [cljs.reader :refer [read-string]]
            [cells.state :as state :refer [sources values]]))

(def html #(with-meta % {:hiccup true}))

(defn value [id]
  @(get @values id))

(defn get-json [url]
  (go
    (let [c (chan)]
      (xhr/send url
                #(let [text (-> % .-target .getResponseText)
                       res (try (-> text read-string :js)
                                (catch js/Error e (.log js/console e)))]
                  (put! c (js->clj res)))
                "GET"
                )
      c)))

; put get-json in user namespace, declare it, try it
#_(interval 500
          (fn []
            (.send goog.net.XhrIo "http://time.jsontest.com"
                   #(cell! 2 (first (vals (js->clj (.getResponseJson (.-target %)))))))))

(defn value! [id val]
  (go
    (reset! (get @values id) val)
    val))

