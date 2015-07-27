(ns cells.refactor.rename
  (:require [cells.state :as state]
            [rewrite-clj.zip :as z]
            [rewrite-clj.node :as n]))

(defn replace-symbol [source old-symbol new-symbol]
  (let [zipper (z/of-string source)]
    (loop [loc zipper]
      (if (z/end? loc)
        (z/root-string loc)
        (if
          (= old-symbol (z/sexpr loc))
          (recur (z/next (z/replace loc new-symbol)))
          (recur (z/next loc)))))))

(defn rename-symbol [old-symbol new-symbol new-cell-fn]
  (let [all-vars (set (flatten [(keys @state/cell-source)
                                (keys (ns-interns 'cljs.core))
                                (keys (ns-interns 'cljs.user))
                                (keys (ns-interns 'cells.cell-helpers))]))]

    (when (and
            (not= old-symbol new-symbol)
            (not (all-vars new-symbol)))
      (new-cell-fn new-symbol @(get @state/cell-source old-symbol))
      (doseq [[_ src-atom] @state/cell-source]
        (reset! src-atom (replace-symbol @src-atom old-symbol new-symbol)))

      (swap! state/cell-source dissoc old-symbol)
      )))