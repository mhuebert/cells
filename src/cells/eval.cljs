(ns cells.eval
  (:require-macros [cljs.core.async.macros :refer [go go-loop]]
                   [reagent.ratom :refer [run!]])
  (:require [clojure.string :refer [join]]
            [cljs.core.async :refer [put! chan <! >!]]
            [cljs.reader :refer [read-string]]
            [cljs.js :as cljs]
            [cells.refactor.rename :as rename]
            [cells.state :as state :refer [index]]
            [reagent.core :as r]))
(enable-console-print!)

(def compiler-state (cljs/empty-state))

(defn eval
  ([source] (eval source #()))
  ([source cb]
   (let [c (chan)]
     (try
       (cljs/eval-str compiler-state
                      source nil {:eval          cljs/js-eval
                                  ;:verbose       true
                                  :context       :expr
                                  :warnings      {:fn-deprecated false}
                                  :def-emits-var true}
                      (fn [{:keys [value error] :as result}]
                        (cb result)
                        (if error (put! c #(prn "compile error" source error))
                                  (if value (put! c value) (put! c "")
                                            ))))
       (catch js/Error e (.log js/console "compile error " e)))
     c)))

(defonce _
         (eval "(declare value! update-cached-value! source! interval value! html self)"))

(defn set-cell-value! [id val]
  (binding [*print-dup* true]
    (eval (str "(let [res " (with-out-str (prn val)) "]
                  (def " id " res)
                  (update-cached-value! '" id " res ))"))))

(defn update-cell-value! [id]
  (eval (str "(let [res (_" id ")]
                (def " id " res)
                (value! '" id " res ))")))

(defn compile-cell! [id source]
  (eval (str "(let [compiled-fn (fn [] " (rename/sugary-derefs source) ")]
          (def _" id " compiled-fn)
          compiled-fn)" )))



















