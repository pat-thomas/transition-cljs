(ns transition-cljs.state
  (:require [transition-cljs.test.loader :as test-loader]))

(def app-state (atom {:tests (test-loader/load-tests)}))
