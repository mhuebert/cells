(ns cells.core
  ^:figwheel-always
  (:require-macros [cljs.core.async.macros :refer [go go-loop]]
                   [reagent.ratom :refer [run!]])
  (:require
    [CodeMirror]
    [CodeMirror-match-brackets]
    [CodeMirror-overlay]
    [CodeMirror-deref]
    [CodeMirror-subpar]
    [reagent.core :as r :refer [cursor]]
    [cells.components :as c]
    [cells.cell-helpers :refer [eval-context cell! new-cell cljs? run-cell! clear-function-cell!]]
    [cells.state :as state :refer [index]]
    [cells.compile :refer [compile]]
    [clojure.string :refer [join]]
    [cljs-cm-editor.core :refer [cm-editor]]
    [cljs.core.async :refer [put! chan <! >!]]))

(enable-console-print!)



(defn has-valid-fn? [id source]
  (let [{:keys [compiled-source compiled-fn]} (get-in @index [:outputs id])]
    (and compiled-fn (= compiled-source source))))


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
                                          :focus true
                                          :click-coords (click-coords %)})
        handle-change (fn [old new]
                        (if (and (cljs? old) (not (cljs? new)))
                          (clear-function-cell! id))
                        (if (and (cljs? new) (not (edit-mode?)))
                          (update-function-cell! id new))
                        (if-not (cljs? new)
                          (swap! cell assoc :value new)))
        handle-editor-blur (fn []
                             (hide-editor)
                             (let [s @source-atom]
                               (if (cljs? s) (handle-change s s)))
                             )]

    (r/create-class {:component-did-mount (fn [this]
                                            (swap! index assoc-in [:cell-views id] this)

                                            (add-watch source-atom (keyword (str id "-source"))
                                                       (fn [_ _ old new]
                                                         (if-not (= old new) (handle-change old new))))

                                            (let [s @source-atom] (if (cljs? s) (update-function-cell! id s))))
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
                                                        :on-focus   #(reset! editor-view {:editing true})
                                                        :class-name "cell-source"}
                                                  [cm-editor source-atom (merge @editor-view
                                                                                {:theme         "3024-day"
                                                                                 :id            id
                                                                                 :extraKeys     (aget js/window "subparKeymap")
                                                                                 :matchBrackets true})]]
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
    [fn-spec "pulse!" ["n" "val-or-fn"] "cell! on current cell every n ms"]
    [fn-spec "source" ["id"] "get cell source"]]

   (into [:div {:class-name "cells"}]
         (for [[id _] @state/cells] [cell-view id]))
   [:a {:on-click   (fn []
                      (let [id (new-cell)]
                        (js/setTimeout
                          #(.showEditor (get-in @index [:cell-views id])) 100)))
        :class-name "touch-btn"} "+"]])

(r/render-component [app] (.getElementById js/document "app"))