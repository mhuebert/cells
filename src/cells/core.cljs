(ns cells.core
  ^:figwheel-always
  (:require
    [cells.js]
    [cells.state :as state]
    [cells.keys :refer [register]]
    [reagent.core :as r]
    [cells.cell-helpers :refer [new-cell]]
    [cells.timing :refer [dispose-cell-function!]]
    [cells.compile :refer [update-cell-function!]]
    [cljs-cm-editor.core :refer [cm-editor cm-editor-static]]))

(enable-console-print!)

(defn click-coords [e]
  "On a click event, return click position or empty list."
  (if (.-currentTarget e) [(.-pageX e) (.-pageY e)] []))

(def codemirror-opts {:theme         "3024-day"
                      :extraKeys     (aget js/window "subparKeymap")
                      :matchBrackets true})

(register "ctrl+r" #(when-let [id @state/current-cell]
                         (let [source (get-in @state/cells [id :source])]
                           (update-cell-function! id source))))

(defn handle-source-change!
  [id source value {:keys [dirty? editing?]}]
  (when (or (and dirty? (not editing?)) (or (string? value) (number? value)))                    ; cell was edited
    (update-cell-function! id source)))

(defn cell-view
  [id]
  (let [editor-state (r/atom {:editing? false})
        value (r/cursor state/cells [id :value])
        source (r/cursor state/cells [id :source])
        show-editor #(do (reset! state/current-cell id)
                         (reset! editor-state {:editing? true  :autofocus true :dirty? false :click-coords (click-coords %)}))
        handle-editor-blur #(do (reset! state/current-cell nil)
                                (swap! editor-state assoc :editing? false)
                                ; we only update formula cells on blur. this is why we track :dirty?
                                (handle-source-change! id @source @value @editor-state))]

    (r/create-class
      {:component-did-mount (fn [this]
                              (swap! state/index assoc-in [:cell-views id] this) ; register this view with global cell index
                              (add-watch source :handle-source-changes
                                         (fn [_ _ old-source new-source]
                                           (when-not (= old-source new-source)
                                             (swap! editor-state assoc :dirty? true)
                                             (handle-source-change! id new-source @value @editor-state))))

                              (update-cell-function! id @source))
       :show-editor         show-editor
       :reagent-render      (fn []
                              (let [val @value
                                    show-editor? (cond (:editing? @editor-state) true ;user has clicked to edit
                                                       ;(fn? val) true ;always show src for functions
                                                       (not val) true
                                                       :else false)]
                                [:div {:class-name "cell"}
                                 [:div {:class-name "cell-meta"} (str id " ")
                                  [:span
                                   (if (and (not show-editor?) (not (fn? val))) ;this is a formula cell but we are not editing
                                     [:span {:key      "formula" :class-name "show-formula"
                                             :on-click show-editor} "show formula"])]]

                                 (cond show-editor?
                                       [:div {:class-name "cell-source" :key "source"
                                              :on-blur    handle-editor-blur
                                              :on-focus   #(swap! editor-state assoc :editing? true)}
                                        [cm-editor source (merge codemirror-opts {:id id} @editor-state)]]

                                       (some-> val meta :hiccup)
                                       [:div {:class-name "cell-as-html" :key "as-html"
                                              :on-click   show-editor}
                                        val]

                                       :else
                                       [:div {:class-name "cell-value" :key "value"
                                              :on-click   show-editor}
                                        [cm-editor-static (if (fn? val) source value) ;always display functions as source
                                         (merge codemirror-opts {:readOnly "nocursor" :matchBrackets false
                                                                 :on-click show-editor})]])]))})))

(defn doc [operator args description]
  (reduce into
          [:div {:class-name "function-legend"}
           [:strong {:style {:color "#333" :font-size 15}} operator]]
          [(interpose "," (map #(do [:span {:style {:color "rgb(0, 164, 255)"}} " " %]) args))
           [[:div {:style {:color "#7d7d7d" :font-size 14 :font-weight 300}} description]]]))

(defn app []
  [:div
   [:div {:style {:font-family "monospace" :margin 30}}
    [doc "@" ["id"] "get cell value"]
    [doc "cell!" ["id" "value"] "set cell value"]
    [doc "interval" ["n" "fn"] "call fn every n ms"]
    [doc [:span {:style {:font-style "italic"}} "self"] [] "current cell id"]
    [doc [:span {:style {:text-transform "uppercase" :font-size 14}} "ctrl-r"] [] "run current cell"]

    #_[fn-spec "source" ["id"] "get cell source"]]

   (reduce into [:div {:class-name "cells"}]
           [(for [[id _] @state/cells] [cell-view id])
            [[:a {:on-click   (fn [e]
                                (let [id (new-cell)]
                                  (js/setTimeout #(.showEditor (get-in @state/index [:cell-views id]) e) 200)))
                  :class-name "touch-btn cell"}
              [:div {:class-name "cell-meta"}]
              [:div {:class-name "cell-value"} "+"]]]])])

(r/render-component [app] (.getElementById js/document "app"))