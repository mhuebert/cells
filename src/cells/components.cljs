(ns cells.components)

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
   [c-doc "@" ["id"] "get cell value"]
   [c-doc "cell!" ["id" "value"] "set cell value"]
   [c-doc "interval" ["n" "fn"] "call fn every n ms"]
   [c-doc [:span {:style {:font-style "italic"}} "self"] [] "current cell id"]
   [c-doc [:span {:style {:text-transform "uppercase" :font-size 14}} "ctrl-r"] [] "run current cell"]

   #_[fn-spec "source" ["id"] "get cell source"]])