(ns irc-server.core
  (:require [irc-server.parser :refer :all]
            [aleph.tcp :refer :all]
            [lamina.core :refer :all]
            [gloss.core :refer :all]))

(defn hexify [s]
  (format "%x" (new java.math.BigInteger (.getBytes s))))

(defn handler [ch client-info]
  (receive-all ch
               (fn [msg] (println (request-parser (str msg "\r\n"))) (enqueue ch "001"))
               ))

(defn start-server
  [port]
  (start-tcp-server handler
                    {:port port
                     :frame (string :ascii :delimiters ["\r\n"])}))
