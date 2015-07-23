(ns cells.cell-helpers
  (:require [reagent.core :as r :refer [cursor]]
            [reagent.ratom :refer [dispose! -peek-at]]
            [cells.state :refer [cells index]]))

(def ^:dynamic *suspend-reactions* false)

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
  (let [d (if *suspend-reactions* -peek-at deref)
        cell (cursor cells [id])
        {:keys [value source]} (d cell)]
    (if (cljs? source) value (or value source))))

(defn source [id]
  (let [d (if *suspend-reactions* -peek-at deref)
        source (cursor cells [id :source])]
    (d source)))

(defn cell!
  ([val] (cell! (new-cell) val))
  ([id val]
   (let [target (cursor cells [id :value])
         val (if (fn? val) (val (cell id)) val)]
     (reset! target val))))

(defn interval
  ([id f] (interval id f 500))
  ([id n f] (interval id n f false))
  ([id n f pulse?]
   (clear-intervals! id)
   (assert (number? n) "interval must be provided with a speed")
   (let [n (max 24 n)
         exec #(try
                (let [res (binding [*suspend-reactions* true] (f))]
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
   })

