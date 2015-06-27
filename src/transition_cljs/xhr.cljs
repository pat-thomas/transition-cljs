(ns transition-cljs.xhr
  (:require [goog.net.XhrIo  :as xhr]
            [cljs.core.async :as a]
            [cljs.reader     :as reader])
  (:require-macros [cljs.core.async.macros :refer [go]]))

(defn GET
  [^String url]
  (let [ch (a/chan 1)]
    (xhr/send
     url
     (fn [event]
       (let [res (-> event .-target .getResponseText)]
         (go
           (>! ch (reader/read-string res))
           (a/close! ch)))))
    ch))
