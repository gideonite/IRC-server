(ns irc-server.core
  (:require [irc-server.parser :refer :all]
            [aleph.tcp :refer :all]
            [lamina.core :refer :all]
            [gloss.core :refer :all]
            [clojure.string :refer [lower-case]]))


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
  (save-target! targets {:ips ["172.0.0.1"] :name "foobar"})
  (save-target! targets {:ips ["123" "456.1"] :name "#foobar"})
  (by-name targets "foobar")
  (by-name targets "#foobar"))


;;
;; HANDLERS
;;

(def targets (atom {}))

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
                                                  :real-name real-name})
                                      (enqueue ch "001"))
                     (= cmd "nick") (do
                                      (register! ch {:nick p1})
                                      (enqueue ch "001"))
                     (= cmd "mode") (enqueue ch "<mode> not supported")
                     (= cmd "whois") (enqueue ch "<whois> not supported")
                     (= cmd "privmsg") (comment p1)
                     :else (do (println "unhandled " cmd)
                             (enqueue ch "001")))))))

(defn start-server
  [port]
  (start-tcp-server handler
                    {:port port
                     :frame (string :ascii :delimiters ["\r\n"])}))
