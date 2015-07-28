(ns cells.keys
  (:require [goog.ui.KeyboardShortcutHandler]
            [goog.events]
            [cells.timing :refer [run-cell!]]
            [cells.state :as state]))

(defonce handler (new goog.ui.KeyboardShortcutHandler js/document))
(defonce actions (atom {}))

(defn key-event [e]
  (let [id (.-identifier e)
        func (get @actions id)]
    (if func (func))))

(defn unregister [shortcut]
  (.unregisterShortcut handler shortcut))

(defn register [shortcut func]
  (unregister shortcut)
  (let [label (str (rand-int 99999))]
    (.registerShortcut handler label shortcut)
    (swap! actions assoc label func)))

(register "ctrl+r" #(when-let [id @state/current-cell]
                     (let [source @(get @state/cells id)]
                       (run-cell! id source))))

(defonce _
         (goog.events/listen handler goog.ui.KeyboardShortcutHandler.EventType.SHORTCUT_TRIGGERED key-event))

