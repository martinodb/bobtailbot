(ns bobtailbot.core
  (:require 
  
            [user :as u]
            
            ;; A brain ns must always be: "bobtailbot.brains.<brain name>.brain"
            [bobtailbot.brains.quick-and-dirty.brain ]
            [bobtailbot.brains.general.brain ]
            [bobtailbot.brains.zinc.brain ]
            
            [bobtailbot.repl :as repl]
            [bobtailbot.irc :as irc]
            [clojure.core.async :as async :refer [go-loop <! >! close!]]
            
            [outpace.config :refer [defconfig]]))



(defconfig brain :general) ; use the name of the brain you want, as a keyword. A default is given.

(defconfig user-interface :irc)
(defconfig greeting "Hello.  Let's chat.")
(defconfig nick "bobtailbot")
(defconfig host "127.0.0.1")
(defconfig port 6667)
(defconfig irc-channel "#bobtailbot")


(def brainns-str (str "bobtailbot.brains." (-> brain (name) (read-string)) ".brain")   )




(defn respond [text]
  (load-string (str "(" brainns-str "/respond " "\"" text "\"" ")")))


(defn hear [text]
  (load-string (str "(" brainns-str "/hear " "\"" text "\"" ")")))


(defn speakup [speakup-chan]
  ((load-string (str "(fn [x] " "(" brainns-str "/speakup "  "x"  ")" ")"  )) speakup-chan) )






(defn -main [& args]
   (case user-interface
     :repl (repl/launch-repl greeting respond)
     :irc (irc/connect nick host port irc-channel greeting hear speakup)
     (repl/launch-repl greeting respond)
     
     )
  )


