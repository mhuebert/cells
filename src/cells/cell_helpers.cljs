(ns cells.cell-helpers
  (:require-macros [reagent.ratom :refer [run!]])
  (:require [reagent.core :as r :refer [cursor]]
            [reagent.ratom :refer [dispose! #_-peek-at]]
            [cells.compile :refer [set-cell-value!]]
            [cells.timing :refer [dispose-reaction! clear-intervals! dispose-cell-function!]]
            [cells.state :as state :refer [cell-source cell-values]]))


(def ^:dynamic *suspend-reactions* false)
(def ^:dynamic self nil)

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
     (swap! cell-source assoc id (r/atom source)))))

(defonce _ (doseq [s state/demo-cells] (new-cell s)))

(def html #(with-meta % {:hiccup true}))

(defn source!
  ([id val]
   (binding [*suspend-reactions* false]
     (reset! (get @cell-source id) (str val))
     nil)))

(defn update-cached-value!
  ([id val]
   (binding [*suspend-reactions* false]                     ; other cells can react to all value changes
     (swap! cell-values assoc id val))))

(defn value! [id val]
  (set-cell-value! id val)
  (update-cached-value! id val)
  val)

#_(defn interval
  ([f] (interval f 500))
  ([n f]
   (clear-intervals! self)
   (assert (number? n) "interval must be provided with a speed")
   (let [n (max 24 n)
         exec #(try (binding [*suspend-reactions* true]
                      (let [res (f)]
                        (prn "must implement interval...")
                        ))

                    (catch js/Error e (.log js/console "pulse! error" self e)))
         interval-id (js/setInterval exec n)]
     (binding [*suspend-reactions* true] (exec))
     (swap! state/index update-in [:interval-ids self] #(conj (or % []) interval-id))
     (exec))))




