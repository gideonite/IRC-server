(ns irc-server.core
  (:require [irc-server.parser :refer :all]
            [aleph.tcp :refer :all]
            [lamina.core :refer :all]
            [gloss.core :refer :all]))


;; A target is a map {:ips [ips] :name name/channel}

(defprotocol IPersistTargets
  (save-target! [store target])
  (by-name [store name])
  )

;;
;; TEMPORARY TARGETS
;;
;; This is an in memory store for target records, an (atom {:name :target}).
;; I.e. you look up by name (name/channel).

(defrecord TemporaryTargets
  [!cache])

(defn temporary-targets []
  (->TemporaryTargets (atom {})))

(extend-type TemporaryTargets
  IPersistTargets
  (save-target! [store target]
    (swap! (:!cache store) assoc (:name target) target))
  (by-name [store name] (@(:!cache store) name)))

(def targets (temporary-targets))

(comment
  (save-target! targets {:ip ["172.0.0.1"] :name "foobar"})
  (save-target! targets {:ip ["123" "456.1"] :name "#foobar"})
  (by-name targets "foobar")
  (by-name targets "#foobar"))

;;
;; HANDLERS
;;

(defn handler [ch target-info]
  (receive-all ch
               (fn [msg]
                 (println (request-parser (str msg "\r\n")))
                 (enqueue ch "001"))))

(defn start-server
  [port]
  (start-tcp-server handler
                    {:port port
                     :frame (string :ascii :delimiters ["\r\n"])}))
