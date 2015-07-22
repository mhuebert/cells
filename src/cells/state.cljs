(ns cells.state
  (:require [reagent.core :as r :refer [cursor]]))

(defonce cells (r/atom {1 {:source "it was that other time,"}
                        2 {:source "(pulse!
  #(str \" rgba(\" (rand-int 255) \", \" (rand-int 255) \", \" (rand-int 255) \", \" (rand 1) \") \") 100)" }
                        3 {:source "(interval!
#(cell! (max 5 (int (cell 4)))
  [:div {:style
    {:background (cell 2)}}
    (cell 1)]) 50)"}
                        4 {:source "(pulse! #(inc (max 5 (int (self)))) 4000)"}



                        }))

(defonce outputs (r/atom {}))

(defonce interval-ids (atom {}))
(defonce reactions (atom {}))

(defonce cell-views (r/atom []))