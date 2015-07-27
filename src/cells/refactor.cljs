(ns cells.refactor
  (:refer-clojure :exclude [find])
  (:require [cells.state :as state]
            [cells.cell-helpers :refer [new-cell]]
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

(defn rename-symbol [old-symbol new-symbol]
  (let [all-vars (set (flatten [(keys @state/cell-source)
                                (keys (ns-interns 'cljs.core))
                                (keys (ns-interns 'cljs.user))
                                (keys (ns-interns 'cells.cell-helpers))]))]

    (when (and
            (not= old-symbol new-symbol)
            (not (all-vars new-symbol)))
      (new-cell new-symbol @(get @state/cell-source old-symbol))

      (js/setTimeout (fn []
                       (doseq [[_ src-atom] @state/cell-source]
                         (reset! src-atom (replace-symbol @src-atom old-symbol new-symbol)))

                       (swap! state/cell-source dissoc old-symbol))
                     100))))


(defn has-parent [loc s]
  (prn "has -")
  (loop [loc loc]
    (if (nil? loc) false
                   (let [lefty (-> loc z/leftmost z/node n/sexpr)]
                     (prn "lefty" lefty)
                     (if (= lefty s)
                       true
                       (recur (z/up loc)))))))

(defn find [f source]
  (let [zipper (z/of-string source)
        result (atom [])]
    (loop [loc zipper]
      (if (z/end? loc) (z/root-string loc)
                       (do (if (f loc) (swap! result conj (z/sexpr loc)))
                           (recur (z/next loc)))))
    @result))

(defn find-reactive-symbols [source]
  (let [symbols (set (keys @state/cell-source))
        f (fn [x] (and (-> x z/node n/sexpr symbols)
                      (not (has-parent x 'interval))))]
    (find f source)))