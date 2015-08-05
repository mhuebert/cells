(ns cells.eval
  (:require [cells.cell-helpers :refer [value value! source source! self! new-cell!
                                        slurp interval html md]]
            [cells.cells :as cells]
            [cells.state :as state :refer [self self-id]]))