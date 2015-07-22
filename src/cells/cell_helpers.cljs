(ns cells.cell-helpers
  (:require [reagent.core :as r :refer [cursor]]
            [reagent.ratom :refer [dispose!]]
            [cells.state :refer [cells interval-ids reactions]]))

(defn dispose-reaction! [id]
  (if-let [rxn (get @reactions id)] (dispose! rxn)))

(defn clear-intervals! [id]
  (doall (map js/clearInterval (get @interval-ids id)))
  (swap! interval-ids assoc id []))

(defn new-cell []
  (let [id (inc (count @cells))]
    (swap! cells assoc id {:source ""
                           :value nil
                           :compiled-fn nil
                           :compiled-source nil})
    id))

(defn cell [id]
  (let [cell @(cursor cells [id])]
    (or (:value cell) (:source cell))))

(defn source [id]
  (let [cell @(cursor cells [id])]
    (:source cell)))

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
                (let [res (f)]
                  (if pulse? (do
                               (cell! id res))))
                (catch js/Error e (.log js/console "pulse! error" id e)))
         interval-id (js/setInterval exec n)]
     (swap! interval-ids assoc id (conj (or (get @interval-ids id) []) interval-id))
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

