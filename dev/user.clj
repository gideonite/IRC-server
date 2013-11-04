(ns user
  (:require [irc-server.core :as irc]))

(defonce server (atom nil))

(defn start []
  (if @server
    (println "already started")
    (let [port 1234]
      (println "initializing server on port " port)
      (swap! server (fn [_] (irc/start-server port))))))

(defn stop []
  (when @server
    (do
      (println "shutting down server")
      (@server)
      (swap! server (fn [_] nil)))))

(defn restart []
  (stop)
  (require 'irc-server.core :reload)
  (require 'irc-server.parser :reload)
  (start))

;;
;; REPL MAIN
;;

(start)
(println "\n")
