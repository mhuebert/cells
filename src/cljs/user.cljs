(ns cljs.user
  (:require [cells.cell-helpers :as helpers]))

(def ^:dynamic self nil)
(def ^:dynamic *last-val nil)
(def value! helpers/value!)
(def cell! helpers/source!)
(def cell helpers/cell)
(def html helpers/html)
(def prn cljs.core/prn)

