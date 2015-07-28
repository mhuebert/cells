(ns cells.components
  (:require [reagent.core :as r]
            [cells.state :as state]
            [cells.cell-helpers :refer [new-cell]]
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
        save (fn [] (rename-symbol id (symbol @n) new-cell))
        handle-change (fn [e]                               ; allowed chars in symbols: http://stackoverflow.com/a/3961674/3421050
                        (reset! n (clojure.string/replace (-> e .-target .-value) #"[^\w-!?&\d+*_:]+" "-")))]
    (fn [id]
      (let [display-id (if-not (.startsWith @n state/number-prefix) @n (if @focused @n (subs @n (count state/number-prefix))))]
        [:input
         {:class-name   "cell-id"
          :on-change    handle-change
          :on-focus     (fn [e] (reset! focused true) nil
                          #_(js/setTimeout #(-> self .getDOMNode (.setSelectionRange 0 999)) 10))
          :on-blur      (fn [] (save) (reset! focused nil))
          :on-key-press #(if (= 13 (.-which %)) (save))
          :style        {:width (+ 3 (* 7.80127 (count display-id)))}
          :value        display-id}]))))

(defn c-new-cell []
  [:a {:on-click   #(do (new-cell) (js/setTimeout focus-last-editor 200))
       :class-name "touch-btn cell"
       :key        "new-cell"}
   [:div {:class-name "cell-meta" :key "meta"}]
   [:div {:class-name "cell-value" :key "value"} "+"]])