(ns cells.editor
  (:require [reagent.core :as r]
            [cljsjs.codemirror]
            [cljsjs.codemirror.mode.clojure]
            [addons.codemirror.match-brackets]
            [addons.codemirror.overlay]
            [addons.codemirror.subpar]))


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
  ([val options]
   (r/create-class
     {:component-did-mount       #(let [config (clj->js (merge cm-defaults options))
                                        editor (js/CodeMirror (-> % .getDOMNode) config)
                                        change-handler (:on-change options)]

                                   (r/set-state % {:editor editor :id (:id options) :a val})

                                   (.setValue editor (coerce val))

                                   (.on editor "change" (fn [_]
                                                          (if change-handler
                                                            (change-handler (.getValue editor)))))

                                   (if-not (empty? (:click-coords options))
                                     (let [[x y] (:click-coords options)
                                           pos (.coordsChar editor (clj->js {:left x :top y}))]
                                       (.focus editor)
                                       (.setCursor editor pos))))

      :componentWillReceiveProps (fn [this [_ val]]
                                   (let [editor (:editor (r/state this))
                                         val (coerce val)]
                                     (if (not= val (.getValue editor))
                                       (set-preserve-cursor editor val))))

      :component-will-unmount    #(.off (:editor (r/state %)))

      :reagent-render            (fn [] [:div {:style editor-style}])})))

(defn cm-editor-static
  ([a] (cm-editor-static a {}))
  ([a options]
   (r/create-class
     {:component-did-mount       #(let [config (clj->js (merge cm-defaults options))
                                        editor (js/CodeMirror (-> % .getDOMNode) config)]

                                   #_(add-watch a :source-edit (fn [_ _ _ source]
                                                                 (let [source (coerce source)]
                                                                   (if (not= source (.getValue editor))
                                                                     (.setValue editor source)))))
                                   (if (:on-click options)
                                     (.on editor "mousedown" (:on-click options)))
                                   (r/set-state % {:editor editor :a a})
                                   (.setValue editor (coerce a) #_(coerce @a)))
      :componentWillReceiveProps (fn [this [_ val]]
                                   (.setValue (:editor (r/state this)) (coerce val)))
      :reagent-render            (fn []
                                   [:div {:style editor-style}])})))




