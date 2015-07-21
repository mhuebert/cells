(ns cells.core
  ^:figwheel-always
  (:require-macros [cljs.core.async.macros :refer [go]])
  (:require
      [reagent.core :as r :refer [cursor]]
      [cells.components :as c]
      [cells.cell-helpers :refer [eval-context cell! new-cell]]
      [cells.state :as state]
      [goog.net.XhrIo :as xhr]
      [clojure.string :refer [join]]
      [cljs-cm-editor.core :refer [cm-editor]]
      [cljs.reader :refer [read-string]]
      [cljs.core.async :refer [put! chan <! >!]]))

(enable-console-print!)



(defn wrap-source [source]
  (str "(fn [{:keys [" (join " " (map name (keys (eval-context 1)))) "]}]" source ")"))

(def compile-url "https://himera-open.herokuapp.com/compile")
#_(def compile-url "http://localhost:8001/compile")
(defn compile [source]
  (let [c (chan)
        source (wrap-source source)]
    (xhr/send compile-url
              #(let [text (-> % .-target .getResponseText)
                     js (try (-> text read-string :js)
                             (catch js/Error e (.log js/console e)))]
                (if js (put! c js) (prn "no value put" text)))
              "POST"
              (str "{:expr " source "}")
              (clj->js {"Content-Type" "application/clojure"}))
    c))




(defn cljs? [source]
  (= (first source) \())

(defn run-cell! [id]

  (try
    (let [f @(r/cursor state/cells [id :compiled :compiled-fn])
          context (eval-context id)]
      (f context))
    (catch js/Error e (do
                        (.log js/console e)
                        (cell! id (c/c-error (str e)))))))

(defn update-function-cell! [source id]
  (go
    (let [compiled-fn (js/eval (<! (compile source)))]
      (swap! state/cells assoc-in [id :compiled]
             {:compiled-source source
              :compiled-fn compiled-fn})
      (run-cell! id))))

(defn eval-cljs! [id source]
  "Deref the source & compiled function "
  (let [{:keys [compiled-source compiled-fn]} @(r/cursor state/cells [id :compiled])
        valid-compiled-fn? (and compiled-fn (= compiled-source source))]
    (if valid-compiled-fn?
      (run-cell! id)
      (do (update-function-cell! source id)))
    #_(swap! cells assoc-in [id :value] value)
    #_value))

(defn click-coords [e]
  "On a click event, return click position or empty list."
  (if (.-currentTarget e) [(.-pageX e) (.-pageY e)] []))

(defn cell-view
  [id]
  (let [editor-view (r/atom {:show false})
        editor-source (cursor editor-view [:source])
        edit-mode? #(= true (:show @editor-view))
        source (cursor state/cells [id :source])
        value (cursor state/cells [id :value])
        show-editor #(do
                      (reset! editor-view {:show true
                                           :click-coords (click-coords %)
                                           :source @source}))
        hide-editor (fn [is-cljs]
                      (if is-cljs (reset! source @editor-source))
                      (reset! editor-view {:show false}))
        register-cell #(reset! state/cell-views [{:id id :component %}])]
    (r/create-class {:component-did-mount register-cell
                     :show-editor         show-editor
                     :reagent-render      (fn []
                                            (let [is-cljs (cljs? @source)]
                                              (if is-cljs (eval-cljs! id @source))
                                              [:div {:class-name "cell"}
                                               [c/c-meta id is-cljs]
                                               (if (edit-mode?)
                                                 [:div {:key "source" :on-blur #(hide-editor is-cljs) :class-name "cell-source"}
                                                  [cm-editor (if is-cljs editor-source source) {:id id :click-coords (:click-coords @editor-view)}]]
                                                 [:div {:key        "value"
                                                        :class-name "cell-value"
                                                        :on-click   show-editor}
                                                  (or @value @source)])]))
                     })))

(defn app []
  [:div
   (into [:div {:class-name "cells"}]
         (for [[id _] @state/cells] [cell-view id]))
   [:a {:on-click   (fn [] (new-cell)
                         (js/setTimeout
                           #(.showEditor (:component (last @state/cell-views))) 100))
        :class-name "touch-btn"} "+"]])

(r/render-component [app] (.getElementById js/document "app"))