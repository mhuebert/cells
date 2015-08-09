(ns cells.state
  (:require [reagent.core :as r]
            [cognitect.transit :as t])
  (:import goog.History))

(def ^:dynamic self nil)
(def ^:dynamic self-id nil)

(def blank-layout {:settings {:x-unit 224
                              :y-unit 126
                              :gap    30}
                   :modes {:show-all-source false}
                   :views    (sorted-set-by #(:order @%))})

(def blank-state {:sources {}
                  :layout blank-layout
                  :values {}
                  :compiled-fns {}
                  :dependents {}
                  :interval-ids {}
                  :current-cell nil
                  :current-meta {}})

;user values
(defonce sources (atom (:sources blank-state)))
(defonce layout (r/atom (:layout blank-state)))

;derived balues
(defonce values (atom (:values blank-state)))
(defonce compiled-fns (atom (:compiled-fns blank-state)))
(defonce dependents (atom (:dependents blank-state)))
(defonce interval-ids (atom (:interval-ids blank-state)))
(defonce current-cell (atom (:current-cell blank-state)))
(defonce current-meta (r/atom (:current-meta blank-state)))

(defn reset-state!
  ([] (reset-state! blank-state))
  ([state]
   (let [state (merge blank-state state)]
     (reset! layout (:layout state))
     (reset! sources (:sources state))
     (reset! values (:values state))
     (reset! compiled-fns (:compiled-fns state))
     (reset! dependents (:dependents state))
     (reset! interval-ids (:interval-ids state))
     (reset! current-cell (:current-cell state))
     (reset! current-meta (:current-meta blank-state)))))

(defn serialize-state []
  (js/encodeURIComponent (js/JSON.stringify (clj->js {:cells (for [[id v] @sources]
                                                               {:id id :source @v})
                                                      :views (map deref (:views @layout))}))))

(defn deserialize-state [data]
  (let [{:keys [cells views]} (-> data js/JSON.parse (js->clj :keywordize-keys true))]
    {:cells (map #(update % :id symbol) cells)
     :views (map #(update % :id symbol) views)}))

(defonce history (History.))

(def demo-cells [{:id 'b
                  :source "(slurp :text \"https://raw.githubusercontent.com/clojure/clojurescript/master/src/main/cljs/cljs/core.cljs\")"
                  }
                 {:id 'masseuse-pony
                  :source "(sort-by (comp - last) (vec (reduce #(update %1 (symbol %2) inc) {} (.split (.toLowerCase b) (re-pattern \"[^a-z]+\")))))"}
                 {:id 'cljs-core-symbols
                  :source "(set (map (comp symbol demunge)(js/Object.keys (goog.object.getValueByKeys js/window \"cljs\" \"core\"))))"}])