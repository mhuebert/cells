(ns cells.state
  (:require [reagent.core :as r :refer [cursor]]))

(defonce cells (r/atom (sorted-map)))
(defonce values (r/atom {}))
(defonce cell-order (r/atom []))

(defonce current-cell (atom nil))
(defonce index (atom {:interval-ids {}
                      :cell-views {}
                      :compiled-source {}}))

(def number-prefix "")
(def demo-cells [{:source "(interval 100 #(apply str (take (+ (count self) (- (rand-int 7) 3)) (repeat \".\"))))"
                  :initial-val "................................................................."}])

(def x-unit 224)
(def y-unit 126)
(def gap 30)

(def ^:dynamic self nil)
(def ^:dynamic self-id nil)
