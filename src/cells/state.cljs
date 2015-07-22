(ns cells.state
  (:require [reagent.core :as r :refer [cursor]]))

(defonce cells (r/atom {
                        1 {:source "Berlin is not so hot today"}
                        2 {:source "(pulse! 500
  #(str \" rgba(\" (rand-int 255) \", \" (rand-int 255) \", \" (rand-int 255) \", \" (rand 1) \") \"))" }
                        3 {:source "(cell! (int (cell 4))
  [:div {:style
    {:background (cell 2)}}
    (cell 5)])"}
                        4 {:source "(pulse! 4000
  #(inc (max 6 (int (self)))))"}
                        5 {:source "(pulse! 1000
  #(clojure.string/join \" \"(shuffle (clojure.string/split (cell 1) \" \" ))))"}



                        }))

(defonce index (r/atom {:outputs {}
                        :interval-ids {}
                        :reactions {}
                        :cell-views []}))

(defonce outputs (cursor index [:outputs]))

(defonce interval-ids (cursor index [:interval-ids]))
(defonce reactions (cursor index [:reactions]))

(defonce cell-views (cursor index [:cell-views]))