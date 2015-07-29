(ns cells.refactor.rename
  (:require [cells.state :as state]
            [rewrite-clj.zip :as z]
            [rewrite-clj.node :as n]))

(defn find-replace [source pred replacement]
  (let [zipper (z/of-string source)]
    (loop [loc zipper]
      (if (z/end? loc)
        (z/root-string loc)
        (if
          (pred loc)
          (recur (z/next (z/replace loc (replacement loc))))
          (recur (z/next loc)))))))

(defn sugary-derefs [source]
  (find-replace source #(let [sexpr (z/sexpr %)]
                         (and
                           (seq? sexpr)
                           (= 'deref (first sexpr))
                           (number? (second sexpr))))
                #(symbol (str state/number-prefix (second (z/sexpr %))))))

(defn replace-symbol [source old-symbol new-symbol]
  (find-replace source #(= old-symbol (z/sexpr %))
                #(do new-symbol)))

(defn rename-symbol [old-symbol new-symbol new-cell-fn kill-cell-fn]
  (let [all-vars (set (flatten [(keys @state/cells)
                                (keys (ns-interns 'cljs.core))
                                (keys (ns-interns 'cljs.user))
                                (keys (ns-interns 'cells.cell-helpers))]))]

    (prn old-symbol new-symbol (= old-symbol new-symbol))

    (when (and
            (not= old-symbol new-symbol)
            (not (all-vars new-symbol)))
      (new-cell-fn new-symbol @(get @state/cells old-symbol))
      (doseq [[_ src-atom] @state/cells]
        (swap! src-atom update :source #(replace-symbol % old-symbol new-symbol)))

      ; kill cell
      )))