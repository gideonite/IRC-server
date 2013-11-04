(ns irc-server.core
  (:require [irc-server.parser :refer :all]
            [aleph.tcp :refer :all]
            [lamina.core :refer :all]
            [gloss.core :refer :all]))


;; A client is a map {:ip :nick}

(defprotocol IPersistClients
  (save-client! [store client])
  (get-client [store client])
  (get-nick [store nick])
  )

;;
;; TEMPORARY CLIENTS
;;
;; This is a memory store for client records, an (atom {:nick :client})

(defrecord TemporaryClients
  [!cache])

(defn temporary-clients []
  (->TemporaryClients (atom {})))

(extend-type TemporaryClients
  IPersistClients
  (save-client! [store client]
    (swap! (:!cache store) assoc (:nick client) client))
  (get-client [store client] (@(:!cache store) (:nick client)))
  (get-nick [store nick] (@(:!cache store) nick)))

(def clients (temporary-clients))

#_
(save-client! clients {:ip 172 :nick "foobar"})
(get-client clients {:ip 172 :nick "foobar"})
(get-nick clients "foobar")

;;
;; HANDLERS
;;

(defn handler [ch client-info]
  (receive-all ch
               (fn [msg]
                 (println (request-parser (str msg "\r\n")))
                 (enqueue ch "001"))))

(defn start-server
  [port]
  (start-tcp-server handler
                    {:port port
                     :frame (string :ascii :delimiters ["\r\n"])}))
