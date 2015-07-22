(ns cells.core
  ^:figwheel-always
  (:require-macros [cljs.core.async.macros :refer [go go-loop]]
                   [reagent.ratom :refer [run!]])
  (:require
      [reagent.core :as r :refer [cursor]]
      [cells.components :as c]
      [cells.cell-helpers :refer [eval-context cell! new-cell clear-intervals! dispose-reaction! cljs?]]
      [cells.state :as state :refer [index]]
      [cells.compile :refer [compile]]
      [clojure.string :refer [join]]
      [cljs-cm-editor.core :refer [cm-editor]]
      [cljs.core.async :refer [put! chan <! >!]]))

(enable-console-print!)

(defn clear-function-cell! [id]
  (clear-intervals! id)
  (dispose-reaction! id)
  (swap! index assoc-in [:outputs id] {}))

(defn has-valid-fn? [id source]
  (let [{:keys [compiled-source compiled-fn]} (get-in @index [:outputs id])]
    (and compiled-fn (= compiled-source source))))

(defn run-cell! [id f]
  (clear-function-cell! id)
  (try
    (let [context (eval-context id)]
      (swap! index assoc-in [:reactions id] (run! (f context))))
    (catch js/Error e (do
                        (.log js/console e)
                        (cell! id (c/c-error (str e)))))))

(defn update-function-cell! [id source]
  (if-not (has-valid-fn? id source)
    (go
      (let [compiled-fn (js/eval (<! (compile source)))]
        (swap! index update-in [:outputs id] merge {id {:compiled-source source
                                                        :compiled-fn     compiled-fn}})
        (run-cell! id compiled-fn)))))

(defn click-coords [e]
  "On a click event, return click position or empty list."
  (if (.-currentTarget e) [(.-pageX e) (.-pageY e)] []))

(defn cell-view
  [id]
  (let [editor-view (r/atom {:editing false})
        edit-mode? #(= true (:editing @editor-view))
        cell (cursor state/cells [id])
        source-atom (cursor state/cells [id :source])
        hide-editor #(reset! editor-view {:editing      false})
        show-editor #(reset! editor-view {:editing      true
                                          :click-coords (click-coords %)})
        handle-editor-blur (fn []
                             (hide-editor)
                             (if (cljs? @source-atom)
                               (reset! source-atom (str @source-atom " "))
                               #_(swap! cell assoc :value @source-atom)))
        register-cell #(swap! index assoc :cell-views [{:id id :component %}])]

    (r/create-class {:component-did-mount (fn [this]
                                            (register-cell this)

                                            (add-watch source-atom (keyword (str id "-source"))
                                                       (fn [_ _ old new]
                                                         (if-not (= old new)
                                                           (if (and (cljs? old) (not (cljs? new)))
                                                             (clear-function-cell! id))
                                                           (if (and (cljs? new) (not (edit-mode?)))
                                                             (update-function-cell! id new)))))
                                            @source-atom)
                     :show-editor         show-editor
                     :reagent-render      (fn []
                                            (let [{:keys [source value]} @cell
                                                  is-cljs (cljs? source)
                                                  editor? (cond (edit-mode?) true
                                                                (and is-cljs (not value)) true
                                                                :else false)]
                                              [:div {:class-name "cell"}
                                               [:div {:style      {:font-size   12
                                                                   :font-family "'Helvetica Neue', Helvetica, Arial, sans-serif"}
                                                      :class-name "cell-meta"}
                                                (str id)
                                                (if is-cljs [:span " ƒ "
                                                             (if-not editor?
                                                               [:span {:key      "formula"
                                                                       :on-click show-editor
                                                                       :class    "show-formula"
                                                                       :style    {:cursor "pointer"
                                                                                  :color  "rgb(186, 186, 186)"}} "show formula"])])

                                                ]
                                               (if editor?
                                                 [:div {:key        "source"
                                                        :on-blur    handle-editor-blur
                                                        :class-name "cell-source"}
                                                  [cm-editor source-atom {:theme "3024-day" :id id :click-coords (:click-coords @editor-view)}]]
                                                 [:div {:key        "value"
                                                        :class-name "cell-value"
                                                        :on-click   show-editor}
                                                  (or value source)])]))
                     })))

(defn fn-spec [operator args description]
  (reduce into
          [:div {:class-name "function-legend"
                 :style {:background "#f9f9f9"
                         :display "inline-block"
                         :margin "0 10px 10px 0"
                         :white-space "pre-wrap"
                         :padding 10
                         :color "#aaa"
                         :font-size 13
                         :font-family "'Helvetica Neue', Helvetica, Arial, sans-serif"}}
           [:strong
            {:style {:color "#333"
                     :font-size 15}}
            operator]]
          [(interpose "," (map #(do [:span {:style {:color     "rgb(0, 164, 255)"
                                                    }} " " %]) args))
           [[:div {:style {:color "#7d7d7d"
                           :font-size 14
                           :font-weight 300}} description]]
           ]))

(defn app []
  [:div
   [:div {:style {:font-family "monospace" :margin 30}}
    [fn-spec "cell" ["id"] "get cell value"]
    [fn-spec "cell!" ["id" "val-or-fn"] "set cell to val-or-fn"]
    [fn-spec "self" [] "current cell value"]
    [fn-spec "self!" ["val-or-fn"] "cell! on current cell"]
    [fn-spec "interval" ["n" "fn"] "call fn every n ms"]
    [fn-spec "pulse!" ["n" "val-or-fn"] "self! every n ms"]
    [fn-spec "source" ["id"] "get cell source"]]

   (into [:div {:class-name "cells"}]
         (for [[id _] @state/cells] [cell-view id]))
   [:a {:on-click   (fn [] (new-cell)
                         (js/setTimeout
                           #(.showEditor (:component (last (:cell-views @index)))) 100))
        :class-name "touch-btn"} "+"]])

(r/render-component [app] (.getElementById js/document "app"))