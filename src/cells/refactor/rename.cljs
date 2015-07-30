(ns cells.refactor.rename
  (:require-macros [cljs.core.async.macros :refer [go]])
  (:require [cells.state :as state]
            [cljs.core.async :refer [<!]]
            [reagent.core :as r]
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

#_(defn sugary-derefs [source]
  (find-replace source #(let [sexpr (z/sexpr %)]
                         (and
                           (seq? sexpr)
                           (= 'deref (first sexpr))
                           (number? (second sexpr))))
                #(symbol (str state/number-prefix (second (z/sexpr %))))))

(defn replace-symbol [source old-symbol new-symbol]
  (find-replace source #(= old-symbol (z/sexpr %))
                #(do new-symbol)))

