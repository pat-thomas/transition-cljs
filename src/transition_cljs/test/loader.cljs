(ns transition-cljs.test.loader
  (:require [cljs.core.async     :as a]
            [transition-cljs.xhr :as xhr])
  (:require-macros [cljs.core.async.macros :refer [go]]))

(defn get-expr-from-test
  [body target-expr]
  (->> body
       (filter (fn [form]
                 (= (first form) target-expr)))
       first
       rest))

(defn locate-tos
  [state]
  (let [tos (filter #(= (first %) 'to)
                    (rest state))]
    (map (fn [[_ on-enter exit-pred]]
           {:on-enter  on-enter
            :exit-pred exit-pred})
         tos)))

(defn ->executable-state
  [state-expr]
  (let [state-name (first state-expr)
        tos        (locate-tos state-expr)]
    {:state-name state-name
     :tos        tos}))

(defn ->state-machine
  [states]
  (map ->executable-state states))

(defn parse-test
  [test-name test-content]
  (println test-content)
  (if-not (= (first test-content) 'deftest)
    (throw (js/Error. (str "not a valid test: " test-name)))
    (let [[_ test-fn-name & body] test-content
          fixtures                (get-expr-from-test body 'fixtures)
          helpers                 (get-expr-from-test body 'helpers)
          states                  (get-expr-from-test body 'states)]
      {:test-name test-name
       :fixtures  fixtures
       :helpers   (map (fn [[fn-name args-vec & helper-fn-body]]
                         `(fn ~fn-name ~args-vec
                            ~@helper-fn-body))
                       helpers)
       :states    (->state-machine states)})))

(defn process-test
  [^String test-name ^String test-content]
  {:name    test-name
   :content (parse-test test-name test-content)})

(defn fetch-test
  [^String test-name]
  (xhr/GET (str "./examples/tests/" test-name ".cljs")))

(defn load-tests
  []
  (let [results   (a/chan)
        output-ch (a/chan)]
    (go
      (let [tests (<! (xhr/GET "test-suite.cljs"))]
        (doseq [test tests]
          (a/>! results test))))
    (go
      (while true
        (let [test-name (a/<! results)
              test-resp (a/<! (fetch-test test-name))]
          (a/>! output-ch (process-test test-name test-resp)))))
    output-ch))
