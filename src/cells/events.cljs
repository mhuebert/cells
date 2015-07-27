(ns cells.events
  (:require-macros [cljs.core.async.macros :refer [go go-loop]])
  (:require [goog.events :as events]
            [cljs.core.async :refer [put! chan <! buffer mult tap pub sub]]
            [cells.state :as state]))

(defn listen [el type]
  (let [out (chan)]
    (events/listen el type
                   (fn [e] (put! out e)))
    out))

(let [mouse-event (listen js/window "mousemove")]
  (go-loop []
           (let [e (<! mouse-event)]
             (if (= "cm-deref" (aget e "target" "className"))
               (let [target (aget e "target")
                     id (int (subs (.-innerText target) 1))]
                 (reset! state/referenced-cells #{id}))
               (reset! state/referenced-cells #{})
               )
             (recur))))