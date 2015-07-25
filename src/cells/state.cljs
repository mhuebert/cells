(ns cells.state
  (:require [reagent.core :as r :refer [cursor]]))

(defonce cells (r/atom (sorted-map 1 {:source "\"hello\" "}
                                   2 {:source "(cljs.core/reverse @1)"}
                                   3 {:source "[1 2 3 4 5 6 7 8 9 0]"}
                                   4 {:source "(interval 450 #(cell! self (cljs.core/shuffle @3)))"}
                                   5 {:source "(fn[](str \"rgba(\" (cljs.core/rand-int 255) \",\" (cljs.core/rand-int 255) \",\" (cljs.core/rand-int 255) \",\" (cljs.core/rand-int 100)\")\"))"}
                                   6 {:source "(fn[]
  [:div {:style
       {:position :absolute
        :left (* 20 (cljs.core/nth @4 (cljs.core/rand-int 9)))
        :transition \"all 1s ease-out\"
        :bottom 0
        :background (@5)
        :width (+ 50 (* 5 (cljs.core/second @4)))
        :height (* 20  (cljs.core/first @4))}}])"}
                                   7 {:source "(cljs.core/into (html [:div]) (cljs.core/take 5 (cljs.core/repeatedly @6)))"}

                                   )))

(defonce index (atom {:outputs {}
                      :interval-ids {}
                      :reactions {}
                      :cell-views {}}))

(defonce current-cell (atom nil))