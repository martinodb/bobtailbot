(ns bobtailbot.config)

;(def parsemode :quickanddirty)
(def parsemode :example-shopping)

;(def user-interface :repl)
(def user-interface :irc)

;; Greeting used in every user interface:
(def greeting "Hello.  Let's chat.")
;(def greeting "Hi, what's up")

;; For irc:
;
(def nick "bobtailbot")
;(def nick "bobtailbot2")
;
(def host "127.0.0.1")
;(def host "chat.freenode.net")
;
(def port 6667)
;
(def irc-channel "#whateverhey")

