(ns cells.core
  ^:figwheel-always
  (:require-macros [cljs.core.async.macros :refer [go]])
  (:require
      [reagent.core :as r :refer [cursor]]
      [goog.net.XhrIo :as xhr]
      [clojure.string :refer [join]]
      [cljs-cm-editor.core :refer [cm-editor]]
      [cljs.reader :refer [read-string]]
      [cljs.core.async :refer [put! chan <! >!]]))

(enable-console-print!)

(defonce cells (r/atom {1 {:source "She was there when I"}
                        2 {:source "(print-str (cell! 3 [:div {:style {:background ((cell 4))}} (cell 1)]))"}
                        3 {:source "(+ (cell 1) \" \" (cell 2))"}
                        4 {:source "(fn[](str \"rgba(\" (rand-int 255) \",\" (rand-int 255) \",\" (rand-int 255) \",\" (rand 1) \")\"))"}
                        }))

(defonce cell-views (r/atom []))

(defn cell [id]
  (let [cell-value (cursor cells [id :value])]
    @cell-value))

(defn cell! [id val]
  (let [cell (cursor cells [id :source])]
    (reset! cell val)))

(def eval-context
  {:cell cell
   :cell! cell!
   })

(defn wrap-source [source]
  (str "(fn [{:keys [" (join " " (map name (keys eval-context))) "]}]" source ")"))

(def compile-url "https://himera-open.herokuapp.com/compile")
#_(def compile-url "http://localhost:8001/compile")
(defn fetch-compiled-function [source]
  (let [c (chan)
        source (wrap-source source)]
    (xhr/send compile-url
              #(let [text (-> % .-target .getResponseText)
                     js (try (-> text read-string :js)
                             (catch js/Error e (prn e)))]
                (if js (put! c js) (prn "no value put" text)))
              "POST"
              (str "{:expr " source "}")
              (clj->js {"Content-Type" "application/clojure"}))
    c))

(defn new-cell []
  (let [id (inc (count @cells))]
    (swap! cells assoc id {:source ""})
    (js/setTimeout
      #(.showEditor (:component (last @cell-views))) 100)))

(defn compile-function! [source id]
  (go
    (let [compiled-fn (js/eval (<! (fetch-compiled-function source)))]
      (swap! cells assoc-in [id :compiled] {:compiled-source source
                                            :compiled-fn compiled-fn}))))

(defn cljs? [source]
  (= (first source) \())

(defn display-error [content]
  [:span {:style {:color "rgb(255, 147, 147)" :background "rgb(137, 0, 0)" :margin -5 :padding 5}}
   content])

(defn eval-cljs [id source]
  "Deref the source & compiled function "
  (let [{:keys [compiled-source compiled-fn]} @(r/cursor cells [id :compiled])
        valid-compiled-fn? (and compiled-fn (= compiled-source source))
        value (if valid-compiled-fn?
                (try (compiled-fn eval-context) (catch js/Error e [display-error (str e)]))
                (do (compile-function! source id) "..."))]
    (swap! cells assoc-in [id :value] value)
    value))

(defn cell-meta [id source]
  [:div {:class-name "display-cell-meta"}
   [:span {:class-name "display-cell-meta-id"} (str id)]
   (if (cljs? source)
     [:span {:class-name "display-cell-meta-cljs"} "ƒ"])])

(defn click-coords [e]
  "On a click event, return click position or empty list."
  (if (.-currentTarget e) [(.-pageX e) (.-pageY e)] []))

(defn display-source [id source]
  (swap! cells assoc-in [id :value] source)
  source)

(defn cell-view
  [id]
  (let [editor-view (r/atom {:show false})
        editor-visible? #(= true (:show @editor-view))
        source (cursor cells [id :source])
        show-editor #(reset! editor-view {:show true :click-coords (click-coords %)})
        hide-editor #(reset! editor-view {:show false})
        register-cell #(reset! cell-views [{:id id :component %}])]
    (r/create-class {:component-did-mount register-cell
                     :show-editor         show-editor
                     :reagent-render      (fn []
                                            (let [display-value (if
                                                                  (cljs? @source)
                                                                  (eval-cljs id @source)
                                                                  (display-source id @source))]
                                              (swap! cells assoc-in [id :value] display-value)

                                              [:div {:class-name "cell"}
                                               [cell-meta id @source]
                                               (if (editor-visible?)
                                                 [:div {:key "source" :on-blur hide-editor :class-name "cell-source"}
                                                  [cm-editor source {:id id :click-coords (:click-coords @editor-view)}]]
                                                 [:div {:key        "value"
                                                        :class-name "cell-value"
                                                        :on-click   show-editor}
                                                  display-value])]))
                     })))

(defn app []
  [:div
   (into [:div {:class-name "cells"}] (for [[id _] @cells] [cell-view id]))
   [:a {:on-click #(new-cell)
        :class-name "touch-btn"} "+"]])

(r/render-component [app] (.getElementById js/document "app"))