(ns irc-server.parser
  (:require [instaparse.core :as insta]))

(def request-parser
  (insta/parser "resources/parser-rules.abnf"
                :input-format :abnf))

(comment
  (request-parser "NICK gideon\r\n")
  (request-parser "USER gideon gideon localhost :Gideon\r\n")
  (request-parser "join #foobar\r\n")
  (request-parser "msg puddytat Hey tat, how are you?\r\n")
  (request-parser "NOTICE gideonite wake up\r\n")
  (request-parser "part\r\n")
  (request-parser "partall\r\n")
  (request-parser "ping nickname\r\n")
  (request-parser "query you\r\n")
  (request-parser "quit\r\n")
  ;; ...
  )
