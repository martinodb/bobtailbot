(ns bobtailbot.core
  (:require 
  
            [user :as u]
            [bobtailbot.brains.example-shopping.brain :as shopbr]
            
            [bobtailbot.config :as cf]
            [bobtailbot.repl :as repl]
            [bobtailbot.irc :as irc]
            [clojure.core.async :as async :refer [go-loop <! >! close!]]))






(defn question-mark? [text] (re-find  #"\?$" text) )
(defn qd-respond [text]
  (if (question-mark? text) "Nice question."  "I see.") )

(defn shopbr-respond [text]
  (shopbr/respond text))


(defn respond [text]
  (case cf/parsemode
    :quickanddirty (qd-respond text)
    :example-shopping (shopbr-respond text)
    (qd-respond text)
    )
  )




(defn qd-irc-speakup [socket irc-channel]
     (go-loop[] 
       (do 
           (<! (async/timeout 3000))
           (irc/write-privmsg socket "chiming in every 3 seconds!" irc-channel)
           (recur))))

(defn shopbr-irc-speakup [socket irc-channel] (shopbr/irc-speakup socket irc-channel))

(defn speakup [socket irc-channel]
  (case [cf/user-interface cf/parsemode]
    [:irc :quickanddirty] (qd-irc-speakup socket irc-channel)
    [:irc :example-shopping] (shopbr-irc-speakup socket irc-channel)
    ()
    )
  )






(defn -main [& args]
   (case cf/user-interface
     :repl (repl/launch-repl cf/greeting respond)
     :irc (irc/connect cf/nick cf/host cf/port cf/irc-channel cf/greeting respond speakup)
     (repl/launch-repl cf/greeting respond)
     
     )
  )


