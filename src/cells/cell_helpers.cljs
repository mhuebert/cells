(ns cells.cell-helpers
  (:require [reagent.core :as r :refer [cursor]]
            [reagent.ratom :refer [dispose!]]
            [cells.state :refer [cells interval-ids reactions]]))

(defn dispose-reaction! [id]
  (if-let [rxn (get @reactions id)] (dispose! rxn)))

(defn clear-intervals! [id]
  (prn "clear intervals for" id)
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

(defn cell!
  ([val] (cell! (new-cell) val))
  ([id val]
   (let [value (cursor cells [id :value])]
     (reset! value val))))

(defn interval!
  ([id f] (interval! id f 500))
  ([id f n] (interval! id f n false))
  ([id f n pulse?]
   (clear-intervals! id)
   (assert (number? n) "pulse! must be provided with a speed")
   (let [n (max 24 n)
         interval-f #(try
                      (let [res (f)]
                        (if pulse? (do
                                     (cell! id res))))
                          (catch js/Error e (.log js/console "pulse! error" id e)))

         interval-id (js/setInterval interval-f n)]
     (swap! interval-ids assoc id (conj (or (get @interval-ids id) []) interval-id))
     f)))

(defn pulse!
  [id f n]
  (interval! id f n true))

(defn eval-context [id]
  {:cell      cell
   :cell!     cell!
   :pulse!    (partial pulse! id)
   :interval! (partial interval! id)
   :self!     (partial cell! id)
   :self      (partial cell id)
   })

