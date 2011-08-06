;   Copyright (c) Rich Hickey. All rights reserved.
;   The use and distribution terms for this software are covered by the
;   Eclipse Public License 1.0 (http://opensource.org/licenses/eclipse-1.0.php)
;   which can be found in the file epl-v10.html at the root of this distribution.
;   By using this software in any fashion, you are agreeing to be bound by
;   the terms of this license.
;   You must not remove this notice, or any other, from this software.

(ns cljs.browser-repl
  (:require [clojure.string :as str]
            [cljs.compiler :as comp])
  (:import java.io.BufferedReader
           java.io.BufferedWriter
           java.io.InputStreamReader
           java.io.OutputStreamWriter
           java.net.Socket
           java.net.ServerSocket))

;; The main purpose of this namespace is to implement the server side
;; of the browser-connected repl. This server will push data to the
;; browser to be evaluated and receive return values. Server push is
;; implemented through long polling. Requests sent from the browser
;; are expected to be HTTP Post requests.

(defonce server-state (atom {:socket nil
                             :connection nil
                             :promised-conn nil
                             :return-value-fn nil}))

(defn connection
  "Promise to return a connection when one is available. If a
  connection is not available, store the promise in server-state."
  []
  (let [p (promise)
        conn (:connection @server-state)]
    (if (and conn (not (.isClosed conn)))
      (do (deliver p conn)
          p)
      (do (swap! server-state (fn [old] (assoc old :promised-conn p)))
          p))))

(defn set-connection
  "Given a new available connection, either use it to deliver the
  connection which was promised or store the connection for later
  use."
  [conn]
  (if-let [promised-conn (:promised-conn @server-state)]
    (do (swap! server-state (fn [old] (-> old
                                         (assoc :connection nil)
                                         (assoc :promised-conn nil))))
        (deliver promised-conn conn))
    (swap! server-state (fn [old] (assoc old :connection conn)))))

(defn set-return-value-fn
  "Save the return value function which will be called when the next
  return value is received."
  [f]
  (swap! server-state (fn [old] (assoc old :return-value-fn f))))

(defn send-and-close
  "Use the passed connection to send a form to the browser."
  [conn form]
  (with-open [writer (BufferedWriter.
                      (OutputStreamWriter. (.getOutputStream conn)))]
    (do (.write writer form)
        (.write writer "\r\n") ;; remove this
        (.flush writer)
        (.close conn))))

(defn send-for-eval
  "Given a form and a return value function, send the form to the
  browser for evaluation. The return value function will be called
  when the return value is received."
  [form return-value-fn]
  (do (set-return-value-fn return-value-fn)
      (send-and-close @(connection) form)))

(defn return-value
  "Called by the server when a return value is received."
  [val]
  (when-let [f (:return-value-fn @server-state)]
    (f val)))

(defn parse-headers
  "Parse the headers of an HTTP POST request."
  [header-lines]
  (apply hash-map
   (mapcat
    (fn [line]
      (let [[k v] (str/split line #":" 2)]
        [(keyword (str/lower-case k)) (str/triml v)]))
    header-lines)))

(comment

  (parse-headers
   ["Host: www.mysite.com"
    "User-Agent: Mozilla/4.0"
    "Content-Length: 27"
    "Content-Type: application/x-www-form-urlencoded"])
)

;;; assumes first line already consumed
(defn read-headers [rdr]
  (loop [next-line (.readLine rdr)
         header-lines []]
    (if (= "" next-line)
      header-lines                      ;we're done reading headers
      (recur (.readLine rdr) (conj header-lines next-line)))))

(defn read-post [rdr]
  (let [headers (parse-headers (read-headers rdr))
        content-length (Integer/parseInt (:content-length headers))
        content (char-array content-length)]
    (io! (.read rdr content 0 content-length)
         (String. content))))

(defn read-request [reader]
  (let [line (.readLine reader)]
    (if (.startsWith line "POST")
      (read-post reader)
      line)))

(defn- handle-connection
  [conn]
  (let [rdr (BufferedReader. (InputStreamReader. (.getInputStream conn)))]
    (if-let [message (read-request rdr)]
      (do (when (not= message "ready")
            (return-value message))
          (set-connection conn)))))

(defn- server-loop
  [server-socket]
  (let [conn (.accept server-socket)]
    (do (.setKeepAlive conn true)
        (future (handle-connection conn))
        (recur server-socket))))

(defn start-server
  "Start the server on the specified port."
  [port]
  (do (println "Starting Server on Port:" port)
      (let [ss (ServerSocket. port)]
        (future (server-loop ss))
        (swap! server-state (fn [old] {:socket ss :port port})))))

(defn stop-server
  []
  (.close (:socket @server-state)))

(defn browser-eval
  [form]
  (let [return-value (promise)]
    (send-for-eval form
                   (fn [val] (deliver return-value val)))
    @return-value))

;; An initial cut at a protocol for repl evaluation. This will be
;; changed and moved somewhere else before it is finished.

(defprotocol ReplEvalEnvironment
  (setup [this])
  (evaluate [this form])
  (tear-down [this]))

(defrecord BrowserEvalEnvironment [port]
  ReplEvalEnvironment
  (setup [this]
    (start-server port))
  (evaluate [this form]
    (browser-eval form))
  (tear-down [this]
    (do (stop-server)
        (reset! server-state {}))))

(defn browser-eval-env [port]
  (BrowserEvalEnvironment. port))

(defn repl
  "This is a simplified REPL (with all compilation removed) for
   driving the above code. In the end we will have a common REPL front
   end and different backend evaluation environments."
  [repl-env]
  (prn "Type: " :cljs/quit " to quit")
  (setup repl-env)
  (loop []
    (print (str "ClojureScript:> "))
    (flush)
    (let [form (read)]
      (cond (= form :cljs/quit) :quit
            :else (let [ret (evaluate repl-env (str form))]
                    (prn ret)
                    (recur)))))
  (tear-down repl-env))

(comment

  ;; Try it out
  
  (use 'cljs.browser-repl)
  (def repl-env (browser-eval-env 9000))
  (repl repl-env)
  ;; curl -v -d "ready" http://127.0.0.1:9000
  ClojureScript:> (+ 1 1)
  ;; curl -v -d "2" http://127.0.0.1:9000
  )
