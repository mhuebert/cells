(ns cljs.user
  (:require [cells.cell-helpers :as helpers]))

(def ^:dynamic self)
(def ^:dynamic self-id )


(def value! helpers/value!)
(def source! helpers/source!)
(def interval helpers/interval)
(def html helpers/html)
(def new-cell helpers/new-cell)
