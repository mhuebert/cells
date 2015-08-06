(ns cells.state
  (:require [reagent.core :as r]))

(def ^:dynamic self nil)
(def ^:dynamic self-id nil)

;user values
(defonce sources (atom {}))
(defonce layout (r/atom {:settings {:x-unit 224
                                    :y-unit 126
                                    :gap    30}
                         :modes {:show-all-source false}
                         :views    (sorted-set-by #(:order @%))}))

;derived values
(defonce values (atom {}))
(defonce compiled-fns (r/atom {}))
(defonce dependents (r/atom {}))
(defonce interval-ids (r/atom {}))
(defonce current-cell (atom nil))


(defonce current-meta (r/atom {}))

(def demo-cells [{:id 'b
                  :source "(slurp :text \"https://gist.githubusercontent.com/jackrusher/1b9d3782cedec26f5b51/raw/213738c97ef33969f8ddaa49f2a97d48993e88f2/dylan-thomas.txt\")"}
                 {:id 'masseuse-pony
                  :source "(reduce #(update-in %1 %2 inc) self (partition 2 1 (remove (partial = \"\") (js->clj (.split (.toLowerCase  b) (re-pattern \"[^a-z]\"))))))"}
                 {:id 'rose
                  :source "(interval 3000 (fn [] (md(let [choose #(loop [kvs %1 n (* (rand) (reduce + (vals %1)))]\n                  (let [[k v] (first kvs) n (- n v)]\n                    (if (or (<= n 0)) k (recur (rest kvs) n))))]\n    (loop [n 12 curr (rand-nth (keys masseuse-pony)) out \"\"]\n      (if (<= n 0) out\n          (let [out (str out \" \" curr)]\n            (recur (dec n) (choose (masseuse-pony curr)) out))))))))"}
                 ])




