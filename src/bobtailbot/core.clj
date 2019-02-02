(ns bobtailbot.core
  (:require 
  
            [user :as u]
            
            ;; A brain ns must always be: "bobtailbot.brains.<brain name>.brain"
            [bobtailbot.brains.quick-and-dirty.brain ]
            [bobtailbot.brains.general.brain ]
            [bobtailbot.brains.zinc.brain ]
            
            [bobtailbot.adapters.repl :as repl]
            [bobtailbot.adapters.irc :as irc]
            
            
            [clojure.core.async :as async :refer [go-loop <! >! close!]]
            
            [outpace.config :refer [defconfig]]))


(defconfig greeting "Hello.  Let's chat.")

(defconfig brain :general) ; use the name of the brain you want, as a keyword. A default is given.

(defconfig adapter :irc)


;; default non-repl chat configs
(defconfig nick "bobtailbot")
(defconfig host "127.0.0.1")
(defconfig port 6667)
(defconfig group-or-chan nick) ; eg: "bobtailbot" ; if a prefix such as "#" is needed, the adapter must add it.
;;


;; irc configs
;(defconfig irc-channel (str "#" group-or-chan)) ; eg: "#bobtailbot"
;;





(def brainns-str (str "bobtailbot.brains." (-> brain (name) (read-string)) ".brain")   )






(defn respond [text]
  (load-string (str "(" brainns-str "/respond " "\"" text "\"" ")")))


(defn hear [text]
  (load-string (str "(" brainns-str "/hear " "\"" text "\"" ")")))


(defn speakup [speakup-chan]
  ((load-string (str "(fn [x] " "(" brainns-str "/speakup "  "x"  ")" ")"  )) speakup-chan) )






(defn -main [& args]
   (case adapter
     :repl (repl/connect nick host port group-or-chan greeting hear speakup respond)
     :irc   (irc/connect nick host port group-or-chan greeting hear speakup respond)
     
     (repl/connect nick host port group-or-chan greeting hear speakup respond)
     
     )
  )


