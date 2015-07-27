(ns cells.state
  (:require [reagent.core :as r :refer [cursor]]))

(defonce cell-source (r/atom (sorted-map)))

(defonce cell-values (r/atom {}))

(defonce current-cell (atom nil))

(defonce referenced-cells (r/atom #{}))

(def demo-cells ["6"
                 "(+ 3 c1)"
                 "(+ c1 c2)"
                 "(fn[x](+ x 1))"
                 "(c4 4)"
                 "(value! 'c5 \"\\\"blah\\\"\") "])

(defonce index (atom {:interval-ids {}
                      :reactions {}
                      :cell-views {}}))

(def number-prefix "c")




#_(defonce cell-source (r/atom (sorted-map 'hello {:src "\"hello, world\" "}
                                   'shuffle-hello {:src "(cljs.core/reverse hello)"}
                                   'numbers {:src "[1 2 3 4 5 6 7 8 9 0]"}
                                   'shuffle-numbers {:src "(interval 450 #(cell! self (cljs.core/shuffle numbers)))"}
                                   'rand-color {:src "(fn[](str \"rgba(\" (cljs.core/rand-int 255) \",\" (cljs.core/rand-int 255) \",\" (cljs.core/rand-int 255) \",\" (cljs.core/rand-int 100)\")\"))"}
                                   'box {:src "(fn[]
  [:div {:style
       {:position :absolute
        :left (* 20 (cljs.core/nth shuffle-numbers (cljs.core/rand-int 9)))
        :transition \"all 1s ease-out\"
        :bottom 0
        :background (rand-color)
        :width (+ 50 (* 5 (cljs.core/second shuffle-numbers)))
        :height (* 20  (cljs.core/first shuffle-numbers))}}])"}
                                   'boxes {:src "(cljs.core/into (html [:div]) (cljs.core/take 5 (cljs.core/repeatedly box)))"}
                                   'c1 {:src "hey"}

                                   )))



