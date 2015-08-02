(ns cells.state
  (:require [reagent.core :as r]))

;persisted values
(defonce sources (atom {}))
(defonce values (atom {}))
(defonce layout (r/atom {:settings {:x-unit 224
                                    :y-unit 126
                                    :gap    30}
                         :modes {:show-all-source false}
                         :views    (sorted-set-by #(:order @%))}))

;temporary and disposable values
(defonce compiled-fns (r/atom {}))
(defonce dependents (r/atom {}))
(defonce interval-ids (r/atom {}))
(defonce current-cell (atom nil))


(def demo-cells [{:source "(interval 500 #(rand))"}
                 {:source "(update self (int (* 10 a)) inc)"}
                 {:id 'histogram
                  :source "(html \n (into [:svg]\n   (let [ks (keys b)\n         mx (apply max (vals b))]\n     (for [[k offset]\n     \t(map vector ks \n             (range 0 100 (/ 100 (count ks))))]\n\t\t\t[:line \n            {:x1 (str offset \"%\")\n             :x2 (str offset \"%\")\n             :y1 (str (- 100 (int (* 100 (/ (get b k) mx)))) \"%\")\n             :y2 \"100%\"\n             :stroke \"blue\"\n             :stroke-width \"20\"\n             :fill \"blue\"}]))))"}
                 {:id 'emerald-green
                  :source "(let [window (take 30 (conj (:window self) a))]\n  {:window window \n   :avg (/ (reduce + window) (count window))})"}
                 {:id 'moving-average
                  :width 2
                  :source "(html\n [:svg (let [vs (map (partial * 100) (:window emerald-green))]\n    (conj\n     (for [[[y1 x1] [y2 x2]] (partition 2 1 (map vector vs (range 0 100 (/ 100 (count vs)))))]\n       [:line {:x1 (str (+ 1 x1) \"%\") :x2 (str (+ 1 x2) \"%\") \n               :y1 (str y1 \"%\") :y2 (str y2 \"%\")\n               :stroke \"blue\" :stroke-width \"2\"}])\n     [:line {:x1 \"0%\" :x2 \"100%\" :y1 (* 100 (:avg emerald-green)) :y2 (* 100 (:avg emerald-green))\n             :stroke \"pink\" :stroke-width \"1\"}]))])\n"}
                 {:id 'paintings
                  :source "(let [id self-id]\n  (.send goog.net.XhrIo\n       (str \"http://dbpedia.org/sparql?query=\"\n            (js/encodeURI \"SELECT ?s ?p ?o  WHERE { ?s dbp:type dbr:Oil_painting . ?s dbo:thumbnail ?o. } limit 30\")\n            \"&format=json\")\n       #(value! id\n                (map (comp :value :o)\n                     (-> (js->clj (.getResponseJson (.-target %)) :keywordize-keys true) :results :bindings)))))\n"}
                 {:source "(interval 2000 #(html [:img {:src (rand-nth paintings) :width \"100%\" :style {:overflow \"hidden\"} }]))"
                  :height 2}

                 ])




