(ns cells.core
  ^:figwheel-always
  (:require
    [cells.js] [cells.events] [cljs.user]
    [cells.state :as state]
    [reagent.core :as r]
    [cells.components :as c]
    [cells.cell-helpers :refer [new-cell]]
    [cells.timing :refer [begin-cell-reaction! compile-and-run!]]
    [cljs-cm-editor.core :refer [cm-editor cm-editor-static focus-last-editor]]))

(enable-console-print!)

(defonce _ (doseq [s state/demo-cells] (new-cell s)))

(defn click-coords [e]
  "On a click event, return click position or empty list."
  (if (.-currentTarget e) [(.-pageX e) (.-pageY e)] []))

(def codemirror-opts {:theme         "3024-day"
                      :extraKeys     (aget js/window "subparKeymap")
                      :matchBrackets true
                      :mode "clojureDeref"})

(defn cell-view
  [id]
  (let [editor-state (r/atom {:editing? false})
        source (get @state/cell-source id)
        value (r/cursor state/cell-values [id])
        show-editor #(do (reset! state/current-cell id)
                         (reset! editor-state {:editing? true :click-coords (click-coords %)}))
        handle-editor-blur #(do (reset! state/current-cell nil)
                                (swap! editor-state assoc :editing? false)
                                (compile-and-run! id @source))]

    (r/create-class
      {:component-did-mount (fn [this]
                              (swap! state/index assoc-in [:cell-views id] this) ; register this view with global cell index
                              )
       :show-editor         show-editor
       :reagent-render      (fn []
                              (let [val @value
                                    show-editor? (cond (:editing? @editor-state) true ;user has clicked to edit
                                                       (fn? val) true ;always show src for functions
                                                       (not val) true
                                                       :else false)]
                                [:div {:style {:width "100%" :height "100%"}}
                                 [:div {:class-name "cell-meta"}
                                  [c/c-cell-id id]
                                  [:span
                                   (if (not show-editor?)
                                     [:span {:key      "formula" :class-name "show-formula"
                                             :on-click show-editor} "source"])]]

                                 (cond show-editor?
                                       [:div {:class-name "cell-source" :key "source"
                                              :on-blur    handle-editor-blur
                                              :on-focus   #(reset! state/current-cell id)}
                                        [cm-editor source (merge @editor-state codemirror-opts {:id id})]]

                                       (some-> val meta :hiccup)
                                       [:div {:class-name "cell-as-html" :key "as-html"}
                                        val]

                                       :else
                                       [:div {:class-name "cell-value" :key "value"
                                              :on-click   show-editor}
                                        [cm-editor-static (if (fn? val) source value) ;always display functions as source
                                         (merge codemirror-opts {:readOnly "nocursor" :matchBrackets false
                                                                 })]])]))})))


(aset js/window "show" (fn [id] (.showEditor (get-in @state/index [:cell-views id]))))

(defn app []
  [:div
   [c/c-docs]
   [:div {:key "cells" :class-name "cells"}
    (for [id (keys @state/cell-source)] [:div {:key id :class-name "cell"} [cell-view id]])
    [c/c-new-cell]]
   ])

(r/render-component [app] (.getElementById js/document "app"))

