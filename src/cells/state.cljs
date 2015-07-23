(ns cells.state
  (:require [reagent.core :as r :refer [cursor]]))

(defonce cells (r/atom (sorted-map 1 {:source "water apple melon fruit milkshake berlin"}
                                   2 {:source "(interval 1000 #(cell! self
  (clojure.string/join \" \"(shuffle (clojure.string/split @1 \" \" )))))"}
                                   3 {:source "(interval 200 #(cell! self
  (str \" rgba(\" (rand-int 255) \", \" (rand-int 255) \", \" (rand-int 255) \", \" (rand 1) \") \")))"}
                                   4 {:source "(let
  [c (max (int @5) 6)]
(cell! c
[:div {:style
      {:background @3}}
    @2]))"}
                                   5 {:source "(interval 1500 #(cell! self
  inc))"})))

(defonce index (atom {:outputs {}
                      :interval-ids {}
                      :reactions {}
                      :cell-views {}}))

(defonce current-cell (atom nil))