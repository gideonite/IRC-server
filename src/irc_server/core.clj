(ns irc-server.core
  (:require [aleph.tcp :refer :all]
            [lamina.core :refer :all]
            [gloss.core :refer :all]))

(defn handler [ch client-info]
  (receive-all ch
               #(enqueue ch (str "You said " % "\n"))))

(defn -main [& args]
  (println "starting up echo tcp server on port 1234")
  (start-tcp-server handler
                    {:port 1234
                     :frame (string :utf-8 :delimiters ["\r\n"])
                     }))
