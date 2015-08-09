(ns cells.layout
  (:require [cells.state :as state :refer [layout]]
            [reagent.core :as r]))

(defn new-view!
  ([opts]
   (swap! layout update :views conj
          (r/atom
            (assoc opts :order (inc (count (:views @layout))))))))

(defn mode [k] (get-in @layout [:modes k]))

(defn mode! [k v]
  (swap! layout assoc-in [:modes k] v))
