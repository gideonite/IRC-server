(ns irc-server.core
  (:require [irc-server.parser :refer :all]
            [aleph.tcp :refer :all]
            [lamina.core :refer :all]
            [gloss.core :refer :all]
            [clojure.string :refer [lower-case upper-case]]))

;;
;; HANDLERS
;;

(def targets (atom {}))

(defn register!
  "[ch] if the ch is not registered, register it without any attribute data

  [chr attrs] if ch is not registered, register with the attrs.  If it is
  registered, overwright whatever attributes are provided in `attrs`.
  "
  ([ch] (when-not (@targets ch) (swap! targets assoc ch {:channel ch}))
   (println "registered " ch))
  ([ch attrs] (if-let [info (@targets ch)]
                (do
                  (when-let [nick (:nick info)] (swap! targets assoc nick ch))
                  (swap! targets assoc ch (merge info attrs)))
                (swap! targets assoc ch attrs))
   (println "registered " attrs " on " ch)))

(defn registered?
  [key]
  (nil? (@targets key)))

(defn dispatcher
  [_ parsed-msg]
  (->> (rest parsed-msg)
    (filter #(= :command (first %)))
    (first)
    (second)
    (upper-case)
    (keyword)))

(defn params [parsed-msg]
  (filter #(= :params (first %)) (rest parsed-msg)))

(defmulti dispatch-handler dispatcher)

(defmethod dispatch-handler :USER [ch parsed-msg]
  (let [[[-params user nick host [-trailing real-name]]] (params parsed-msg)]
    (register! ch {:user user :nick nick :host host :real-name real-name})))

(defmethod dispatch-handler :NICK [ch parsed-msg]
  (let [[[-params nick]] (params parsed-msg)]
    (register! ch {:nick nick})))

(defmethod dispatch-handler :PRIVMSG [ch parsed-msg]
  (let [[[-params target-nick [-trailing msg]]] (params parsed-msg)
        src (get @targets ch)
        target-ch (get @targets target-nick)
        out (str (:nick src) ": PRIVMSG " target-nick " :" msg)]
    (when target-ch (enqueue target-ch out))))

(defn main-handler [ch client-info]
  (receive-all ch dispatch-handler))

;;
;;

(defn handler [ch client-info]
  (enqueue ch "001")
  (receive-all ch
               (fn [msg]
                 (let [[-message
                        [-command cmd] [-params p1 p2 p3 & ps] :as parsed]
                       (request-parser (str msg "\r\n"))
                       cmd (lower-case cmd)]

                   (try (dispatch-handler ch parsed)
                     (catch Exception e
                       (println
                         "command not found "
                         (last (clojure.string/split (.getMessage e)  #" ")))))

                   (cond
                     (= cmd "ping") (enqueue ch "pong")
                     (= cmd "mode") (enqueue ch "<mode> not supported")
                     (= cmd "whois") (enqueue ch "<whois> not supported")
                     :else (do (println "unhandled by cond" cmd)
                             (enqueue ch "001")))))))

(defn start-server
  [port]
  (start-tcp-server handler
                    {:port port
                     :frame (string :ascii :delimiters ["\r\n"])}))

(user/restart)

(comment
  (dispatcher 'ch (request-parser "nick gideon\r\n"))
  (dispatcher 'ch (request-parser ":gideonite PRIVMSG gideon :are you there?\r\n"))
  (dispatcher 'ch (request-parser "join #foobar\r\n"))
  (dispatcher 'ch (request-parser "PRIVMSG puddytat :Hey tat, how are you?\r\n"))
  (dispatcher 'ch (request-parser "USER gideon gideon localhost :Gideon\r\n"))
  (dispatcher 'ch (request-parser "NOTICE gideonite wake up\r\n")))

(comment
  (dispatch-handler (channel)
                    (request-parser "UNDEFINED gideonite wake up\r\n"))

  (dispatch-handler (channel)
                    (request-parser "USER gideon gideon localhost :Gideon\r\n")))
