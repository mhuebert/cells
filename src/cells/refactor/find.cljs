(ns cells.refactor.find
  (:refer-clojure :exclude [find])
  (:require [cells.state :as state]
            [rewrite-clj.zip :as z]
            [rewrite-clj.node :as n]))


(defn has-left-parent [loc s]
  (loop [loc loc]
    (if (nil? loc) false
                   (let [lefty (-> loc z/leftmost z/node n/sexpr)]
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
        f (fn [loc] (and (-> loc z/node n/sexpr symbols)
                       (and (not (has-left-parent loc 'interval))
                            (not (#{'value! 'source!} (some-> loc z/left z/sexpr))))))]
    (find f source)))