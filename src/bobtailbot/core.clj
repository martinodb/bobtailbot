(ns bobtailbot.core
  (:require 
  
            [user :as u]
            
            [bobtailbot.brains.example-shopping.brain :as shopbr]
            [bobtailbot.brains.quick-and-dirty.brain :as qdbr]
            
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

(def chatmode 
   (case user-interface 
   :repl :single
   :irc :group
   
   ))




(defn respond [text]
  (case parsemode
    :quickanddirty (qdbr/respond text)
    :example-shopping (shopbr/respond text)
    (qdbr/respond text)
    )
  )


(defn hear [text]
  (case parsemode
    :quickanddirty  (do);(qdbr/hear text) ; not implemented yet
    :example-shopping (shopbr/hear text)
    (qdbr/respond text)
    )
  )





(defn speakup [speakup-chan]
  (case parsemode
    :quickanddirty (qdbr/speakup speakup-chan)
    :example-shopping (shopbr/speakup2 speakup-chan)
    ()
    )
  )


(defn init-response [chatmode]
  (case parsemode
    :quickanddirty (do)
    :example-shopping (shopbr/init-response chatmode)
    (do)
    )
  )



(defn -main [& args]
   (init-response chatmode)
   (case user-interface
     :repl (repl/launch-repl greeting respond)
     :irc (irc/connect nick host port irc-channel greeting hear speakup)
     (repl/launch-repl greeting respond)
     
     )
  )


