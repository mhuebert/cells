(ns cells.compile
  (:require [clojure.string :refer [join]]
            [cljs.core.async :refer [put! chan <! >!]]
            [goog.net.XhrIo :as xhr]
            [cells.cell-helpers :refer [eval-context cell! new-cell]]
            [cljs.reader :refer [read-string]]))

(defn wrap-source [source]
  (str "(fn [{:keys [" (join " " (map name (keys (eval-context 1)))) "]}]" source ")"))

#_(def compile-url "http://localhost:8001/compile")
(def compile-url "https://himera-open.herokuapp.com/compile")

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