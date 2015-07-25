(ns cells.components)

(defn c-error [content]
  (with-meta
    [:span {:style {:color "rgb(255, 147, 147)" :background "rgb(137, 0, 0)" :margin -5 :padding 5}}
     content] {:hiccup true}))

