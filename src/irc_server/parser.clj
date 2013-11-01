(ns irc-server.parser
  (:require [instaparse.core :as insta]))

(def request-parser
  (insta/parser   ;; TODO plop this into its own file
                "
                message    =  [ ':' prefix SP ] command [ params ] crlf
                prefix     =  servername / ( nickname [ [ '!' user ] '@' host ] )
                command    =  1*ALPHA / 3DIGIT
                params     =  *14( SP middle ) [ SP ':' trailing ]
                           / 14( SP middle ) [ SP [ ':' ] trailing ]

                servername = 14*ALPHA
                nickname = servername
                user = nickname
                host = user

                nospcrlfcl =  %x01-09 / %x0B-0C / %x0E-1F / %x21-39 / %x3B-FF
                middle     =  nospcrlfcl *( ':' / nospcrlfcl )
                trailing   =  *( ':' / ' ' / nospcrlfcl )
                crlf       =  CR LF
                "
                :input-format :abnf))

(comment
  (request-parser "NICK\r\n")
  (request-parser "NICK gideon\r\n")
  (request-parser "USER gideon gideon localhost :Gideon\r\n")
  (request-parser "join #foobar\r\n")
  )
