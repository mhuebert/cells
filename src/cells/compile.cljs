(ns cells.compile
  (:require-macros [cljs.core.async.macros :refer [go go-loop]]
                   [reagent.ratom :refer [run!]])
  (:require [clojure.string :refer [join]]
            [cljs.core.async :refer [put! chan <! >!]]
            [goog.net.XhrIo :as xhr]
            [cells.cell-helpers :refer [eval-context cell! new-cell]]
            [cljs.reader :refer [read-string]]
            [cljs.js :as cljs]
            [cells.state :refer [index]]
            [cells.timing :refer [dispose-cell-function!]]
            [cells.components :as c]))

(defn wrap-source [source]
  (str "(fn [{:keys [" (join " " (map name (keys (eval-context 1)))) "]}]" source ")"))

(def cstate (cljs/empty-state))

#_(defn init-repl [mode]
  (set! *target* mode)
  ;; Setup the initial repl namespace
  (cljs/compile cstate
                 "(fn[x] (prn x))"
                 'cljs.user
                 {:eval cljs/js-eval
                  :load (fn [{:keys [name macros path]}]
                          (prn name macros path)
                          (prn (-> js/window (aget "cljs" "core") .toString))
                          (condp = name
                            'clojure.core {:lang :js
                                           :context :expr
                                           :source (-> js/window (aget "cljs" "core") .toString)}
                            ))}
                 (fn [res] (prn res))))

(defn compile [source]
  (let [c (chan)
        source (wrap-source source)]
    (cljs/eval-str cstate source nil {:eval cljs/js-eval
                                         :verbose true
                                         :context :expr
                                         :load (fn [{:keys [name macros]}] (prn name macros))
                                         :def-emits-var true}
                   (fn [{:keys [value error]}]
                     (if error (put! c #(prn "compile error" source error))
                               (if value (put! c value)
                                         (prn "no value" error source)))))
    c))

(defn valid-fn? [id source]
  (let [{:keys [compiled-source compiled-fn]} (get-in @index [:outputs id])]
    (and compiled-fn (= compiled-source source))))

(defn run-cell! [id compiled-fn]
  (dispose-cell-function! id)
  (try (let [reaction (run! (compiled-fn (eval-context id)))]
         (swap! index assoc-in [:reactions id] reaction))
       (catch js/Error e (cell! id (c/c-error (str e))))))

(defn wrap [id source]
  (str "(cell! " id " " source ")"))

(defn update-cell-function! [id source]
  (when (not (valid-fn? id source))
    (go
      (let [compiled-fn (<! (compile (wrap id source)))]
        (swap! index update-in [:outputs id] merge {id {:compiled-source source
                                                        :compiled-fn     compiled-fn}})
        (run-cell! id compiled-fn)))))
