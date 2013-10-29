(ns irc-server.server
  (:require [lamina.core :refer :all]
            [aleph.tcp :refer :all]))

(defn echo-handler [channel client-info]
  (siphon channel channel))

(defn -main [& args]
  (start-tcp-server echo-handler {:port 1234}))
