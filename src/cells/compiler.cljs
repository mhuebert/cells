(ns cells.compiler
  (:require [cljs.core.async :refer [put! chan <!]]
            [cljs.reader :refer [read-string]]
            [cljs.js :as cljs]
            [cells.state :refer [index self]]))

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
         (def ^:dynamic self)
         (def ^:dynamic self-id)
         "))

(defn def-cell [id]
  (binding [*print-dup* true]
    (eval `(def ~id (~'value '~id)))))

(defn eval-def-cell [id]
  (eval `(binding [~'self-id '~id
                   ~'self ~id]
           (let [res# (~(symbol (str "_" (name id))))]
             (def ~id res#)
             (reset! (get @~'values '~id) res#)
             res#))))

(defn compile-cell-fn [id source]
  (swap! index assoc-in [:compiled-source id] source)
  (eval-str (str "(let [compiled-fn (fn [] " source ")]
          (def _" id " compiled-fn)
          compiled-fn)")))


















