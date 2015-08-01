(ns cells.editor
  (:require [reagent.core :as r]
            [CodeMirror]
            [CodeMirror-match-brackets]
            [CodeMirror-overlay]
            [CodeMirror-subpar]))


(def cm-defaults
  {:theme           "3024-day"
   :extraKeys       (aget js/window "subparKeymap")
   :lineNumbers     false
   :lineWrapping    true
   :styleActiveLine true
   :matchBrackets   true
   :mode            "clojure"})

(defn focused? [editor]
  (-> editor (aget "state") (aget "focused")))

(defn set-preserve-cursor [editor value]
  (let [cursor-pos (.getCursor editor)]
    (.setValue editor value)
    (if (focused? editor)
      (.setCursor editor cursor-pos))))

(defn coerce [s]
  (if (string? s) s (str s)))

(def editor-style {:display "flex" :flex 1})

(defn cm-editor
  ([a] (cm-editor a {}))
  ([a options]
   (r/create-class
     {:component-did-mount    #(let [config (clj->js (merge cm-defaults options))
                                     editor (js/CodeMirror (-> % .getDOMNode) config)]
                                (r/set-state % {:editor editor :id (:id options) :a a})
                                (.setValue editor (coerce @a))

                                (add-watch a :source-edit (fn [_ _ _ source]
                                                            (if (not= source (.getValue editor))
                                                              (set-preserve-cursor editor (coerce source)))))
                                (.on editor "change" (fn [_]
                                                       (let [value (.getValue editor)]
                                                         (reset! a value))))
                                (if-not (empty? (:click-coords options))
                                  (let [[x y] (:click-coords options)
                                        pos (.coordsChar editor (clj->js {:left x :top y}))]
                                    (.focus editor)
                                    (.setCursor editor pos))))

      :component-will-unmount #(let [{:keys [editor]} (r/state %)] (.off editor))

      :reagent-render         (fn [] [:div {:style editor-style}])})))

(defn cm-editor-static
  ([a] (cm-editor-static a {}))
  ([a options]
   (r/create-class
     {:component-did-mount #(let [config (clj->js (merge cm-defaults options))
                                  editor (js/CodeMirror (-> % .getDOMNode) config)]
                             (add-watch a :source-edit (fn [_ _ _ source]
                                                (let [source (coerce source)]
                                                  (if (not= source (.getValue editor))
                                                    (.setValue editor source)))))
                             (if (:on-click options)
                               (.on editor "mousedown" (:on-click options)))
                             (r/set-state % {:editor editor :a a})
                             (.setValue editor (coerce @a)))

      :reagent-render      (fn []
                             [:div {:style editor-style}])})))




