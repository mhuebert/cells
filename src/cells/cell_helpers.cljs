(ns cells.cell-helpers
  (:require-macros [cljs.core.async.macros :refer [go go-loop]]
                   [reagent.ratom :refer [run!]])
  (:require [reagent.core :as r :refer [cursor]]
            [goog.net.XhrIo :as xhr]
            [cljs.core.async :refer [put! chan <! >!]]
            [cljs.reader :refer [read-string]]
            [cells.layout :as layout]
            [cells.cells :as cells :refer [clear-intervals!]]
            [cells.state :as state :refer [sources values self self-id]]))

(defn ->html
  "Convert hiccup into html"
  [source]
  (with-meta source {:hiccup true}))

(defn value
  "Returns the value of a cell."
  [id]
  (if-let [a (get @values id)]
    @a nil))

(defn md
  "Parses markdown and returns html."
  [source]
  (->html [:span {:class "markdown-body" :dangerouslySetInnerHTML {:__html (js/marked source)}} ]))

(defn html
  "Inserts a string as html into the cell."
  [source]
  (let [source (or (.-innerHTML source)                     ;a DOM element
                   (some-> source (aget 0) .-parentNode .-outerHTML)
                   (some-> source (aget 0 0) .-outerHTML)   ;a nested DOM element, like from D3
                   source)]
    (->html [:span {:dangerouslySetInnerHTML {:__html source}}])))

(defn d3-svg
  "Create a DOM Node, and return an appended d3 svg node"
  []
  (-> js/d3
      (.select (js/document.createElement "svg"))
      (.attr "width" 150)
      (.attr "height" 150)))

(defn source
  "Returns the source of a cell."
  [id]
  @(get @sources id))

(defn value!
  "Sets the value of a cell."
  [id val]
  (let [a (get @values id)]
    (when (not= val @a)
      (reset! a val)))
  val)

(defn source!
  "Sets the source of a cell."
  [id val]
  (reset! (get @sources id) val)
  val)

(defn self!
  "Sets the value of the current cell."
  [val]
  (value! self-id val))

(defn interval
  "Calls the provided function every n milliseconds and sets the current cell's
  value to the result."
  ([f] (interval f 500))
  ([n f]
   (let [id self-id
         value (get @values id)
         exec #(binding [self @(get @values id)
                         self-id id]
                (reset! value (f self)))]
     (clear-intervals! id)
     (let [interval-id (js/setInterval exec (max 24 n))]
       (swap! state/interval-ids update id #(conj (or % []) interval-id)))
     (f @value))))

(defn slurp
  "Initiates an ajax request for the given path and sets the current cell's
  value to the result."
  ([path] (slurp {} path))
  ([opts path]
   (let [id self-id
         opts (cond (keyword? opts) {:as opts}
                    (fn? opts) {:fn opts}
                    :else opts)
         opts (merge {:as :text :fn identity} opts)
         parse-fn (comp (:fn opts)
                        (condp = (:as opts)
                          :text #(.getResponseText %)
                          :json->clj #(-> % .getResponseJson (js->clj :keywordize-keys true))
                          :json #(-> % .getResponseJson)))]
     (xhr/send path #(value! id (-> % .-target parse-fn)))
     nil)))

(defn new-cell!
  "Creates a new cell, adds a view for the cell to the current layout, and returns the new cell's id."
  ([] (new-cell! {}))
  ([opts]
   (let [id (or (:id opts) (cells/new-name))
         opts (assoc opts :id id)]
     (go (layout/new-view! {:id (<! (cells/new-cell! opts))}))
     id)))