(ns cells.timing
  (:require-macros [cljs.core.async.macros :refer [go go-loop]]
                   [reagent.ratom :refer [run!]])
  (:require [reagent.ratom :refer [dispose! -peek-at]]
            [cells.state :as state]
            [cells.refactor.find :refer [find-reactive-symbols]]
            [cells.compiler :as eval]))

(declare run-cell! compile-cell!)

(defn- update-values [m f & args]
  (reduce (fn [r [k v]] (assoc r k (apply f v args))) {} m))

(defn update-subs! [id]
  (swap! state/dependents update-values #(disj % id))
  (doseq [s (find-reactive-symbols (-> @state/source (get id) deref))]
    (swap! state/dependents update s #(conj (or % #{}) id))))

(defn clear-intervals! [id]
  (doseq [i (get @state/interval-ids id)] (js/clearInterval i))
  (swap! state/interval-ids assoc id []))

(defn run-cell! [id]
  (go
    (clear-intervals! id)
    (<! (eval/eval-def-cell id))
    (update-subs! id)))

(defn compile-cell! [id]
  (go
    (<! (eval/compile-cell-fn id))
    (<! (run-cell! id))))