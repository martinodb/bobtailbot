(ns bobtailbot.core
  (:require 
  
            [user :as u]
            
            ;; A brain ns must always be: "bobtailbot.brains.<brain-name>.brain"
            [bobtailbot.brains.quick-and-dirty.brain ]
            [bobtailbot.brains.general.brain ]
            [bobtailbot.brains.zinc.brain ]
            
            ;; An adapter ns must always be "bobtailbot.adapters.<adapter-name>"
            ;; Also, an adapter must always have a "connect" function as follows: "(<adapter-name>/connect nick host port group-or-chan greeting hear speakup respond)"
            ;; If it doesn't need some of those parameters, it can simpy ignore them.
            [bobtailbot.adapters.repl]
            [bobtailbot.adapters.irc]
            
            
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

(def adapterns-str (str "bobtailbot.adapters." (-> adapter (name) (read-string)) )   )




(defn respond [text]
  (load-string (str "(" brainns-str "/respond " "\"" text "\"" ")")))


(defn hear [text]
  (load-string (str "(" brainns-str "/hear " "\"" text "\"" ")")))


(defn speakup [speakup-chan]
  ((load-string (str "(fn [x] " "(" brainns-str "/speakup "  "x"  ")" ")"  )) speakup-chan) )



(defn connect [nick host port group-or-chan greeting hear speakup respond]
  ((load-string (str "(fn [nick host port group-or-chan greeting hear speakup respond] " "(" adapterns-str "/connect "  "nick host port group-or-chan greeting hear speakup respond"  ")" ")"  )) nick host port group-or-chan greeting hear speakup respond) )





(defn -main [& args]
   (do (connect nick host port group-or-chan greeting hear speakup respond)  ))
