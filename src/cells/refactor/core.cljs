(ns cells.refactor.core
  (:refer-clojure :exclude [find])
  (:require [cells.state :as state :refer [values]]
            [rewrite-clj.zip :as z]
            [clojure.zip :as zip]
            [rewrite-clj.node :as n]))

(defn path
  "Path from root to loc, including whitespace and comment nodes."
  [loc]
  (->> (iterate z/up loc)
       (take-while z/up)
       (map (comp count zip/lefts))
       (reverse)))

(defn root
  "zips all the way up and returns the root node, reflecting any
 changes."
  [loc]
  (if (nil? (z/up loc))
    (vary-meta (z/down loc) assoc :rewrite-clj.zip.move/end? nil)
    (recur (z/up loc))))

(defn find-replace [zipper pred replacement]
  (loop [loc zipper]
    (if (z/end? loc)
      (root loc)
      (if
        (pred loc)
        (recur (z/next (z/replace loc (replacement loc))))
        (recur (z/next loc))))))

(defn handle-cell-references [zipper]
  (let [ref-set (set (keys @values))]
    (find-replace zipper #(ref-set (z/sexpr %))
                  #(do `(~'value '~(z/sexpr %))))))

(defn parse-slurp-args
  ([path] (parse-slurp-args :text path))
  ([content-type path]
   (let [f (condp = content-type
             :text '#(-> % .-target .getResponseText)
             :json '#(-> % .-target .getResponseJson (js->clj :keywordize-keys true)))]
     [path f])))

(defn invert-slurp [loc]
  (let [[path# parse-fn] (apply parse-slurp-args (rest (z/sexpr (z/up loc))))
        res (gensym)]
    (-> loc
        z/up
        (z/replace `(~parse-fn ~res))
        root
        (zip/edit (fn [node]
                    (let [root# (n/sexpr node)]
                      (n/coerce `(~'xhr/send ~path#
                                   (fn [~res]
                                     (~'value! '~state/self-id ~root#)))))))
        root)))

(defn handle-slurp [loc]
  (if-let [slurp-loc (z/find-depth-first loc #(= 'slurp (z/sexpr %)))]
    (invert-slurp slurp-loc)
    loc))

(defn cell-transform [source]
  (let [zipper (z/of-string source)]
    (-> zipper
        handle-slurp
        handle-cell-references
        z/root-string)))

