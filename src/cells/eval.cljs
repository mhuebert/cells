(ns cells.eval
  (:require-macros [cljs.core.async.macros :refer [go go-loop]]
                   [reagent.ratom :refer [run!]])
  (:require [clojure.string :refer [join]]
            [cljs.core.async :refer [put! chan <! >!]]
            [cljs.reader :refer [read-string]]
            [cljs.js :as cljs]
            [cells.refactor.rename :as rename]
            [cells.state :as state :refer [index self]]
            [reagent.core :as r]))
(enable-console-print!)

(def compiler-state (cljs/empty-state))
(def compiler-options {:eval          cljs/js-eval
                       ;:verbose       true
                       :context       :expr
                       :warnings      {:fn-deprecated false}
                       :def-emits-var true})

(defn eval-forms
  ([forms] (eval-forms forms #()))
  ([forms cb]
   (prn forms)
   (let [c (chan)]
     (try
       (cljs/eval compiler-state
                  forms
                  compiler-options
                  (fn [{:keys [value error] :as result}]
                    (prn "res " value error)
                    (cb result)
                    (if error (put! c #(prn "compile error" forms error))
                              (if value (put! c value) (put! c '())))))

       (catch js/Error e (.log js/console "compile error " e)))
     c)))
(defn eval-str
  ([source] (eval-str source #()))
  ([source cb]
   (let [c (chan)]
     (try
       (cljs/eval-str compiler-state
                      source nil compiler-options
                      (fn [{:keys [value error] :as result}]
                        (cb result)
                        (if error (put! c #(prn "compile error" source error))
                                  (if value (put! c value) (put! c "")
                                            ))))
       (catch js/Error e (.log js/console "compile error " e)))
     c)))

(defonce _
         (eval-str "(declare source! interval value! html)
                    (def ^:dynamic self)
                    (def ^:dynamic self-id)"))

(defn set-cell-value! [id val]
  (binding [*print-dup* true]
    (eval-str (str "(let [res " (with-out-str (prn val)) "]
                  (def " id " res))"))))


#_(defn update-cell-value! [id]
  (prn (str "(let [res (_" id ")]
                (def " id " res)
                (value! '" id " res ))"))
  (eval-forms `(let [res# (~(symbol (str "_" id)))]
                 (def ~id res#)
                 (~'value! '~id res#))))

(defn update-cell-value! [id]
  (eval-str (str "(binding [self-id '" (name id) " self " (name id) "]
                    (let [res (_" id ")]
                      (def " id " res)
                      (value! '" id " res )))")))

(defn compile-cell! [id source]
  (eval-str (str "(let [compiled-fn (fn [] " (rename/sugary-derefs source) ")]
          (def _" id " compiled-fn)
          compiled-fn)")))



















