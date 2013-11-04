(ns irc-server.core
  (:require [irc-server.parser :refer :all]
            [aleph.tcp :refer :all]
            [lamina.core :refer :all]
            [gloss.core :refer :all]))

(defn handler [ch client-info]
  (receive-all ch
               ;#(enqueue ch (str "You said: " % "\n"))
               ;#(enqueue ch (request-parser %))
               ))

(defn -main [& args]
  (println "starting up echo tcp server on port 1234")
  (start-tcp-server handler
                    {:port 1234
                     :frame (string :ascii :delimiters ["\r\n"])
                     }))
