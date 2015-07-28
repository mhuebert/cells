(ns cells.cell-helpers
  (:require-macros [cljs.core.async.macros :refer [go go-loop]]
                   [reagent.ratom :refer [run!]])
  (:require [reagent.core :as r :refer [cursor]]
            [cells.eval :as eval]
            [goog.net.XhrIo :as xhr]
            [cljs.core.async :refer [put! chan <! >!]]
            [reagent.ratom :refer [dispose!]]
            [cljs.reader :refer [read-string]]
            [cells.eval]
            [cells.timing :refer [clear-intervals! begin-cell-reaction!]]
            [cells.state :as state :refer [cells values]]))

(declare cell)

(defn alphabet-name []
  (let [char-index-start 97
        char-index-end 123]
    (loop [i char-index-start
           repetitions 1]
      (let [letter (symbol (apply str (repeat repetitions (char i))))]
        (if-not (contains? @cells letter) letter
                                     (if (= i char-index-end)
                                       (recur char-index-start (inc repetitions))
                                       (recur (inc i) repetitions)))))))

(defn number-name []
  (inc (count @state/cells)))

(defn new-cell
  ([] (new-cell nil ""))
  ([id source]
   (go
     (let [id (cond
                (empty? id) (alphabet-name)
                (number? id) (symbol (str state/number-prefix id))
                :else id)]
       (swap! cells assoc id (r/atom source))
       (if-not (get values id)
         (swap! values assoc id (r/atom nil)))
       (<! (begin-cell-reaction! id))))))

(def html #(with-meta % {:hiccup true}))

(defn coerce-id [id]
  (if (number? id) (symbol (str state/number-prefix id)) id))

(defn source!
  ([id val]
   (let [id (coerce-id id)]
     (reset! (get @cells id) (str val))
     nil)))

(defn value! [id val]
  (let [id (coerce-id id)]
    (eval/set-cell-value! id val)
    (reset! (get @values id) val)
    val))

(defn get-val [id]
  (aget js/window "cljs" "user" (name id)))

(defn interval
  ([f] (interval f 500))
  ([n f]
   (let [id cljs.user/self-id
         exec #(binding [cljs.user/self (get-val id)]
                (value! id (f cljs.user/self)))]
     (clear-intervals! id)
     (let [interval-id (js/setInterval exec (max 24 n))]
       (swap! state/index update-in [:interval-ids id] #(conj (or % []) interval-id)))
     (f (get-val id)))))


#_(defn get-json [url]
  (let [id self]
    (xhr/send url
              #(let [text (-> % .-target .getResponseText)
                     res (try (-> text read-string :js)
                             (catch js/Error e (.log js/console e)))]
                (value! id (js->clj res)))
              "GET"
              ))
  "...")

; put get-json in user namespace, declare it, try it


