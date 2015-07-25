(ns cells.cell-helpers
  (:require-macros [reagent.ratom :refer [run!]])
  (:require [reagent.core :as r :refer [cursor]]
            [reagent.ratom :refer [dispose! -peek-at]]
            [cells.timing :refer [dispose-reaction! clear-intervals! dispose-cell-function!]]
            [cells.state :as state :refer [cells]]))


(def ^:dynamic *suspend-reactions* false)

(declare cell)

(aset js/window.cljs.core "deref"
      (let [deref (aget js/window "cljs" "core" "deref")]
        (fn [x]
          (if (or (number? x) (keyword? x))
            (do
              (cell x)) (deref x)))))

(defn new-cell []
  (let [id (inc (count @cells))]
    (swap! cells assoc id {:source ""})
    id))

(defn cell-attr [field id]
  (let [d (if *suspend-reactions* -peek-at cljs.core/deref)
        value (cursor cells [id field])]
    (d value)))

(def cell (partial cell-attr :value))
(def source (partial cell-attr :source))

(defn cell!
  ([id fn-or-val]
   (binding [*suspend-reactions* false]                     ; other cells can react to all value changes
     (let [target (cursor cells [id :value])]
       (reset! target fn-or-val)))))

(defn interval
  ([id f] (interval id f 500))
  ([id n f ]
   (clear-intervals! id)
   (assert (number? n) "interval must be provided with a speed")
   (let [n (max 24 n)
         exec #(try (binding [*suspend-reactions* true] (f))

                    (catch js/Error e (.log js/console "pulse! error" id e)))
         interval-id (js/setInterval exec n)]
     (binding [*suspend-reactions* true] (exec))
     (swap! state/index update-in [:interval-ids id] #(conj (or % []) interval-id))
     (exec))))

(defn eval-context [id]
  {:cell!    cell!
   :interval (partial interval id)
   :self     id
   :source   source
   :html   #(with-meta % {:hiccup true})
   })


