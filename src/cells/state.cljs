(ns cells.state
  (:require [reagent.core :as r]))


;persisted values
(defonce sources (atom {}))
(defonce values (atom {}))
(defonce layout (r/atom {:settings {:x-unit 224
                                    :y-unit 126
                                    :gap    30}
                         :views    (sorted-set-by #(:order @%))}))

;temporary and disposable values
(defonce compiled-fns (r/atom {}))
(defonce dependents (r/atom {}))
(defonce interval-ids (r/atom {}))
(defonce current-cell (atom nil))


(def demo-cells [{:source "(interval 500 #(int (* 10 (rand))))"}
                 {:source "(update self a inc)"}
                 #_{:source "(html\n [:svg {:height \"100%\" :width \"100%\"}\n  (let [ks (keys b)\n        mx (apply max (vals b))]\n    (for [[k offset] (map vector ks (range 0 100 (/ 100 (count ks))))]\n      [:line {:x1 (str offset \"%\")\n              :x2 (str offset \"%\")\n              :y1 (str (- 100 (int (* 100 (/ (get b k) mx)))) \"%\")\n              :y2 \"100%\"\n              :stroke \"blue\" :stroke-width \"5\" :fill \"blue\" }]))])"}
                 ])




