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


(def demo-cells ["(interval 400 inc)"
                 "(+ a 1)"
                 ])




