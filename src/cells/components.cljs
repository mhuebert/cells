(ns cells.components)

(defn c-error [content]
  [:span {:style {:color "rgb(255, 147, 147)" :background "rgb(137, 0, 0)" :margin -5 :padding 5}}
   content])

(defn c-meta [id source?]
  [:div {:class-name "display-cell-meta"}
   [:span {:class-name "display-cell-meta-id"} (str id)]
   (if source?
     [:span {:class-name "display-cell-meta-cljs"} "ƒ"])])