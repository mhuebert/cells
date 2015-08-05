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

(defonce html #(with-meta % {:hiccup true}))

(defn value [id]
  @(get @values id))

(defn md [source]
  (html [:span {:class "markdown-body" :dangerouslySetInnerHTML {:__html (js/marked source)}} ]))

(defn source [id]
  @(get @sources id))

(defn value! [id val]
  (let [a (get @values id)]
    (when (not= val @a)
      (reset! a val)))
  val)

(defn source! [id val]
  (reset! (get @sources id) val)
  val)

(defn self! [val]
  (value! self-id val))

(defn get-json [url]
  (go
    (let [c (chan)]
      (xhr/send url
                #(let [text (-> % .-target .getResponseText)
                       res (try (-> text read-string :js)
                                (catch js/Error e (.log js/console e)))]
                  (put! c (js->clj res)))
                "GET")
      c)))

; put get-json in user namespace, declare it, try it
#_(interval 500
          (fn []
            (.send goog.net.XhrIo "http://time.jsontest.com"
                   #(cell! 2 (first (vals (js->clj (.getResponseJson (.-target %)))))))))

(defn interval
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
                          :json #(-> % .getResponseJson (js->clj :keywordize-keys true))))]
     (xhr/send path #(value! id (-> % .-target parse-fn)))
     nil)))

(defn new-cell!
  ([] (new-cell! {}))
  ([opts]
   (let [id (or (:id opts) (cells/new-name))
         opts (assoc opts :id id)]
     (go (layout/new-view! (<! (cells/new-cell! opts))))
     id)))