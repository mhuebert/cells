(ns cells.compiler
  (:require-macros [cljs.core.async.macros :refer [go]])
  (:require [cljs.core.async :refer [put! chan <!]]
            [cljs.reader :refer [read-string]]
            [cljs.js :as cljs]
            [cljs.pprint :refer [pprint]]
            [cognitect.transit :as t]
            [goog.net.XhrIo :as xhr]
            [cells.io :refer [GET]])
  (:import [goog.net.XhrIo]))

(enable-console-print!)

(defonce compiler-state (cljs/empty-state))
(defonce compiler-options {:ns      'cells.eval
                       :eval    #(do #_(prn %) (cljs/js-eval %))
                       :context :expr
                       :load    (fn [{:keys [name macros path]} cb]
                                  (cb nil))})

(defn compiler-cb [c]
  (fn [{:keys [value error]}]
    (if error (do
                (prn error)
                (println (.. error -cause -stack))
                (put! c error))
              (put! c (or value false)))))

(defn load-cache
  ([cstate s] (load-cache cstate s {}))
  ([cstate s opts]
   (let [ext (or (:ext opts) :cljs)]
     (go
       (let [path (str "js/caches/" (clojure.string/replace (name s) "." "/") "." (name ext) ".cache.edn")
             cache-edn (<! (GET path))
             cache (read-string cache-edn)]
         (cljs.js/load-analysis-cache! cstate s cache)
         cache)))))

(defn analyzed? [st s]
  (-> @st :cljs.analyzer/namespaces (get s)))

(defn load-caches! []
  (go
    (if-not (analyzed? compiler-state 'cells.eval) (<! (load-cache compiler-state 'cells.eval)))))

(defn eval
  ([forms] (eval forms {}))
  ([forms opts]
   (let [c (chan)]
     (cljs/eval compiler-state forms (merge compiler-options opts) (compiler-cb c))
     c)))

(defn eval-str [source]
  (let [c (chan)]
    (cljs/eval-str compiler-state source nil compiler-options (compiler-cb c))
    c))

#_(defn run-cell [id]
  (eval `(def ~id (@(get cells.state/compiled-fns ~id)))))

(defn def-value [id]
  (eval `(def ~id (~'value '~id))))

(defn source->fn [source]
  (eval-str (str "(fn[] " source " )")))


















