(ns bobtailbot.core
  (:require 
  
            [user :as u]
            
            [clojure.tools.cli :refer [parse-opts]]
            [clojure.string :as string]
            
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
            
            [outpace.config :refer [defconfig]])
            
     (:import (java.net InetAddress))
     (:gen-class) )


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




;; Adapted from:
;; https://github.com/clojure/tools.cli#example-usage

(def cli-options
  [
   ["-g" "--greeting GREETING" "Greeting" :default greeting]
   ["-b" "--brain BRAIN" "Brain" :default brain]
   ["-a" "--adapter ADAPTER" "Adapter" :default adapter]
   ["-n" "--nick NICK" "Nick" :default nick]
   
   ["-H" "--host HOST" "Remote host"
    :default host ;; configured default host is localhost.
    :default-desc "localhost"
    :parse-fn #(InetAddress/getByName %)]
    
   ["-p" "--port PORT" "Port number"
    :default port
    :parse-fn #(Integer/parseInt %)
    :validate [#(< 0 % 0x10000) "Must be a number between 0 and 65536"]]
    
    ["-c" "--group-or-chan GROUP-OR-CHAN" "Group or channel" :default group-or-chan]

   ["-h" "--help"]])



(defn usage [options-summary]
  (->> ["Bobtailbot"
        ""
        "Usage: lein run [options] [action]"
        ""
        "Options:"
        options-summary
        ""
        "Actions:"
        "  start    launch the bot"
        "  stop     (unimplemented)"
        "  status    (unimplemented)"
        ""
        "Please refer to the project README for more information."]
       (string/join \newline)))


(defn error-msg [errors]
  (str "The following errors occurred while parsing your command:\n\n"
       (string/join \newline errors)))



(defn validate-args
  "Validate command line arguments. Either return a map indicating the program
  should exit (with a error message, and optional ok status), or a map
  indicating the action the program should take and the options provided."
  [args]
  (let [{:keys [options arguments errors summary]} (parse-opts args cli-options)]
    (cond
      (:help options) ; help => exit OK with usage summary
          {:exit-message (usage summary) :ok? true}
      errors ; errors => exit with description of errors
          {:exit-message (error-msg errors)}
      ;; custom validation on arguments
      (and (= 1 (count arguments)) (#{"start" "stop" "status"} (first arguments)))
          {:action (first arguments) :options options}
      (= 0 (count arguments))
          {:action "start" :options options}
      :else ; failed custom validation => exit with usage summary
          {:exit-message (usage summary)})))


(defn exit [status msg]
  (println msg)
  (System/exit status))



(defn -main [& args]
  (let [{:keys [action options exit-message ok?] :as value-map} (validate-args args)
         brain (:brain options)
         adapter (:adapter options)
         
         brainns-str (str "bobtailbot.brains." (-> brain  (name) (str) (string/trim) ) ".brain") ; 
         adapterns-str (str "bobtailbot.adapters." (-> adapter  (name) (str) (string/trim) ) ) ; I shouldn't need string/trim, but I do, for some reason.
         
         respond  (fn [text] (load-string (str "(" brainns-str "/respond " "\"" text "\"" ")")) )
         hear (fn [text] (load-string (str "(" brainns-str "/hear " "\"" text "\"" ")")) )
         speakup (fn [speakup-chan] ((load-string (str "(fn [x] " "(" brainns-str "/speakup "  "x"  ")" ")"  )) speakup-chan)  )
         
         connect (fn [nick host port group-or-chan greeting hear speakup respond]
                     ((load-string (str "(fn [nick host port group-or-chan greeting hear speakup respond] "
                                        "(" adapterns-str "/connect "
                                        "nick host port group-or-chan greeting hear speakup respond"  ")" ")"  ) )
                           nick host port group-or-chan greeting hear speakup respond)  )
         
          ]
    (if exit-message
      (exit (if ok? 0 1) exit-message)
      (do (println "value-map: " value-map) ;; for debugging
          (println "brain : " brain)
          (println "adapter: " adapter)
          (println "brainns-str: " brainns-str)
          (println "adapterns-str: " adapterns-str)
          (println "respond: " respond)
          ;(println "respond to question: " (respond "who does Mary love?"))
          
          (println "hear: " hear)
          (println "speakup: " speakup)
          (println "connect: " connect)
          
          
          (case action
           "start"  (connect (:nick options) (:host options) (:port options) (:group-or-chan options) (:greeting options) hear speakup respond  )
           "stop"   "stop: unimplemented arg. Use 'quit' instead"
           "status" "status: unimplemented arg.") ) )  )   )


