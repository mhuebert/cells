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
(def demo-cells [""])
#_(def demo-cells [ "(interval 1000 #(rand))"
                 "(let [window (take 30 (conj (:window self) a))]\n  {:window window \n   :avg (/ (reduce + window) (count window))})"
                 {:width 2 :height 2
                  :source "(html\n [:svg\n  (let [vs (map (partial * 100) (:window b))]\n    (conj\n     (for [[[y1 x1] [y2 x2]] (partition 2 1 (map vector vs (range 0 100 (/ 100 (count vs)))))]\n       [:line {:x1 (str (+ 1 x1) \"%\") :x2 (str (+ 1 x2) \"%\") \n               :y1 (str y1 \"%\") :y2 (str y2 \"%\")\n               :stroke \"blue\" :stroke-width \"2\"}])\n     [:line {:x1 \"0%\" :x2 \"100%\" :y1 (* 100 (:avg b)) :y2 (* 100 (:avg b))\n             :stroke \"pink\" :stroke-width \"1\"}]))])"}])

(def x-unit 224)
(def y-unit 126)
(def gap 30)

(def ^:dynamic self nil)
(def ^:dynamic self-id nil)
