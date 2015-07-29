(ns cells.components
  (:require-macros [cljs.core.async.macros :refer [go go-loop]])
  (:require [reagent.core :as r]
            [cells.state :as state]
            [cljs.core.async :refer [put! chan <! buffer mult tap pub sub unsub close!]]
            [cells.events :refer [listen mouse-events]]
            [cells.cell-helpers :refer [new-cell kill-cell]]
            [cells.refactor.rename :refer [rename-symbol]]
            [cljs-cm-editor.core :refer [focus-last-editor]]))

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
  {:width  (-> width (* state/x-unit) (+ (* state/gap (dec width))))
   :height (-> height (* state/y-unit) (+ (* state/gap (dec height))) (+ 20))})

(defn c-cell-size [cell]
  (let [dragging (chan)
        begin-drag-pos (atom [])
        begin-drag-cell (atom {})
        mouse-down-handler (fn [e]
                             (reset! begin-drag-pos [(.-pageX e) (.-pageY e)])
                             (reset! begin-drag-cell @cell)
                             (sub mouse-events :mousemove dragging)
                             (sub mouse-events :mouseup dragging)
                             (.preventDefault e))
        end-drag (fn []
                   (unsub mouse-events :mousemove dragging)
                   (unsub mouse-events :mouseup dragging)
                   (swap! cell merge {:dragging false})
                   (swap! cell dissoc :drag-width :drag-height))]

    (go-loop []
             (let [e (<! dragging)]
               (if (= (.-type e) "mouseup")
                 (end-drag)
                 (let [{:keys [width height]} @begin-drag-cell
                       [x1 y1] @begin-drag-pos
                       [x2 y2] [(.-pageX e) (.-pageY e)]
                       [dx dy] [(- x2 x1) (- y2 y1)]]
                   (swap! cell merge
                          {:dragging    true
                           :drag-width  (+ width (/ dx state/x-unit))
                           :width       (max 1 (Math.round (+ width (/ dx state/x-unit))))
                           :drag-height (+ height (/ dy state/y-unit))
                           :height (max 1 (Math.round (+ height (/ dy state/y-unit))))})
                   ))
               (recur)))
    (fn [cell]
      [:div {:class-name    "cell-size"
             :on-mouse-down mouse-down-handler
             :on-mouse-up   #(do
                              (prn "mouseup")
                              (unsub mouse-events :mousemove dragging))
             ;:on-click      stop
             }])))

(defn c-docs []
  [:div {:style {:font-family "monospace" :margin 30}}
   #_[c-doc "value!" ["id" "value"] "set cell value"]
   #_[c-doc "source!" ["id" "string"] "set cell source"]
   #_[fn-spec "source" ["id"] "get cell source"]
   [c-doc "interval" ["n" "fn"] "call fn every n ms"]
   [c-doc [:span {:style {:font-style "italic"}} "self"] [] "current cell value"]
   [c-doc [:span {:style {:text-transform "uppercase" :font-size 14}} "ctrl-r"] [] "run current cell"]])

(defn c-cell-id [id]
  (let [n (r/atom (str (name id)))
        focused (r/atom false)
        self (r/current-component)
        save (fn [] (rename-symbol id (symbol @n) new-cell kill-cell))
        handle-change (fn [e]                               ; allowed chars in symbols: http://stackoverflow.com/a/3961674/3421050
                        (reset! n (clojure.string/replace (-> e .-target .-value) #"[^\w-!?&\d+*_:]+" "-")))]
    (fn [id]
      (let [display-id (if-not (.startsWith @n state/number-prefix) @n (if @focused @n (subs @n (count state/number-prefix))))]
        [:input
         {:class-name   "cell-id"
          :on-change    handle-change
          :on-focus     (fn [e] (reset! focused true) nil
                          (js/setTimeout #(-> self .getDOMNode (.setSelectionRange 0 999)) 10))
          :on-blur      (fn [] (save) (reset! focused nil))
          :on-key-press #(if (= 13 (.-which %)) (save))
          :style        {:width (+ 3 (* 7.80127 (count display-id)))}
          :value        display-id}]))))

(defn c-new-cell [styles]
  [:a {:on-click   #(do (new-cell) (js/setTimeout focus-last-editor 200))
       :class-name "touch-btn cell"
       :key        "new-cell"
       :style styles}
   [:div {:class-name "cell-meta" :key "meta"}]
   [:div {:class-name "cell-value" :key "value"} "+"]])