(ns cells.core
  ^:figwheel-always
  (:require-macros [cljs.core.async.macros :refer [go]])
  (:require
    [cells.events]
    [cells.eval]
    [cells.cell-helpers]
    [cells.routes]
    [cells.compiler :as compiler]
    [cells.state :as state]
    [cells.events :refer [mouse-event!]]
    [cells.keys]
    [cells.components :as c]
    [cells.layout :as layout]
    [cells.cells :as cells]
    [reagent.core :as r]
    [goog.events]
    [cljsjs.d3]
    [cljsjs.topojson])
  (:import goog.events.EventType))

(enable-console-print!)

(defn load-demo-cells! []
  (go (doseq [s state/demo-cells]
        (let [id (<! (cells/new-cell! (merge {:id (cells/alphabet-name)} s)))
              view (dissoc s :source)]
          (layout/new-view! (merge {:id id} view))))))

(defonce _ (do
             (go (<! (compiler/load-caches! 'cells.eval))
                 (cells.routes/init))
             (compiler/load-caches! 'cells.cell-helpers)))



(defn app []
  (fn []
    [:div
     [:div {:class-name "command-bar"}
      [:a {:on-click #(state/reset-state!)} "clear"]
      [:a {:on-click #(do
                       (let [s (state/serialize-state)]
                         (.setToken state/history (str "/quick/" s))))} "share"]
      #_[:a {:on-click #(do (state/reset-state! state/blank-state)
                          (load-demo-cells!)
                          )} "demo"]]
     #_[c/docs]
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