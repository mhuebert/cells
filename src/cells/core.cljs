(ns cells.core
  ^:figwheel-always
  (:require-macros [cljs.core.async.macros :refer [go go-loop]]
                   [reagent.ratom :refer [run!]])
  (:require
      [reagent.core :as r :refer [cursor]]

      [cells.components :as c]
      [cells.cell-helpers :refer [eval-context cell! new-cell clear-intervals! dispose-reaction!]]
      [cells.state :as state]
      [cells.compile :refer [compile]]
      [clojure.string :refer [join]]
      [cljs-cm-editor.core :refer [cm-editor]]
      [cljs.core.async :refer [put! chan <! >!]]))

(enable-console-print!)

(defn cljs? [source]
  (= (first source) \())

(defn run-cell! [id f]
  (clear-intervals! id)
  (dispose-reaction! id)
  (try
    (let [context (eval-context id)]
      (swap! state/reactions assoc id (run! (f context))))
    (catch js/Error e (do
                        (.log js/console e)
                        (cell! id (c/c-error (str e)))))))

(defn update-function-cell! [source id]
  (go
    (let [compiled-fn (js/eval (<! (compile source)))
          cell (cursor state/outputs [id])]
      (swap! cell merge {id {:compiled-source source
                             :compiled-fn     compiled-fn}})


      (run-cell! id compiled-fn))))

(defn eval-cljs! [id source]
  "Deref the source & compiled function "
  (let [{:keys [compiled-source compiled-fn]} (get @state/outputs id)
        valid-compiled-fn? (and compiled-fn (= compiled-source source))]
    (if-not valid-compiled-fn?
      (update-function-cell! source id))))

(defn click-coords [e]
  "On a click event, return click position or empty list."
  (if (.-currentTarget e) [(.-pageX e) (.-pageY e)] []))

(defn cell-view
  [id]
  (let [editor-view (r/atom {:show false})
        edit-mode? #(= true (:show @editor-view))
        cell (cursor state/cells [id])
        source-atom (cursor state/cells [id :source])
        show-editor #(do
                      (reset! editor-view {:show true
                                           :click-coords (click-coords %)
                                           :source @source-atom}))
        hide-editor (fn [is-cljs]
                      (reset! editor-view {:show false})
                      (if is-cljs (reset! source-atom (str @source-atom " "))))
        register-cell #(reset! state/cell-views [{:id id :component %}])]

    (r/create-class {:component-did-mount (fn [this]
                                            (register-cell this)

                                            (add-watch source-atom (keyword (str id "-source"))
                                                       (fn [_ _ old new]
                                                         (if-not (= old new)
                                                           (if (and (cljs? new) (not (edit-mode?)))
                                                             (eval-cljs! id new)))))
                                            @source-atom)
                     :show-editor         show-editor
                     :reagent-render      (fn []
                                            (let [{:keys [source value]} @cell
                                                  is-cljs (cljs? source)]
                                              [:div {:class-name "cell"}
                                               [c/c-meta id is-cljs]
                                               (if (edit-mode?)
                                                 [:div {:key        "source"
                                                        :on-blur    #(hide-editor is-cljs)
                                                        :class-name "cell-source"}
                                                  [cm-editor source-atom {:id id :click-coords (:click-coords @editor-view)}]]
                                                 [:div {:key        "value"
                                                        :class-name "cell-value"
                                                        :on-click   show-editor}
                                                  (or value source)])]))
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