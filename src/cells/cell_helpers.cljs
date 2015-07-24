(ns cells.cell-helpers
  (:require-macros [reagent.ratom :refer [run!]])
  (:require [reagent.core :as r :refer [cursor]]
            [reagent.ratom :refer [dispose! -peek-at]]
            [cells.timing :refer [dispose-reaction! clear-intervals! dispose-cell-function!]]
            [cells.state :as state :refer [cells]]))


(enable-console-print!)


(def ^:dynamic *suspend-reactions* false)

(declare cell)
(aset js/window.cljs.core "_deref"
      (let [deref (aget js/window "cljs" "core" "_deref")]
        (fn [x]
          (if (or (number? x) (keyword? x))
            (cell x) (deref x)))))


(defn source-type [source]
  (cond (#{\(} (first source)) :cljs-expr
        (#{\!} (first source)) :cljs-return
        :else :text))

(defn cell-is [type source]
  (= type (source-type source)))

(defn new-cell []
  (let [id (inc (count @cells))]
    (swap! cells assoc id {:source ""
                           :value nil
                           :compiled-fn nil
                           :compiled-source nil})
    id))

(defn cell [id]
  (let [d (if *suspend-reactions* -peek-at cljs.core/deref)
        cell (cursor cells [id])
        {:keys [value source]} (d cell)]
    (if (#{:cljs-expr :cljs-return} (source-type source)) value (or value source))))

(defn source [id]
  (let [d (if *suspend-reactions* -peek-at cljs.core/deref)
        source (cursor cells [id :source])]
    (d source)))

(defn cell!
  ([id val]
   (binding [*suspend-reactions* false]                     ; other cells can react to all value changes
     (let [target (cursor cells [id :value])]
       (reset! target val)))))

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
   })


