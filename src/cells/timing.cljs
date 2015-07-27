(ns cells.timing
  (:require-macros [cljs.core.async.macros :refer [go go-loop]]
                   [reagent.ratom :refer [run!]])
  (:require [reagent.ratom :refer [dispose! -peek-at]]
            [cells.state :as state :refer [index cell-source]]
            [cells.refactor.find :refer [find-reactive-symbols]]
            [reagent.core :as r]

            [cells.eval :as eval]
            ))

(def ^:dynamic self nil)

(defn dispose-reaction! [id]
  (when-let [rxn (get-in @index [:reactions id])]
    (dispose! rxn)))

(defn clear-intervals! [id]
  (doall (map js/clearInterval (get-in @index [:interval-ids id])))
  (swap! index assoc-in [:interval-ids id] []))

(defn deref-reactive-symbols! [id]

  (try (if-let [source-atom (get state/cell-source id)]
         (doseq [s (find-reactive-symbols @source-atom)]
           @(r/cursor state/cell-values [s])))
       (catch js/Error e (.log js/console "deref error" id e))))

(defn run-cell! [id]
  (clear-intervals! id)
  (dispose-reaction! id)
  (try (let
         [reaction (run!
                     (deref-reactive-symbols! id)
                     (binding [self id]
                       (eval/update-cell-value! id)))]
         (swap! index assoc-in [:reactions id] reaction))
       (catch js/Error e (.log js/console "run-cell! error" e))))

(defn compile-and-run! [id source]
  (go (<! (eval/compile-cell! id source))
      (run-cell! id)))

(defn begin-cell-reaction! [id]
  (let [source-atom (get @cell-source id)]
    (compile-and-run! id @source-atom)
    (add-watch source-atom :handle-source-changes
               (fn [_ _ old-source new-source]
                 (when (and (not= old-source new-source) (not= id @state/current-cell))
                   (compile-and-run! id new-source))))))

