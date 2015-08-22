(ns cells.eval
  (:require [cells.cell-helpers :refer [value value! source source! self! new-cell!
                                        slurp interval ->html md html d3-svg]]
            [cells.cells :as cells]
            [goog.net.XhrIo :as xhr]
            [cljs.tools.reader :as reader]
            [cells.state :as state :refer [self self-id]]))

