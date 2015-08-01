(ns cells.compiler
  (:require [cljs.core.async :refer [put! chan <!]]
            [cljs.reader :refer [read-string]]
            [cljs.js :as cljs]
            [cljs.pprint :refer [pprint]]))

(enable-console-print!)

(def compiler-state (cljs/empty-state))
(def compiler-options {:eval          cljs/js-eval
                       :load          (fn [_ _])            ;TODO
                       :context       :expr})

(defn compiler-cb [c]
  (fn [{:keys [value error]}]
    (if error (do (put! c (js/Error error)))
              (put! c (or value false)))))

(defn eval [forms]
  (let [c (chan)]
    (try
      (cljs/eval compiler-state forms compiler-options (compiler-cb c))
      (catch js/Error e (.log js/console "compile error " e forms)))
    c))

(defn eval-str [source]
  (let [c (chan)]
    (try
      (cljs/eval-str compiler-state source nil compiler-options (compiler-cb c))
      (catch js/Error e (.log js/console "compile error " e source)))
    c))

(defonce _
         (let [names ['self 'self-id 'interval 'html 'value 'values 'new-cell 'value!]]
           (eval `(declare ~@names))))

(defn declare-in-cljs-user [id]
  (eval `(declare ~id)))

(defn def-in-cljs-user [id value]
  (eval `(def ~id ~value)))

(defn def-value-in-cljs-user [id]
  (eval `(def ~id (~'value '~id))))

(defn compile-as-fn [source]
  (eval-str (str "(fn[] " source " )")))


















