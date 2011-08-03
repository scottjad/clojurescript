(ns clojure.browser.event
  (:require [goog.events :as events]
            [goog.events.EventTarget :as gevent-target]
            [goog.events.EventType   :as gevent-type]
            [goog.events.Listener    :as gevent-listener]))

(defprotocol EventListener
  (handle-event  [this event])
  (fire-listener [this event]))

(extend-type goog.events.Listener
  EventListener
  (fire-listener [this event]
    (goog.events/fireListener this event))
  (handle-event [this event]
    (this/handleEvent event)))

(def event-types->gevent-types
  (into {}
        (map
         (fn [[k v]]
           [(keyword (. k (toLowerCase)))
            v])
         (js->clj goog.events.EventType))))

(def event-types (set (keys event-types->gevent-types)))


(defn listen
  ([src type fn]
     (listen src type fn false))
  ([src type fn capture?]
     (goog.events/listen src
                         (event-types->gevent-types type)
                         fn
                         capture?)))

(defn listen-once
  ([src type fn]
     (listen-once src type fn false))
  ([src type fn capture?]
     (goog.events/listenOnce src
                             (event-types->gevent-types type)
                             fn
                             capture?)))

(defn unlisten
  ([src type fn]
     (unlisten src type fn false))
  ([src type fn capture?]
     (goog.events/unlisten src
                           (event-types->gevent-types type)
                           fn
                           capture?)))

(defn unlisten-by-key
  [key]
  (goog.events/unlistenByKey key))

(defn dispatch-event
  [src event]
  (goog.events/dispatchEvent src event))

(defn expose [e]
  (goog.events/expose e))

(defn fire-listeners
  [obj type capture event])

(defn get-listener [src type listener opt_capt opt_handler]); ⇒ ?Listener
(defn all-listeners [obj type capture]); ⇒ Array.<Listener>
(defn total-listener-count []
  (goog.events/getTotalListenerCount)); ⇒ number
(defn unique-event-id [event-type]); ⇒ string
(defn has-listener [obj opt_type opt_capture]); ⇒ boolean
;; TODO? (defn listen-with-wrapper [src wrapper listener opt_capt opt_handler])
;; TODO? (defn protect-browser-event-entry-point [errorHandler])
(defn remove-all [opt_obj opt_type opt_capt]); ⇒ number
;; TODO? (defn unlisten-with-wrapper [src wrapper listener opt_capt opt_handler])
