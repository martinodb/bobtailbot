(ns bobtailbot.core
  (:require 
  
            [user :as u]
            [bobtailbot.brains.example-shopping.brain :as shopbr]
            
            
            [bobtailbot.repl :as repl]
            [bobtailbot.irc :as irc]
            [clojure.core.async :as async :refer [go-loop <! >! close!]]
            
            [outpace.config :refer [defconfig]]))


(defconfig parsemode :example-shopping)
(defconfig user-interface :irc)
(defconfig greeting "Hello.  Let's chat.")
(defconfig nick "bobtailbot")
(defconfig host "127.0.0.1")
(defconfig port 6667)
(defconfig irc-channel "#whateverhey")



(defn question-mark? [text] (re-find  #"\?$" text) )
(defn qd-respond [text]
  (if (question-mark? text) "Nice question."  "I see.") )

(defn shopbr-respond [text]
  (shopbr/respond text))


(defn respond [text]
  (case parsemode
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
  (case [user-interface parsemode]
    [:irc :quickanddirty] (qd-irc-speakup socket irc-channel)
    [:irc :example-shopping] (shopbr-irc-speakup socket irc-channel)
    ()
    )
  )






(defn -main [& args]
   (case user-interface
     :repl (repl/launch-repl greeting respond)
     :irc (irc/connect nick host port irc-channel greeting respond speakup)
     (repl/launch-repl greeting respond)
     
     )
  )


