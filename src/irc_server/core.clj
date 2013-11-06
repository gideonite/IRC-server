(ns irc-server.core
  (:require [irc-server.parser :refer :all]
            [aleph.tcp :refer :all]
            [lamina.core :refer :all]
            [gloss.core :refer :all]
            [clojure.string :refer [lower-case]]))

;;
;; HANDLERS
;;

(def targets (atom {}))


;; TODO: use multimethods to dispath register on the command type

(defn register!
  ([ch] (when-not (@targets ch) (swap! targets assoc ch {:channel ch})))
  ([ch attrs] (if-let [info (@targets ch)]
                (swap! targets assoc ch (merge info attrs))
                (swap! targets assoc ch attrs))))

(defn handler [ch client-info]
  (register! ch)

  (receive-all ch
               (fn [msg]
                 (let [[-message
                        [-command cmd] [-params p1 p2 p3 & ps] :as parsed]
                       (request-parser (str msg "\r\n"))
                       cmd (lower-case cmd)]
                   (cond
                     (= cmd "user") (let [[[-trailing trailing]] ps
                                          user-name p1
                                          nick p2
                                          host p3
                                          real-name trailing]
                                      (register! ch
                                                 {:user user-name
                                                  :nick nick
                                                  :host host
                                                  :real-name real-name}))
                     (= cmd "nick") (do
                                      (register! ch {:nick p1})
                                      (enqueue ch "001"))
                     (= cmd "mode") (enqueue ch "<mode> not supported")
                     (= cmd "whois") (enqueue ch "<whois> not supported")
                     (= cmd "privmsg") (let [[-trailing message] p2
                                             src (get @targets ch)
                                             target-nick p1])
                     :else (do (println "unhandled " cmd)
                             (enqueue ch "001")))))))

(defn start-server
  [port]
  (start-tcp-server handler
                    {:port port
                     :frame (string :ascii :delimiters ["\r\n"])}))
