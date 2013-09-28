(ns torus-pong.client
  (:require-macros [cljs.core.async.macros :as m :refer [go]])
  (:require [cljs.core.async :refer [alts! >! <! timeout close!]]
            [torus-pong.async.websocket :as websocket]
            [torus-pong.async.utils :refer [event-chan map-chan]]
            [torus-pong.utils :refer [log]]))


;; commands

(defn key-event->command
  [e]
  (let [code (.-keyCode e)]
    (case code
      38 [:player/up]
      40 [:player/down]
      nil)))

(defn command-chan
  []
  (event-chan "keydown" key-event->command))


;; client process

(defn spawn-client-process!
  [ws-in ws-out command-chan]
  (go (while true
        (let [[v c] (alts! [ws-out command-chan])]
          (condp = c
            ws-out       (do (log ["Got message from server" v]))
            command-chan (do (log ["Captured command from user, sending to server" v])
                             (>! ws-in v)))))))

(defn ^:export run
  []
  (.log js/console "pong!")
  (let [{:keys [in out]} (websocket/connect! "ws://localhost:8080")]
    (spawn-client-process! in out (command-chan))))
