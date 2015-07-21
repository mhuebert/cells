(ns cells.state
  (:require [reagent.core :as r :refer [cursor]]))

(defonce cells (r/atom {1 {:source "She was there when I"}
                        2 {:source "(pulse!
  #(str \" rgba(\" (rand-int 255) \", \" (rand-int 255) \", \" (rand-int 255) \", \" (rand 1) \") \") 700)" }
                        3 {:source "(cell! (max 5 (int (cell 4)))
  [:div {:style
    {:background (cell 2)}}
    (cell 1)])"}
                        4 {:source "(pulse! #(inc (max 5 (int (self)))) 3000)"}



                        }))

(defonce interval-ids (atom {}))

(defonce cell-views (r/atom []))