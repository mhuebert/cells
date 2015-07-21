(ns cells.cell-helpers
  (:require [reagent.core :as r :refer [cursor]]
            [cells.state :refer [cells interval-ids]]))

(declare ^:dynamic *ratom-context*)
(declare ^:dynamic *non-reactive*)

(defn clear-intervals! [id]
  (doall (map js/clearInterval (get @interval-ids id)))
  (swap! interval-ids assoc id []))

(defn new-cell []
  (let [id (inc (count @cells))]
    (swap! cells assoc id {:source ""})
    id))

(defn cell [id]
  (let [cell @(cursor cells [id])]
    (if (= id 3) (prn "id" id "value" (:value cell)))
    (or (:value cell) (:source cell))))

(defn cell!
  ([val] (cell! (new-cell) val))
  ([id val]
   (let [cell (cursor cells [id :value])]
     (reset! cell val))))

(defn interval!
  ([id f] (interval! id f 500))
  ([id f n]
   (clear-intervals! id)
   (assert (number? n) "pulse! must be provided with a speed")
   (let [n (max 24 n)
         interval-f #(try (binding [*non-reactive* true] (f))
                          (catch js/Error e (.log js/console "pulse! error" id e)))
         ;_ (interval-f)
         interval-id (js/setInterval interval-f n)]
     (swap! interval-ids assoc id (conj (or (get @interval-ids id) []) interval-id))
     f)))

(defn pulse!
  [id f n]
  (interval! id #(cell! id (f)) n))

(defn eval-context [id]
  {:cell      cell
   :cell!     cell!
   :pulse!    (partial pulse! id)
   :interval! interval!
   :self!     (partial cell! id)
   :self      (partial cell id)
   })

