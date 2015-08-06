(ns cells.editor
  (:require-macros [cljs.core.async.macros :refer [go]])
  (:require [reagent.core :as r]
            [cljs.core.async :refer [<!]]
            [cljsjs.codemirror]
            [cljsjs.codemirror.mode.clojure]
            [addons.codemirror.match-brackets]
            [addons.codemirror.overlay]
            [addons.codemirror.subpar]
            [addons.codemirror.show-hint]
            [cells.compiler :as compiler]
            [cells.state :as state]))

(def cm-defaults
  {:theme "3024-day"
   :keyMap (aget js/window "subparKeymap")
   :lineNumbers false
   :lineWrapping true
   :styleActiveLine true
   :matchBrackets true
   :mode "clojure"})

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



(def static-vars
  (memoize (fn []                                           ;clojure is not happy if we try ns-interns too soon, hence this is a fn
             (clojure.set/union (set (keys (ns-interns 'cljs.core)))
                                (set (map (comp symbol demunge) (js/Object.keys (.. js/window -cells -cell_helpers))))))))

(def static-var-strings
  (memoize (fn [] (map name (static-vars)))))

(defn show-doc [word]
  (go
    (let [m (if ((static-vars) (symbol word))
                (<! (compiler/eval-str (str "(meta (var " word "))")))
                nil)]
      (reset! state/current-meta m))))

(defn hint [word from to]
  (fn [_ _]
    (let [vars (into (static-var-strings) (map name (keys @state/values)))
          completions (filter #(.startsWith % word) vars)
          completions (if (= completions (list word)) '() completions)]
      (clj->js {:list completions
                :from from :to to}))))

(defn pre-post [n s]
  [(subs s 0 n) (subs s n (count s))])

(defn cm-editor
  ([a] (cm-editor a {}))
  ([val options]
   (r/create-class
     {:component-did-mount
      (fn [this]
        (let [config (clj->js (merge cm-defaults options))
              editor (js/CodeMirror (-> this .getDOMNode) config)]

          (r/set-state this {:editor editor :id (:id options) :a val})

          (.setValue editor (coerce val))

          (if-let [handler (:on-mount options)]
            (handler editor))

          (if-let [handler (:on-blur options)]
            (.on editor "blur" #(do
                                 (show-doc "")
                                 (handler %))))

          (if-let [handler (:on-key-handled options)]
            (.on editor "keyHandled" #(handler %)))

          (if-let [handler (:on-change options)]
            (.on editor "change" #(handler (.getValue %))))

          (.on editor "select" show-doc)

          (.on editor "cursorActivity"
               #(let [pos (.getCursor editor)
                      line (.getLine editor (.-line pos))
                      is-char (re-find #"\w" (nth line (dec (.-ch pos))))
                      [pre post] (pre-post (.-ch pos) line)
                      pre-word (last (re-find #".*?([\w-!./><+&$#?*]+)$" pre))
                      word (str pre-word
                                (last (re-find #"^([\w\-!\./\>\<+&$#?*]+).*?$" post)))
                      end-of-word? (or (empty? post) (re-find #"[^\w\-!\./\>\<+&$#?*]" (first post)))]
                 (show-doc word)
                 (if (and is-char pre-word end-of-word?)
                   (let [word-start (clj->js {:line (.-line pos)
                                              :ch   (- (.-ch pos) (count pre-word))})
                         options (clj->js {:completeSingle false
                                           :hint           (hint pre-word word-start pos)})]

                     (.showHint editor options)
                     ))))

          (if-not (empty? (:click-coords options))
            (let [[x y] (:click-coords options)
                  pos (.coordsChar editor (clj->js {:left x :top y}))]
              (.focus editor)
              (.setCursor editor pos)))))

      :componentWillReceiveProps
      (fn [this [_ val]]
        (let [editor (:editor (r/state this))
              val (coerce val)]
          (if (not= val (.getValue editor))
            (set-preserve-cursor editor val))))

      :component-will-unmount
      #(.off (:editor (r/state %)))

      :reagent-render
      (fn [] [:div {:style editor-style}])})))

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
                                   (.setValue editor (coerce a)))
      :componentWillReceiveProps (fn [this [_ val]]
                                   (.setValue (:editor (r/state this)) (coerce val)))
      :reagent-render            (fn []
                                   [:div {:style editor-style}])})))




