(ns cells.components
  (:require-macros [cljs.core.async.macros :refer [alt! go go-loop]])
  (:require [reagent.core :as r]
            [cells.state :as state :refer [layout sources values]]
            [cells.layout :refer [add-cell-view!]]
            [cljs.core.async :refer [put! chan <! buffer mult tap pub sub unsub close!]]
            [cells.events :refer [listen window-mouse-events]]
            [cells.cells :refer [new-cell! rename-symbol!]]
            [cells.editor :refer [cm-editor cm-editor-static]]))

(declare cell-id cell-resize cell-style click-coords)
(defn mode [k] (get-in @layout [:modes k]))
(defn mode! [k v]
  (swap! layout assoc-in [:modes k] v))
(defn cell [view]
  (let [id (:id @view)
        value (get @state/values id)
        editor-content (atom @(get @state/sources id))
        show-editor #(do (reset! state/current-cell id)
                         (swap! view merge {:editing?     true
                                            :click-coords (click-coords %)}))
        handle-editor-blur #(do (reset! state/current-cell nil)
                                (swap! view merge {:editing? false
                                                   :click-coords []})
                                (reset! (get @state/sources id) @editor-content))
        handle-editor-focus #(reset! state/current-cell id)

        view-mode (cond (:editing? @view) :source
                        (some-> @value meta :hiccup) (if (mode :show-all-source) :value :hiccup)
                        :else :value)]

    [:div {:class-name "cell"
           :style (cell-style @view)}
     [cell-resize view]

     [:div {:class-name "cell-meta"}
      [cell-id id]
      [:span { :on-click show-editor :class-name (str "show-formula" (if (= view-mode :source) " hidden"))} "source"]]

     [:div {:class-name "cell-content"
            :on-click show-editor
            :on-focus     handle-editor-focus
            :on-blur      handle-editor-blur}
      (condp = view-mode
        :value
        ^{:key :value} [cm-editor-static (if (mode :show-all-source) @(get @sources id) @value)
                        {:readOnly "nocursor" :matchBrackets false}]
        :source
        ^{:key :source} [cm-editor @(get @sources id)
                         (assoc @view :on-change #(reset! editor-content %))]
        :hiccup
        [:div {:class-name "cell-as-html" :key "as-html"} @value])]]))

(defn c-error [content]
  (with-meta
    [:span {:style {:color "rgb(255, 147, 147)" :background "rgb(137, 0, 0)" :padding 5}}
     content] {:hiccup true}))

(defn c-doc [operator args description]
  (reduce into
          [:div {:class-name "function-legend"
                 :key "docs"}
           [:strong {:style {:color "#333" :font-size 15}} operator]]
          [(interpose "," (map #(do [:span {:style {:color "rgb(0, 164, 255)"}} " " %]) args))
           [[:div {:style {:color "#7d7d7d" :font-size 14 :font-weight 300}} description]]]))

(defn stop [e] (.preventDefault e) (.stopPropagation e))

(defn cell-style [{:keys [width height]}]
  (let [{:keys [x-unit y-unit gap]} (:settings @layout)]
    {:width  (-> width (* x-unit) (+ (* gap (dec width))))
     :height (-> height (* y-unit) (+ (* gap (dec height))) (+ 20))}))

(defn cell-resize [view]
  (let [drag-events (chan)
        drag-start-state (atom {})
        {:keys [x-unit y-unit gap]} (:settings @layout)
        mouse-down-handler (fn [e]
                             (reset! drag-start-state
                                     {:start-style (cell-style @view)
                                      :x1 (.-clientX e)
                                      :y1 (.-clientY e)})
                             (sub window-mouse-events :mousemove drag-events)
                             (sub window-mouse-events :mouseup drag-events)
                             (.preventDefault e))
        end-drag (fn []
                   (unsub window-mouse-events :mousemove drag-events)
                   (unsub window-mouse-events :mouseup drag-events)
                   (swap! view dissoc :drag-style))]

    (go-loop []
             (let [e (<! drag-events)]
               (if (= "mouseup" (.-type e))
                 (end-drag)
                 (let [{:keys [x1 y1 start-style]} @drag-start-state
                       {:keys [width height]} start-style
                       [x2 y2] [(.-clientX e) (.-clientY e)]
                       [dx dy] [(- x2 x1) (- y2 y1)]
                       shadow-w (-> width (+ dx) (max 10))
                       shadow-h (-> height (+ dy) (max 10))]
                   (swap! view merge
                          {:width      (-> shadow-w (/ (+ x-unit gap)) Math.round (max 1))
                           :height     (-> shadow-h (/ (+ y-unit gap)) Math.round (max 1))
                           :drag-style {:width   shadow-w
                                        :height  (- shadow-h 20)
                                        :display :block}})))
               (recur)))

    (fn [cell]
      [:div
       [:div {:class-name "cell-drag-shadow" :style (:drag-style @view)}]
       [:div {:class-name    "cell-size"
              :on-mouse-down mouse-down-handler}]])))

(defn docs []
  [:div {:style {:font-family "monospace" :margin 30}}
   [c-doc "value!" ["'id" "value"] "set cell value"]
   #_[c-doc "source!" ["id" "string"] "set cell source"]
   [c-doc "interval" ["n" "fn"] "call fn every n ms"]
   [c-doc [:span {:style {:font-style "italic"}} "self"] [] "current cell value"]
   #_[c-doc [:span {:style {:text-transform "uppercase" :font-size 14}} "ctrl-enter"] [] "run current cell"]])

(defn cell-id [id]
  (let [n (r/atom (str (name id)))
        self (r/current-component)
        enter-global-edit #(mode! :show-all-source id)
        exit-global-edit (fn [id] #(if (= id (mode :show-all-source)) (mode! :show-all-source false)))
        save (fn []
               (rename-symbol! id (symbol @n))
               (js/setTimeout (exit-global-edit id) 1000))
        handle-change (fn [e]                               ; allowed chars in symbols: http://stackoverflow.com/a/3961674/3421050
                        (reset! n (clojure.string/replace (-> e .-target .-value) #"[^\w-!?&\d+*_:]+" "-")))]
    (r/create-class
      {:should-component-update (fn [_ [_ v1] [_ v2]]
                                  (not= (:id v1) (:id v2)))
       :reagent-render
                                (fn [id]
                                  [:input
                                   {:class-name   "cell-id"
                                    :on-change    handle-change
                                    :on-focus     (fn [e]
                                                    (enter-global-edit)
                                                    (js/setTimeout #(-> self .getDOMNode (.setSelectionRange 0 999)) 10))
                                    :on-blur      (fn []
                                                    (js/setTimeout (exit-global-edit id) 1000)
                                                    (save))
                                    :on-key-press #(if (= 13 (.-which %)) (save))
                                    :style        {:width (+ 3 (* 8.40137 (count @n)))}
                                    :value        @n}])})))

(defn new-cell-btn [styles]
  [:a {:on-click   #(go (add-cell-view! (<! (new-cell!)) {:editing? true
                                                          :autofocus true}))
       :class-name "touch-btn cell"
       :key        "new-cell"
       :style      (cell-style {:width 1 :height 1})}
   [:div {:class-name "cell-meta" :key "meta"}]
   [:div {:class-name "cell-value" :key "value"} "+"]])

(defn click-coords [e]
  "On a click event, return click position or empty list."
  (if (.-currentTarget e) [(.-pageX e) (.-pageY e)] []))

