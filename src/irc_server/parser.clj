(ns irc-server.parser
  (:require [instaparse.core :as insta]))

(defn
  keyword->concat
  "return a ( fn: coll -> [:keyword (str (rest coll))] )"
  [kw]
  (fn [& cs] [kw (apply str cs)])
  )

(defn request-parser
  [string]
  (insta/transform
    {:ALPHA identity
     :command (keyword->concat :command)
     :middle #(apply str %&)
     :trailing (keyword->concat :trailing)
     :nickname (keyword->concat :nickname)
     :SP identity
     :CR identity
     :nospcrlfcl identity
     }
    ((insta/parser "resources/parser-rules.abnf"
                  :input-format :abnf)
       string)))

(comment
  (request-parser ":gideonite PRIVMSG gideon :are you there?\r\n")

  (request-parser "NICK gideon\r\n")
  (request-parser "USER gideon gideon localhost :Gideon\r\n")
  (request-parser "join #foobar\r\n")
  (request-parser "PRIVMSG puddytat :Hey tat, how are you?\r\n")
  (request-parser "NOTICE gideonite wake up\r\n")
  (request-parser "part\r\n")
  (request-parser "partall\r\n")
  (request-parser "ping nickname\r\n")
  (request-parser "query you\r\n")
  (request-parser "quit\r\n")
  (request-parser "MODE  +i\r\n")
  ;; ...
  )
