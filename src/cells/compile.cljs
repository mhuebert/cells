(ns cells.compile
  (:require-macros [cljs.core.async.macros :refer [go go-loop]]
                   [reagent.ratom :refer [run!]])
  (:require [clojure.string :refer [join]]
            [cljs.core.async :refer [put! chan <! >!]]
            [cljs.user :refer [self *last-val]]
            [cljs.reader :refer [read-string]]
            [cljs.js :as cljs]
            [cells.state :as state :refer [index]]
            [cells.timing :refer [dispose-cell-function!]]
            [reagent.core :as r]))

(enable-console-print!)

(def compiler-state (cljs/empty-state))
(defn eval
  [source]
  (let [c (chan)]
    (try
      (cljs/eval-str compiler-state
                     source nil {:eval          cljs/js-eval
                                 ;:verbose       true
                                 :context       :expr
                                 :warnings {:fn-deprecated false}
                                 :def-emits-var true}
                     (fn [{:keys [value error]}]
                       (if error (put! c #(prn "compile error" source error))
                                 (if value (put! c value) (put! c "")
                                           ))))
      (catch js/Error e (.log js/console "compile error " e)))
    c))

(defonce _
         (eval "(declare value! update-cached-value! source! cell html self)"))

(defn deref-cljs-user-symbols [js-source]
  (let [referenced-symbols (doall (set (map (comp symbol last) (re-seq #"cljs\.user\.([^\(\)\[\]\{\}\s\;\.]+)" js-source))))]
    (doseq [s referenced-symbols]
      @(r/cursor state/cell-values [s]))))

(defn set-cell-value! [id val]
  (binding [*print-dup* true]
    (eval (str "(let [res " (with-out-str (prn val)) "]
                  (def " id " res)
                  (update-cached-value! '" id " res ))"))))

(defn run-cell! [id]
  (dispose-cell-function! id)
  (binding [self id
            *last-val (get @state/cell-values id)]
    (try (let
           [compiled-fn (aget js/window "cljs" "user" (str "_" id))
            reaction (run!
                       (eval (str  "
                         (let [res (_" id ")]
                           (def " id " res)
                           (value! '" id " res ))"))
                       (deref-cljs-user-symbols (.toString compiled-fn)))]
           (swap! index assoc-in [:reactions id] reaction))
         (catch js/Error e (.log js/console "run-cell! error" e)))))

(defn wrap-source [id source]
  (str "(let [compiled-fn (fn [] " source ")
              res (compiled-fn)]
          (def _" id " compiled-fn)
          compiled-fn)" ))

(defn compile-cell! [id source]
  (go
    (<! (eval (wrap-source id source)))
    (swap! index update-in [:outputs id] merge {id {:compiled-source source}})
    (run-cell! id)))



















