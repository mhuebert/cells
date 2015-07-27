(ns cells.cell-helpers
  (:require-macros [cljs.core.async.macros :refer [go go-loop]]
                   [reagent.ratom :refer [run!]])
  (:require [reagent.core :as r :refer [cursor]]
            [cells.eval :as eval]
            [cljs.core.async :refer [put! chan <! >!]]
            [reagent.ratom :refer [dispose!]]
            [cells.eval]
            [cells.timing :refer [clear-intervals! begin-cell-reaction! self]]
            [cells.state :as state :refer [cell-source cell-values]]))

(def ^:dynamic *suspend-reactions* false)

(declare cell)

(defn alphabet-name []
  (let [char-index-start 97
        char-index-end 123]
    (loop [i char-index-start
           repetitions 1]
      (let [letter (symbol (apply str (repeat repetitions (char i))))]
        (if-not (contains? @cell-source letter) letter
                                     (if (= i char-index-end)
                                       (recur char-index-start (inc repetitions))
                                       (recur (inc i) repetitions)))))))

(defn number-name []
  (inc (count @state/cell-source)))

(defn new-cell
  ([]
   (new-cell (number-name)))
  ([id-or-source]
   (if (string? id-or-source) (new-cell (number-name) id-or-source)
                              (new-cell id-or-source "")))
  ([id source]
   (let [id (if (number? id) (symbol (str state/number-prefix id)) id)]
     (swap! cell-source assoc id (r/atom source))
     (begin-cell-reaction! id))))

(def html #(with-meta % {:hiccup true}))

(defn source!
  ([id val]
   (binding [*suspend-reactions* false]
     (reset! (get @cell-source id) (str val))
     nil)))

(defn value! [id val]
  (eval/set-cell-value! id val)
  (binding [*suspend-reactions* false]
    (swap! cell-values assoc id val))
  val)

(defn interval
  ([f] (interval f 500))
  ([n f]
   (clear-intervals! self)
   (assert (number? n) "interval must be provided with a speed")
   (let [n (max 24 n)
         id self
         exec #(try (binding [*suspend-reactions* true]
                      (let [res (f)]
                        (value! id res)))
                    (catch js/Error e (.log js/console "interval error" self e)))
         interval-id (js/setInterval exec n)]
     (swap! state/index update-in [:interval-ids self] #(conj (or % []) interval-id))
     (binding [*suspend-reactions* true] (f)))))




