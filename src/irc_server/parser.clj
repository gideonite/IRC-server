(ns irc-server.parser
  (:require [instaparse.core :as insta]))

(def request-parser
  (insta/parser "resources/parser-rules.abnf"
                :input-format :abnf))

(comment
  (request-parser "NICK gideon\r\n")
  (request-parser "USER gideon gideon localhost :Gideon\r\n")
  (request-parser "join #foobar\r\n")
  )
