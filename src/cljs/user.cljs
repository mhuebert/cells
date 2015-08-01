(ns cljs.user
  (:require [cells.cell-helpers :as helpers]
            [cells.cells :as cells]
            [cells.state :as state]))

(defonce ^:dynamic self nil)
(defonce ^:dynamic self-id nil)

(defonce value helpers/value)
(defonce interval cells/interval)
(defonce html helpers/html)
(defonce new-cell cells/new-cell!)
(defonce values state/values)
(defonce value! helpers/value!)

;(def source! helpers/source!)

