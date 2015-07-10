(ns cells.core
  ^:figwheel-always
  (:require-macros [cljs.core.async.macros :refer [go]])
  (:require
      [reagent.core :as r :refer [cursor]]
      [goog.net.XhrIo :as xhr]
      [cells.editors :refer [cm-editor]]
      [cljs.reader :refer [read-string]]
      [cljs.core.async :refer [put! chan <! >!]]))

(enable-console-print!)

; had to implement OPTIONS method on /compile in himera
; as described here:

(defonce cells (r/atom {1 {:source "zebra"
                           #_:priority #_1
                           #_:display #_(fn [value] #_...)}
                        2 {:source "giraffe"}
                        3 {:source "(+ (cell 1) \" \" (cell 2))"}
                        4 {:source "go figure."}
                        }))

(defonce cell-views (r/atom []))

(defn cell [id]
  (let [cell-value (cursor cells [id :value])]
    @cell-value))

(defn wrap-source [source]
  (str "(fn [cell] " source ")"))

(def compile-url "https://himera-open.herokuapp.com/compile")
(defn compile [source]
  (let [c (chan)
        source (wrap-source source)]

    (xhr/send compile-url
              #(put! c (-> % .-target .getResponseText read-string :js))
              "POST"
              (str "{:expr " source "}")
              (clj->js {"Content-Type" "application/clojure"}))
    c))



(defn new-cell []
  (let [id (inc (count @cells))]
    (swap! cells assoc id {:source ""})
    (js/setTimeout
      #(.toggleView (:component (last @cell-views))) 100)))

(defn compile-function! [source id]
  (go
    (let [compiled-fn (js/eval (<! (compile source)))]
      (swap! cells assoc-in [id :compiled] {:compiled-source source
                                            :compiled-fn compiled-fn}))))

(defn formula? [source]
  (= (first source) \())

(defn display-error [content]
  [:span {:style {:color "rgb(255, 147, 147)" :background "rgb(137, 0, 0)" :margin -5 :padding 5}}
   content])

(defn eval-cell [id]
  (let [source @(r/cursor cells [id :source])
        {:keys [compiled-source compiled-fn]} (or @(r/cursor cells [id :compiled]) {})
        value (cond
                (not (formula? source)) source
                (and compiled-fn (= compiled-source source)) (try (compiled-fn cell)
                                                                  (catch js/Error e [display-error (str e)]))
                :else (do (compile-function! source id) "..."))]
    (swap! cells assoc-in [id :value] value)
    value))

(defn cell-view
  [id]
  (let [editor-view (r/atom {:show false})
        source (cursor cells [id :source])
        toggle-view #(if (:show @editor-view) 
                      (reset! editor-view {:show false})
                      (reset! editor-view {:show         true
                                           :click-coords (if (.-currentTarget %) [(.-pageX %) (.-pageY %)] [])}))]
    (r/create-class {
                     :component-did-mount #(swap! cell-views conj {:id id :component %})
                     :toggle-view toggle-view
                     :reagent-render      (fn []
                                            [:div {:class-name "display-cell"}

                                             [:div {:class-name "display-cell-meta"}
                                              [:span {:class-name "display-cell-meta-id"} (str id)]
                                              (if (formula? @source) [:span {:class-name "display-cell-meta-formula"} "ƒ"])]

                                             (if (:show @editor-view) [:div {:key "source" :on-blur toggle-view :class-name "display-cell-source"}
                                                                [cm-editor source {:id id :click-coords (:click-coords @editor-view)}]]
                                                               [:div {:key "value" :class-name "display-cell-value"
                                                                      :on-click   toggle-view}
                                                                (eval-cell id)])])
                     })))

(defn app []
  [:div
   (into [:div {:class-name "cells"}] (for [[id _] @cells] [cell-view id]))
   [:a {:on-click #(new-cell)
        :class-name "touch-btn"} "+"]])

(r/render-component [app] (.getElementById js/document "app"))