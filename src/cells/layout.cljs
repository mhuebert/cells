(ns cells.layout
  (:require [cells.state :as state]
            [reagent.core :as r]))

(defn add-cell-view!
  ([id] (add-cell-view! id {}))
  ([id opts]
   (swap! state/layout update :views conj
          (r/atom
            (merge
              {:id     id
               :width  1
               :height 1
               :order  (inc (count (:views @state/layout)))}
              opts)))))