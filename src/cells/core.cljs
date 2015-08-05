(ns cells.core
  ^:figwheel-always
  (:require-macros [cljs.core.async.macros :refer [go]])
  (:require
    [cells.events]
    [cells.eval]
    [cells.cell-helpers]
    [cells.compiler :as compiler]
    [cells.state :as state]
    [cells.events :refer [mouse-event!]]
    [cells.keys]
    [cells.components :as c]
    [cells.layout :as layout]
    [cells.cells :as cells]
    [reagent.core :as r]
    [goog.events])
  (:import goog.events.EventType))

(enable-console-print!)

(defonce _
         (go (<! (compiler/load-caches! 'cells.eval))
             (doseq [s state/demo-cells]
               (layout/new-view! (<! (cells/new-cell! (merge {:id (cells/alphabet-name)} s)))
                                      s))
             (<! (compiler/load-caches! 'cells.cell-helpers))
             ))


(defn app []
  (fn []
    [:div
     [c/docs]
     [:div {:key "cells" :class-name "cells"}
      (doall (for [view (:views @state/layout)]
               ^{:key (:id @view)}
               [c/cell view]))
      [c/new-cell-btn]]
     [c/eldoc @state/current-meta]]))

(r/render-component [app] (.getElementById js/document "app"))

(do
  (goog.events/listen js/window
                      (clj->js [goog.events.EventType.MOUSEMOVE
                                goog.events.EventType.MOUSEDOWN
                                goog.events.EventType.MOUSEUP]) mouse-event!))