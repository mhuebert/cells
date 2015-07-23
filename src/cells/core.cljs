(ns cells.core
  ^:figwheel-always
  (:require
    [cells.js]
    [cells.state :as state]
    [cells.keys :refer [register]]
    [reagent.core :as r]
    [cells.cell-helpers :refer [new-cell cljs?]]
    [cells.timing :refer [clear-function-cell!]]
    [cells.compile :refer [update-function-cell!]]
    [cljs-cm-editor.core :refer [cm-editor]]))

(enable-console-print!)

(defn click-coords [e]
  "On a click event, return click position or empty list."
  (if (.-currentTarget e) [(.-pageX e) (.-pageY e)] []))

(register "ctrl+r" #(when-let [id @state/current-cell]
                         (let [source (get-in @state/cells [id :source])]
                           (if (cljs? source) (update-function-cell! id source)))))

(defn cell-view
  [id]
  (let [editor-view (r/atom {:editing false})
        value-atom (r/cursor state/cells [id :value])
        source-atom (r/cursor state/cells [id :source])
        show-editor (fn [e]
                      (reset! state/current-cell id)
                      (reset! editor-view {:editing      true
                                           :focus        true
                                           :dirty        false
                                           :click-coords (click-coords e)}))

        handle-change (fn [old new]
                        (when (and (cljs? old) (not (cljs? new)))
                          (clear-function-cell! id))
                        (when (and (cljs? new) (:dirty @editor-view) (not (:editing @editor-view)))
                          (update-function-cell! id new))
                        (when-not (cljs? new)
                          (reset! value-atom new)))

        handle-editor-blur (fn []
                             (reset! state/current-cell nil)
                             (swap! editor-view assoc :editing false)
                             (let [s @source-atom]
                               (if (cljs? s) (handle-change s s))))]

    (r/create-class {:component-did-mount (fn [this]

                                            (swap! state/index assoc-in [:cell-views id] this)

                                            (add-watch source-atom (keyword (str id "-source"))
                                                       (fn [_ _ old new]
                                                         (when-not (= old new)
                                                           (if-not (:dirty @editor-view) (swap! editor-view assoc :dirty not))
                                                           (handle-change old new))))

                                            (let [s @source-atom] (if (cljs? s) (update-function-cell! id s))))
                     :show-editor         show-editor
                     :reagent-render      (fn []
                                            (let [source @source-atom value @value-atom
                                                  is-cljs (cljs? source)
                                                  editor? (cond (:editing @editor-view) true
                                                                (and is-cljs (not value)) true
                                                                :else false)]
                                              [:div {:class-name "cell"}
                                               [:div {:style      {:font-size   12
                                                                   :font-family "'Helvetica Neue', Helvetica, Arial, sans-serif"}
                                                      :class-name "cell-meta"}
                                                (str id)
                                                (if is-cljs [:span " ƒ "
                                                             (if-not editor?
                                                               [:span {:key      "formula"
                                                                       :on-click show-editor
                                                                       :class-name    "show-formula"} "show formula"])])

                                                ]
                                               (if editor?
                                                 [:div {:key        "source"
                                                        :on-blur    handle-editor-blur
                                                        :on-focus   #(reset! editor-view {:editing true})
                                                        :class-name "cell-source"}
                                                  [cm-editor source-atom (merge @editor-view
                                                                                {:theme         "3024-day"
                                                                                 :id            id
                                                                                 :extraKeys     (aget js/window "subparKeymap")
                                                                                 :matchBrackets true})]]
                                                 [:div {:key        "value"
                                                        :class-name "cell-value"
                                                        :on-click   show-editor}
                                                  (or value source)])]))
                     })))

(defn doc [type operator args description]
  (reduce into
          [:div {:class-name "function-legend"}
           [:strong {:style {:color "#333" :font-size 15}} operator]]
          [(interpose "," (map #(do [:span {:style {:color "rgb(0, 164, 255)"}} " " %]) args))
           [[:div {:style {:color "#7d7d7d" :font-size 14 :font-weight 300}} description]]]))

(defn app []
  [:div
   [:div {:style {:font-family "monospace" :margin 30}}
    [doc :fn "@" ["id"] "cell value"]
    [doc :fn "cell!" ["id" "val-or-fn"] "set cell to val-or-fn"]
    [doc :fn "interval" ["n" "fn"] "call fn every n ms"]
    [doc :var "self" [] "current cell id"]
    [doc :key [:span {:style {:text-transform "uppercase" :font-size 14}} "ctrl-r"] [] "run current cell"]

    #_[fn-spec "source" ["id"] "get cell source"]]

   (reduce into [:div {:class-name "cells"}]
           [(for [[id _] @state/cells] [cell-view id])
            [[:a {:on-click   (fn []
                                (let [id (new-cell)]
                                  (js/setTimeout
                                    #(.showEditor (get-in @state/index [:cell-views id])) 100)))
                  :class-name "touch-btn cell"}
              [:div {:class-name "cell-meta"}]
              [:div {:class-name "cell-value"} "+"]]]])])

(r/render-component [app] (.getElementById js/document "app"))