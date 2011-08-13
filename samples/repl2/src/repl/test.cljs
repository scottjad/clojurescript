(ns repl.test
  (:require [clojure.browser.net :as net]
            [clojure.browser.event :as event]
            [clojure.browser.repl2 :as repl]
            ;; this is here because we would like to use it from the repl
            [clojure.browser.dom :as dom]))

(defn start-repl []
  (repl/start-repl "http://localhost:9000"))

(js/setTimeout start-repl 1000)

(comment

  ;; Note: If you would like for the compiler to be aware of
  ;; everything in a project then delete the 'out' directory before
  ;; calling build. This will force the compiler to compile the whole
  ;; project.

  ;; Compile this project to JavaScript
  (use 'cljs.closure)
  (def opts {:output-to "samples/repl2/out/repl.js" :output-dir "samples/repl2/out"})
  (build "samples/repl2/src" opts)
  
  ;; Start REPL
  (do (require '[cljs.repl :as repl])
      (require '[cljs.repl.browser :as browser])
      (def env (browser/repl-env "samples/repl2" 9000))
      (repl/repl env)
      )

  ;; Open a browser and point it to http://localhost:9000/index.html

  ;; Evaluate some basic forms
  (+ 1 1)
  {:a :b}
  "hello"
  (reduce + [1 2 3 4 5])
  (js/alert "Hello World!")

  ;; Can only load files that depend on what is already avaialble.
  (load-file "clojure/string.cljs")
  (clojure.string/reverse "Hello")

  ;; TODO: This is not being done automatically as it is in rhino.
  (ns cljs.user)
  (defn sum [coll] (reduce + coll))
  (sum [2 2 2 2])

  ;; Create dom elements.
  ;; This require only works because we have already compiled
  ;; 'clojure.browser.dom when the project was built.
  (ns dom.testing (:require [clojure.browser.dom :as dom]))
  (dom/append (dom/get-element "content")
              (dom/element "Hello World!"))
  
  ;; TODO: This will not work unless you have already required what it
  ;; depends on. You may think that just copying all of goog to 'out'
  ;; will solve the problem but it may not in every case. What if this
  ;; depends on another cljs file which has not been compiled?  It
  ;; will also fail if you have already added clojure.browser.dom as a
  ;; dependency.
  (load-file "clojure/browser/dom.cljs")

  )