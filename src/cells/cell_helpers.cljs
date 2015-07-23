(ns cells.cell-helpers
  (:require-macros [reagent.ratom :refer [run!]])
  (:require [reagent.core :as r :refer [cursor]]
            [reagent.ratom :refer [dispose! -peek-at]]
            [cells.state :refer [cells index]]
            [cells.components :as c]))

(def ^:dynamic *suspend-reactions* false)
(def ^:dynamic *deref-cells* false)
(declare cell)
(enable-console-print!)
(aset js/window.cljs.core "_deref"
      (let [deref (aget js/window "cljs" "core" "_deref")]
        (fn [x]
          (if (or (number? x) (keyword? x))
            (cell x) (deref x)))))


(defn cljs? [source]
  (= (first source) \())

(defn dispose-reaction! [id]
  (if-let [rxn (get-in @index [:reactions id])] (dispose! rxn)))

(defn clear-intervals! [id]
  (doall (map js/clearInterval (get-in @index [:interval-ids id])))
  (swap! index assoc-in [:interval-ids id] []))

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
    (if (cljs? source) value (or value source))))



(defn source [id]
  (let [d (if *suspend-reactions* -peek-at cljs.core/deref)
        source (cursor cells [id :source])]
    (d source)))

(defn cell!
  ([val] (cell! (new-cell) val))
  ([id fn-or-val]
   (let [target (cursor cells [id :value])
         val (if (fn? fn-or-val) (fn-or-val (cell id)) fn-or-val)]
     (reset! target val))))


(defn interval
  ([id f] (interval id f 500))
  ([id n f] (interval id n f false))
  ([id n f pulse?]
   (clear-intervals! id)
   (assert (number? n) "interval must be provided with a speed")
   (let [n (max 24 n)
         exec #(try
                (let [res (binding [*suspend-reactions* true
                                    *deref-cells* true] (f))]
                  (if pulse? (do
                               (cell! id res))))
                (catch js/Error e (.log js/console "pulse! error" id e)))
         interval-id (js/setInterval exec n)]
     (exec)
     (swap! index update-in [:interval-ids id] #(conj (or % []) interval-id))
     f)))

(defn pulse!
  [id f n]
  (interval id f n true))

(defn eval-context [id]
  {:cell      cell
   :cell!     cell!
   :pulse!    (partial pulse! id)
   :interval (partial interval id)
   :self!     (partial cell! id)
   :self      (partial cell id)
   :source    source
   :deref cell
   })

(defn clear-function-cell! [id]
  (clear-intervals! id)
  (dispose-reaction! id)
  (swap! index assoc-in [:outputs id] {}))

(defn run-cell! [id f]
  (clear-function-cell! id)
  (try
    (let [context (eval-context id)]
      (swap! index assoc-in [:reactions id] (binding [*deref-cells* true] (run! (f context)))))
    (catch js/Error e (do
                        (.log js/console "run-cell! error" e)
                        (cell! id (c/c-error (str e)))))))
