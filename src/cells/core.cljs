(ns cells.core
  ^:figwheel-always
  (:require
      [reagent.core :as r :refer [cursor]]
      [cells.editors :refer [cm-editor]]))

(enable-console-print!)

(defonce cells (r/atom {1 {:source "cell 1"
                           :priority 1
                           :display (fn [value] #_...)}
                        2 {:source "cell 2"}
                        3 {:source "(1"}
                        4 {:source "hey"}
                        }))

(defn new-cell []
  (let [id (inc (count @cells))]
    (swap! cells assoc id {:source ""})))

(defn get-cell [id]
  (cursor cells [id]))

(defn eval-function [source]
  (let [referenced-id (int (nth source 1))
        referenced-value (cursor cells [referenced-id :value] )
        new-value (or @referenced-value "")]
    new-value))


(defn formula? [source]
  (= (first source) \())

(defn eval-cell [id]
  (let [source (:source @(get-cell id))
        value (if (formula? source) (eval-function source) source)]
    (prn "eval-cell" id)
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
                :margin "0 30px"}} "+"]
   ])

(r/render-component [app] (.getElementById js/document "app"))