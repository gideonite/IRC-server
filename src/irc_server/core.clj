(ns irc-server.core
  (:require [irc-server.parser :refer :all]
            [aleph.tcp :refer :all]
            [lamina.core :refer :all]
            [gloss.core :refer :all]))

(defn hexify [s]
  (format "%x" (new java.math.BigInteger (.getBytes s))))

(defn handler [ch client-info]
  (receive-all ch
               ;(fn [msg] (enqueue ch "001"))
               #(enqueue ch (str "You said: " % "\n"))
               ;#(enqueue ch (request-parser %))
               ))

(defn start-server
  [port]
  (start-tcp-server handler
                    {:port port
                     :frame (string :ascii :delimiters ["\r\n"])}))
