(ns cells.core
  ^:figwheel-always
  (:require-macros [cljs.core.async.macros :refer [go]])
  (:require
      [reagent.core :as r :refer [cursor]]
      [goog.net.XhrIo :as xhr]
      [cells.editors :refer [cm-editor]]
      [cljs.reader :refer [read-string]]
      [cljs.core.async :refer [put! chan <! >!]])
  )

(enable-console-print!)

; had to implement OPTIONS method on /compile in himera
; as described here:

(defonce cells (r/atom {1 {:source "zebra"
                           :priority 1
                           :display (fn [value] #_...)}
                        2 {:source "giraffe"}
                        3 {:source "(+ (cell 1) \" \" (cell 2))"}
                        4 {:source "go figure."}
                        }))

(defn cell [id]
  (let [cell-value (cursor cells [id :value])]
    @cell-value))

(defn wrap-source [source]
  (str "(fn [cell] " source ")"))

(def compile-url "http://himera-open.herokuapp.com/compile")
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
    (swap! cells assoc id {:source ""})))

(defn compile-function! [source id]
  (go
    (let [compiled-fn (js/eval (<! (compile source)))]
      (swap! cells assoc-in [id :compiled] {:compiled-source source
                                            :compiled-fn compiled-fn}))))

(defn formula? [source]
  (= (first source) \())

(defn eval-cell [id]
  (let [source @(r/cursor cells [id :source])
        {:keys [compiled-source compiled-fn]} (or @(r/cursor cells [id :compiled]) {})
        value (cond
                (not (formula? source)) source
                (and compiled-fn (= compiled-source source)) (compiled-fn cell)
                :else (do (compile-function! source id) "..."))]
    (swap! cells assoc-in [id :value] value)
    value))

(defn display-cell [id]
      (let [show-source? (r/atom false)
            source (cursor cells [id :source])
            toggle-view #(reset! show-source? (not @show-source?))]
           (fn []
               [:div {:class-name "display-cell"}
                [:div {:class-name "display-cell-meta"}
                 #_[:span {:class-name "display-cell-meta-id"} (str id)]
                 (if (formula? @source) [:span {:class-name "display-cell-meta-formula"} "ƒ"])]
                (if @show-source? [:div {:on-blur toggle-view :class-name "display-cell-source"}
                                   [cm-editor source {:id id}]]
                                  [:div {:class-name "display-cell-value"
                                         :on-click #(toggle-view)}
                                   (eval-cell id)])])))

(defn app []
  [:div
   (into [:div {:class-name "cells"}] (for [[id _] @cells] [display-cell id]))
   [:a {:on-click #(new-cell)
        :style {:font-size 40
                :padding "30px 40px"
                :background "#efefef"
                :display "inline-block"
                :margin "0 30px"}} "+"]])

(r/render-component [app] (.getElementById js/document "app"))