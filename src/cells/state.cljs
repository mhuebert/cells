(ns cells.state
  (:require [reagent.core :as r :refer [cursor]]))



(defonce cells (r/atom {
                        1 {:source "water apple melon fruit milkshake berlin"}
                        2 {:source "(pulse! 1000
  #(clojure.string/join \" \"(shuffle (clojure.string/split (cell 1) \" \" ))))"}
                        3 {:source "(pulse! 200
  #(str \" rgba(\" (rand-int 255) \", \" (rand-int 255) \", \" (rand-int 255) \", \" (rand 1) \") \"))" }
                        4 {:source "(let [c (int (cell 5))
         c (max c 6)]
(cell! c
[:div {:style
    {:background (cell 3)}}
    (cell 2)]))"}
                        5 {:source "(pulse! 1500
  #(inc(self)))"}
                        6 {:source ""}
                        }))

(defonce index (atom {:outputs {}
                        :interval-ids {}
                        :reactions {}
                        :cell-views []}))

