(ns cells.state
  (:require [reagent.core :as r :refer [cursor]]))

(defonce cells (r/atom (sorted-map 1 {:source "\"hello\" "}
                                   2 {:source "(cljs.core/reverse @1)"}
                                   3 {:source "[1 2 3 4 5 6 7 8 9 0]"}
                                   4 {:source "(interval 250 #(cell! self (cljs.core/shuffle @3)))"}
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
#_(defonce cells (r/atom (sorted-map 1 {:source "[\"water\" \"apple\" \"melon\" \"fruit\" \"milkshake\" \"berlin\"]"}
                                   2 {:source "(interval 1000
  #(cell! self [:span
 {:style {:font-size 30}}(first (shuffle @1))]))"}
                                   3 {:source "(fn ;random color
  []
  (str \" rgba(\" (rand-int 255) \", \" (rand-int 255) \", \" (rand-int 255) \", \" (rand 1) \") \"))"}
                                   4 {:source "(fn ;colored cell
  [color text]
  (html [:div {:style
        {:background color}}
    text]))"}
                                   5 {:source "(interval 1500 #(cell! self
  (inc @self)))"}
                                   6 {:source "(cell! (max @5 7) (@4 (@3) @2))"}
                                   )))

(defonce index (atom {:outputs {}
                      :interval-ids {}
                      :reactions {}
                      :cell-views {}}))

(defonce current-cell (atom nil))