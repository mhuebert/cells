(ns cells.timing
  (:require [reagent.ratom :refer [dispose! -peek-at]]
            [cells.state :refer [index]]))

(defn dispose-reaction! [id]

  (when-let [rxn (get-in @index [:reactions id])]
    (dispose! rxn)))

(defn clear-intervals! [id]
  (doall (map js/clearInterval (get-in @index [:interval-ids id])))
  (swap! index assoc-in [:interval-ids id] []))

(defn clear-function-cell! [id]
  (clear-intervals! id)
  (dispose-reaction! id)
  (swap! index assoc-in [:outputs id] {}))