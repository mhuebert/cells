(ns cells.timing
  (:require-macros [cljs.core.async.macros :refer [go go-loop]]
                   [reagent.ratom :refer [run!]])
  (:require [reagent.ratom :refer [dispose! -peek-at]]
            [cells.state :as state :refer [index cells]]
            [cells.refactor.find :refer [find-reactive-symbols]]
            [cells.eval :as eval]))

(declare run-cell!)

(defn refresh-dependencies [id]
  (doseq [a (vals @state/values)] (remove-watch a id))
  (doseq [s (find-reactive-symbols @(get @state/cells id))]
    (if-not (get @state/values s) (swap! state/values assoc s (atom nil)))
    (add-watch (get @state/values s) id (fn [_ _ old new]
                                          (if-not (= old new)
                                            (run-cell! id))))))

(defn clear-intervals! [id]
  (doall (map js/clearInterval (get-in @index [:interval-ids id])))
  (swap! index assoc-in [:interval-ids id] []))

(defn run-cell! [id]
  (clear-intervals! id)
  (go
    (<! (eval/update-cell-value! id))
    (refresh-dependencies id)))

(defn compile-and-run! [id source]
  (go (<! (eval/compile-cell! id source))
      (run-cell! id)))

(defn begin-cell-reaction! [id]
  (go
    (let [source-atom (get @cells id)]
      (<! (compile-and-run! id @source-atom))
      (add-watch source-atom :handle-source-changes
                 (fn [_ _ old-source new-source]
                   (when (and (not= old-source new-source) (not= id @state/current-cell))
                     (compile-and-run! id new-source)))))))

