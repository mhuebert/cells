(ns cells.timing
  (:require-macros [cljs.core.async.macros :refer [go go-loop]]
                   [reagent.ratom :refer [run!]])
  (:require [reagent.ratom :refer [dispose! -peek-at]]
            [cells.state :as state :refer [index]]
            [cells.refactor.find :refer [find-reactive-symbols]]
            [cells.compiler :as eval]))

(declare run-cell!)

(defn set-watches! [id]
  (doseq [a (vals @state/values)] (remove-watch a id))
  (doseq [s (find-reactive-symbols (-> @state/cells (get id) deref :source))]
    (if-not (get @state/values s) (swap! state/values assoc s (atom nil)))
    (add-watch (get @state/values s) id (fn [_ _ old new]
                                          (if-not (= old new)
                                            (run-cell! id))))))

(defn clear-intervals! [id]
  (doseq [i (get-in @index [:interval-ids id])] (js/clearInterval i))
  (swap! index assoc-in [:interval-ids id] []))

(defn run-cell! [id]
  (go
    (let [source (-> @state/cells (get id) deref :source)
          compiled-source (or (get-in @state/index [:compiled-source id]) (rand))]
      (clear-intervals! id)
      (when (not= source compiled-source)
        (<! (eval/compile-cell-fn id source)))
      (<! (eval/eval-def-cell id))
      (set-watches! id))))
