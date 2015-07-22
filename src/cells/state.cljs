(ns cells.state
  (:require [reagent.core :as r :refer [cursor]]))

(defonce cells (r/atom {
                        1 {:source "Berlin is not so hot today"}
                        2 {:source "(pulse!
  #(str \" rgba(\" (rand-int 255) \", \" (rand-int 255) \", \" (rand-int 255) \", \" (rand 1) \") \") 100)" }
                        3 {:source "(cell! (int (cell 4))
  [:div {:style
    {:background (cell 2)}}
    (cell 5)])"}
                        4 {:source "(pulse! #(inc (max 6 (int (self)))) 4000)"}
                        5 {:source "(pulse! #(clojure.string/join \" \"(shuffle (clojure.string/split (cell 1) \" \" ))) 500)"}



                        }))

(defonce index (r/atom {:outputs {}
                        :interval-ids {}
                        :reactions {}
                        :cell-views []}))

(defonce outputs (cursor index [:outputs]))

(defonce interval-ids (cursor index [:interval-ids]))
(defonce reactions (cursor index [:reactions]))

(defonce cell-views (cursor index [:cell-views]))