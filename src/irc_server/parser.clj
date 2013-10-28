(ns irc-server.parser
  (:require [instaparse.core :as insta]))

(def request-parser
  (insta/parser   ;; TODO plop this into its own file
    "message = (':' prefix SPACE)? command params crlf
     prefix = servername | nick ('!' user)? ('@' host)?
     SPACE = #'\\s+'
     command = letter+ | number number number
     params = SPACE (':' trailing | middle params)?
     crlf = #'[\r\n]'

     servername = #'.*'
     nick = #'\\p{ASCII}{1,9}'
     user = #'\\p{ASCII}*'
     host = #'\\p{ASCII}*'

     letter = #'[a-zA-z]'
     number = #'\\d'

     middle = #'[\\p{ASCII}^[:]][\\p{ASCII}[^\r\n]]+'
     trailing = #'[\\p{ASCII}[^\r\n]]*'"))
