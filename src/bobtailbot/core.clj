(ns bobtailbot.core
  (:require 
  
            [user :as u]
            
            ;[bobtailbot.brains.example-shopping.brain :as shopbr]
            [bobtailbot.brains.quick-and-dirty.brain :as qdbr]
            [bobtailbot.brains.general.brain :as genbr]
            
            [bobtailbot.repl :as repl]
            [bobtailbot.irc :as irc]
            [clojure.core.async :as async :refer [go-loop <! >! close!]]
            
            [outpace.config :refer [defconfig]]))



(defconfig brain :general)
(defconfig user-interface :irc)
(defconfig greeting "Hello.  Let's chat.")
(defconfig nick "bobtailbot")
(defconfig host "127.0.0.1")
(defconfig port 6667)
(defconfig irc-channel "#bobtailbot")






(defn respond [text]
  (case brain
    :quickanddirty (qdbr/respond text)
    ;:example-shopping (shopbr/respond text)
    :general (genbr/respond text)
    (qdbr/respond text)
    )
  )


(defn hear [text]
  (case brain
    :quickanddirty  (qdbr/hear text)
    ;:example-shopping (shopbr/hear text)
    :general (genbr/hear text)
    (qdbr/hear text)
    )
  )





(defn speakup [speakup-chan]
  (case brain
    :quickanddirty (qdbr/speakup speakup-chan)
    ;:example-shopping (shopbr/speakup speakup-chan)
    :general (genbr/speakup speakup-chan)
    (qdbr/speakup speakup-chan)
    )
  )






(defn -main [& args]
   (case user-interface
     :repl (repl/launch-repl greeting respond)
     :irc (irc/connect nick host port irc-channel greeting hear speakup)
     (repl/launch-repl greeting respond)
     
     )
  )


