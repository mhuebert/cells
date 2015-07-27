(ns cells.core
  ^:figwheel-always
  (:require
    [cells.js]
    [cljs.user]
    [cells.state :as state]
    [cells.keys :refer [register]]
    [reagent.core :as r]
    [cells.events]
    ;[cells.refactor :refer [rename-symbol]]
    [cells.components :as c]
    [cells.cell-helpers :refer [new-cell]]
    [cells.timing :refer [dispose-cell-function!]]
    [cells.compile :refer [compile-cell!]]
    [cljs-cm-editor.core :refer [cm-editor cm-editor-static focus-last-editor]]))

(enable-console-print!)

(defn click-coords [e]
  "On a click event, return click position or empty list."
  (if (.-currentTarget e) [(.-pageX e) (.-pageY e)] []))

(def codemirror-opts {:theme         "3024-day"
                      :extraKeys     (aget js/window "subparKeymap")
                      :matchBrackets true
                      :mode "clojureDeref"})

(register "ctrl+r" #(when-let [id @state/current-cell]
                         (let [source @(get @state/cell-source id)]
                           (compile-cell! id source))))



(defn cell-id [id]
  (let [n (r/atom (str (name id)))
        focused (r/atom false)
        save #()                                            ;#(rename-symbol id (symbol @n))
        handle-change (fn [e]                               ; allowed chars in symbols: http://stackoverflow.com/a/3961674/3421050
                        (reset! n (clojure.string/replace (-> e .-target .-value) #"[^\w-!?&\d+*_:]+" "-")))]
    (fn [id]
      (let [display-id (if-not (.startsWith @n state/number-prefix) @n (if @focused @n (subs @n (count state/number-prefix))))]
        [:input
         {:class-name   "cell-id"
          :on-change    handle-change
          :on-focus     #(reset! focused true)
          :on-blur      (fn [] (save) (reset! focused nil))
          :on-key-press #(if (= 13 (.-which %)) (save))
          :style        {:width (+ 3 (* 7.80127 (count display-id)))}
          :value        display-id}]))))

(defn cell-view
  [id]
  (let [editor-state (r/atom {:editing? false})
        source (get @state/cell-source id)
        value (r/cursor state/cell-values [id])
        show-editor #(do (reset! state/current-cell id)
                         (reset! editor-state {:editing? true :click-coords (click-coords %)}))
        handle-editor-blur #(do (reset! state/current-cell nil)
                                (swap! editor-state assoc :editing? false)
                                (compile-cell! id @source))]

    (r/create-class
      {:component-did-mount (fn [this]
                              (swap! state/index assoc-in [:cell-views id] this) ; register this view with global cell index
                              (compile-cell! id @source)
                              (add-watch source :handle-source-changes
                                         (fn [_ _ old-source new-source]
                                           (when (and (not= old-source new-source) (not (:editing? @editor-state)))
                                             (compile-cell! id new-source)))))
       :show-editor         show-editor
       :reagent-render (fn []
                         (let [val @value
                               show-editor? (cond (:editing? @editor-state) true ;user has clicked to edit
                                                  (fn? val) true ;always show src for functions
                                                  (not val) true
                                                  :else false)]
                                [:div {:style {:width "100%" :height "100%"}}
                                 [:div {:class-name "cell-meta"}
                                  [cell-id id]
                                  [:span
                                   (if (and (not show-editor?) (not (fn? val))) ;this is a formula cell but we are not editing
                                     [:span {:key      "formula" :class-name "show-formula"
                                             :on-click show-editor} "source"])]]

                                 (cond show-editor?
                                       [:div {:class-name "cell-source" :key "source"
                                              :on-blur    handle-editor-blur
                                              :on-focus   #(swap! editor-state assoc :editing? true)}
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

(defn c-new-cell []
  [:a {:on-click   #(do (new-cell) (js/setTimeout focus-last-editor 200))
       :class-name "touch-btn cell"
       :key        "new-cell"}
   [:div {:class-name "cell-meta" :key "meta"}]
   [:div {:class-name "cell-value" :key "value"} "+"]])

(defn app []
  [:div
   [c/c-docs]
   [:div {:key "cells" :class-name "cells"}
    (for [id (keys @state/cell-source)] [:div {:key id :class-name "cell"} [cell-view id]])
    [c-new-cell]]
   ])

(r/render-component [app] (.getElementById js/document "app"))

