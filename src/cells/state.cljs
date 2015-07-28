(ns cells.state
  (:require [reagent.core :as r :refer [cursor]]))

(defonce cells (r/atom (sorted-map)))
(defonce values (r/atom {}))

(defonce current-cell (atom nil))
(defonce index (atom {:interval-ids {}
                      :cell-views {}}))

(def number-prefix "")
(def demo-cells ["6"
                 "(+ 3 a)"
                 "(+ a b)"
                 "(fn[x](+ x 1))"
                 "(d 4)"])

(def ^:dynamic self nil)
(def ^:dynamic self-id nil)
