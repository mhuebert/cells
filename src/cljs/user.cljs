(ns cljs.user
  (:require [cells.cell-helpers :as helpers]
            [cells.state :as state]))

(defonce ^:dynamic self nil)
(defonce ^:dynamic self-id nil)

(defonce value helpers/value)
(defonce interval helpers/interval)
(defonce html helpers/html)
(defonce new-cell helpers/new-cell)
(defonce values state/values)




;(def source! helpers/source!)
;(def value! helpers/value!)
