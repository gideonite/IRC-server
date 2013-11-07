(ns irc-server.core
  (:require [irc-server.parser :refer :all]
            [aleph.tcp :refer :all]
            [lamina.core :refer :all]
            [gloss.core :refer :all]
            [clojure.string :refer [lower-case upper-case]]))

(def ch->user (atom {}))
(def nick->ch (atom {}))

(def codes {:RPL_NAMREPLY 353
            :RPL_ENDOFNAMES 366
            })

(defn dispatcher
  [_ parsed-msg]
  (->> (rest parsed-msg)
    (filter #(= :command (first %)))
    (first)
    (second)
    (upper-case)
    (keyword)))

(defn params [parsed-msg]
  "returns the params vector in a parsed message.  Includes the keyword
  :params."
  (filter #(= :params (first %)) (rest parsed-msg)))

(defmulti dispatch-handler dispatcher)

(defmethod dispatch-handler :USER [ch parsed-msg]
  (let [[[-params user nick host [-trailing real-name]]] (params parsed-msg)]
    (swap! nick->ch assoc nick ch)
    (swap! ch->user assoc ch {:user user :nick nick :host host :real-name real-name})))

(defmethod dispatch-handler :NICK [ch parsed-msg]
  (let [[[-params nick]] (params parsed-msg)
        u (get @ch->user ch {})]
    (swap! nick->ch assoc nick ch)))

(defmethod dispatch-handler :PRIVMSG [src-ch parsed-msg]
  (let [[[-params target-name [-trailing msg]]] (params parsed-msg)
        src (get @ch->user src-ch)
        target-nick-ch (get @nick->ch target-name)
        target-channel-chs (map @nick->ch
                                (get @ch-name->nicks target-name))
        out (str ":" (:nick src) "! PRIVMSG " target-name " :" msg)]

    (assert (not (and target-nick-ch
                      target-channel-chs)))

    (when target-nick-ch (enqueue target-nick-ch out))
    (when target-channel-chs
      (doseq [c target-channel-chs]
        (enqueue c out)))))

(def ch-name->nicks (atom {}))

(defmethod dispatch-handler :JOIN [src-ch parsed-msg]
  (let [[[-params ch-name]] (params parsed-msg)]
    (let [user (@ch->user src-ch)
          get-nicks-in-ch #(@ch-name->nicks ch-name)
          nicks (get-nicks-in-ch)]
      (if (seq nicks)
        ;; add nick to channel
        (swap! ch-name->nicks assoc ch-name
               (conj (@ch-name->nicks ch-name) (:nick user)))

        ;; create new channel
        (swap! ch-name->nicks assoc ch-name [(:nick user)]))

      ;; JOIN message received
      (enqueue src-ch
               (str (codes :RPL_NAMREPLY) " :" (:nick user) " = "
                    ch-name
                    " :" (clojure.string/join " "
                                              (@ch-name->nicks ch-name))))

      ;; notify everyone in the channel
      (let [notice (str ":" (:nick user) " JOIN " ch-name)]
        (doseq [c (map @nick->ch (@ch-name->nicks ch-name))]
          (enqueue c notice)))

      ;; send list of nicks in channel to client
      (enqueue src-ch
               (clojure.string/join " "
                                    [(codes :RPL_ENDOFNAMES)
                                     (:nick user)
                                     ch-name
                                     ":End of /NAMES list."])))))

(comment
  (do
    (reset! ch->user {})
    (reset! nick->ch {})

    (dispatch-handler (channel)
                      (request-parser "NICK gideon\r\n"))
    (dispatch-handler (channel)
                      (request-parser "USER gideon gideon localhost :Gideon\r\n")))
  )

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
