(ns cells.layout
  (:require [cells.state :as state :refer [layout]]
            [reagent.core :as r]))

(defn new-view!
  ([id] (new-view! id {}))
  ([id opts]
   (swap! layout update :views conj
          (r/atom
            (merge
              {:id     id
               :width  1
               :height 1
               :order  (inc (count (:views @layout)))}
              opts)))))

(defn mode [k] (get-in @layout [:modes k]))

(defn mode! [k v]
  (swap! layout assoc-in [:modes k] v))
