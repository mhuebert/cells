(ns cells.cell-helpers
  (:require-macros [cljs.core.async.macros :refer [go go-loop]]
                   [reagent.ratom :refer [run!]])
  (:require [reagent.core :as r :refer [cursor]]
            [cells.compiler :as eval]
            [goog.net.XhrIo :as xhr]
            [cljs.core.async :refer [put! chan <! >!]]
            [reagent.ratom :refer [dispose!]]
            [cljs.reader :refer [read-string]]
            [cells.compiler]
            [cells.timing :refer [clear-intervals! run-cell!]]
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
  ([] (new-cell (alphabet-name) ""))
  ([source] (new-cell (alphabet-name) source))
  ([id source]
   (go
     (let [id (cond
                (number? id) (symbol (str state/number-prefix id))
                (nil? id) (alphabet-name)
                (get @cells id) (alphabet-name)
                :else id)]
       (swap! cells assoc id (r/atom source))
       (if-not (get values id) ;may already exist if another cell is listening
         (swap! values assoc id (r/atom nil)))
       (<! (run-cell! id))
       id))))

(def html #(with-meta % {:hiccup true}))

(defn value [id]
  @(get @values id))

(defn coerce-id [id]
  (if (number? id) (symbol (str state/number-prefix id)) id))


(defn get-val [id]
  (aget js/window "cljs" "user" (name id)))



(defn interval
  ([f] (interval f 500))
  ([n f]
   (let [id cljs.user/self-id
         exec #(binding [cljs.user/self (get-val id)]
                (reset! (get @values id) (f cljs.user/self))
                (eval/def-cell id))]
     (clear-intervals! id)
     (let [interval-id (js/setInterval exec (max 24 n))]
       (swap! state/index update-in [:interval-ids id] #(conj (or % []) interval-id)))
     (f (get-val id)))))


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

#_(defn source!
    ([id val]
     (let [id (coerce-id id)]
       (reset! (get @cells id) (str val))
       nil)))

#_(defn value! [id val]
    (go (let [id (coerce-id id)]
          (reset! (get @values id) val)
          (<! (eval/def-cell id))
          val)))

