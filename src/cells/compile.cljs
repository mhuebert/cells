(ns cells.compile
  (:require-macros [cljs.core.async.macros :refer [go go-loop]]
                   [reagent.ratom :refer [run!]])
  (:require [clojure.string :refer [join]]
            [cljs.core.async :refer [put! chan <! >!]]
            [goog.net.XhrIo :as xhr]
            [cells.cell-helpers :refer [eval-context cell! new-cell cell-type]]
            [cljs.reader :refer [read-string]]
            [cells.state :refer [index]]
            [cells.timing :refer [dispose-cell-function!]]
            [cells.components :as c]))

(defn wrap-source [source]
  (str "(fn [{:keys [" (join " " (map name (keys (eval-context 1)))) "]}]" source ")"))

(def compile-url "http://localhost:8001/compile")
#_(def compile-url "https://himera-open.herokuapp.com/compile")

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

(defn valid-fn? [id source]
  (let [{:keys [compiled-source compiled-fn]} (get-in @index [:outputs id])]
    (and compiled-fn (= compiled-source source))))

(defn run-cell! [id compiled-fn]
  (dispose-cell-function! id)
  (let [reaction (run! (compiled-fn (eval-context id)))]
    (swap! index assoc-in [:reactions id] reaction)))

(defn wrap [id source]
  (condp = (cell-type source)
    :cljs-expr source
    :cljs-return (str "(cell! " id (subs source 1) ")")))

(defn update-cell-function! [id source]
  (when (and (not= :text (cell-type source)) (not (valid-fn? id source)))
    (go
      (let [compiled-fn (js/eval (<! (compile (wrap id source))))]
        (swap! index update-in [:outputs id] merge {id {:compiled-source source
                                                        :compiled-fn     compiled-fn}})
        (run-cell! id compiled-fn)))))
