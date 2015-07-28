(ns cells.refactor.core
  (:refer-clojure :exclude [find])
  (:require [cells.refactor.find :as find]
            [cells.refactor.rename :as rename]))

(def pre-compile
  (comp rename/sugary-derefs))