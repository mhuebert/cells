(ns cells.state
  (:require [reagent.core :as r :refer [cursor]]))


(defonce cells (r/atom (sorted-map 1 {:source "![\"water\" \"apple\" \"melon\" \"fruit\" \"milkshake\" \"berlin\"]"}
                                   2 {:source "(interval 1000
  #(cell! self [:span
 {:style {:font-size 30}}(first (shuffle @1))]))"}
                                   3 {:source "!(fn ;random color
  []
  (str \" rgba(\" (rand-int 255) \", \" (rand-int 255) \", \" (rand-int 255) \", \" (rand 1) \") \"))"}
                                   4 {:source "!(fn ;colored cell
  [color text]
  [:div {:style
        {:background color}}
    text])"}
                                   5 {:source "(interval 1500 #(cell! self
  (inc @self)))"}
                                   6 {:source "!(cell! (max @5 7) (@4 (@3) @2))"}
                                   )))

(defonce index (atom {:outputs {}
                      :interval-ids {}
                      :reactions {}
                      :cell-views {}}))

(defonce current-cell (atom nil))