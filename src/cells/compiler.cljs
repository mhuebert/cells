(ns cells.compiler
  (:require [cljs.core.async :refer [put! chan <!]]
            [cljs.reader :refer [read-string]]
            [cljs.js :as cljs]
            [cljs.pprint :refer [pprint]]))

(enable-console-print!)





(def compiler-state (cljs/empty-state))
(def compiler-options {:eval          cljs/js-eval
                       ;:verbose       true
                       :load (fn [_ _])
                       :context       :expr
                       :warnings      {:fn-deprecated false}
                       :def-emits-var true})

(defn compiler-cb [c]
  (fn [{:keys [value error]}]
    (if error (do (prn "compiler error" error)
                  (put! c (js/Error error)))
              (put! c (or value false)))))

(defn eval [forms]
  (let [c (chan)]
    (try
      (cljs/eval compiler-state forms compiler-options (compiler-cb c))
      (catch js/Error e (.log js/console "compile error " e)))
    c))

(defn eval-str [source]
  (let [c (chan)]
    (try
      (cljs/eval-str compiler-state source nil compiler-options (compiler-cb c))
      (catch js/Error e (.log js/console "compile error " e)))
    c))

(defonce _
         (eval-str "
         (declare interval html value values new-cell value!)
         "))

(defn declare-in-cljs-user [id]
  (eval `(declare ~id)))

(defn def-in-cljs-user [id value]
  (eval `(def ~id ~value)))

(defn compile-as-fn [source]
  (eval-str (str "(fn [] " source ")")))


















