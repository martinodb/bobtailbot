(ns bobtailbot.core
  (:require 
  
            [user :as u]
            
            [taoensso.timbre :as timbre   :refer [log  trace  debug  info  warn  error  fatal  report logf tracef debugf infof warnf errorf fatalf reportf  spy get-env]]
            [taoensso.timbre.appenders.core :as appenders]
            
            [taoensso.timbre.tools.logging :as tlog :refer [use-timbre]] ; to use with libraries that use clojure.tools.logging
            
            
            
            [clojure.tools.cli :refer [parse-opts]]
            [clojure.string :as string]
            
            ;; A brain ns must always be: "bobtailbot.brains.<brain-name>.brain"
            [bobtailbot.brains.template.brain ]
            [bobtailbot.brains.general.brain ]
            [bobtailbot.brains.zinc.brain ]
            
            ;; An adapter ns must always be "bobtailbot.adapters.<adapter-name>"
            ;; Also, an adapter must always have a "connect" function as follows: "(<adapter-name>/connect nick host port group-or-chan greeting hear speakup respond)"
            ;; If it doesn't need some of those parameters, it can simpy ignore them.
            [bobtailbot.adapters.repl]
            [bobtailbot.adapters.irc]
            
            
            [bobtailbot.tools :as tools :refer [locals-map keyed]]
            
            
            [clojure.core.async :as async :refer [go-loop <! >! close!]]
            
            [outpace.config :refer [defconfig]])
            
     (:import (java.net InetAddress))
     (:gen-class) )




;;;;;;; logging
(def log-fname "bobtailbot-history.log")

(defn hush-log "send log messages to file and not to stdout"
    [] (timbre/merge-config!
           {:appenders {
              :spit (appenders/spit-appender {:fname log-fname})
              :println {:enabled? false}}}))


(defn say-n-log "send log messages to file and also to stdout"
    [] (timbre/merge-config!
           {:appenders {
              :spit (appenders/spit-appender {:fname log-fname})
              ;:println {:enabled? true}
              
              }}))



(defn clean-log "wipe the log file clean" [] (spit log-fname "") )

(defn setup-tlog "Set up Timbre logging for clojure.tools.logging"
   [] (use-timbre) )






;;;;;;;; GLOBAL CONFIGS ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;;; Default brain and adapter.
(defconfig brain :general) ; use the name of the brain you want, as a keyword. A default is given.
(defconfig adapter :irc)
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;;;;;;;; Default chat config options. They can be overriden by each adapter's options. CLI options override all.
;;;;;;;;; All configs can be set in the file "config.edn"
(defconfig nick "bobtailbot")
(defconfig host "127.0.0.1")
(defconfig port 6667)
(defconfig group-or-chan nick) ; eg: "bobtailbot" ; if a prefix such as "#" is needed, the adapter must add it.
(defconfig greeting "Hello.  Let's chat.")
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;








(defn brainns-str "Get the brain namespace (as string) from brain"
   [brain] (str "bobtailbot.brains." (-> brain (name) (read-string)) ".brain")   )

(defn adapterns-str "Get the adapter namespace (as string) from adapter"
   [adapter] (str "bobtailbot.adapters." (-> adapter (name) (read-string)) )   )


(defn respond-b "Get the respond function of given brain, or respond to given text with given brain"
   ([brain] (fn [text] (load-string (str "(" (brainns-str brain) "/respond " "\"" text "\"" ")"))) )
   ([brain text] ((respond-b brain) text) ) )


(defn hear-b "Get the hear function of given brain, or hear given text with given brain"
   ([brain] (fn [text]   (load-string (str "(" (brainns-str brain) "/hear " "\"" text "\"" ")"))    ) )
   ([brain text] ((hear-b brain) text) ) )

(defn speakup-b "Get the speakup function of given brain, or speakup in given speakup-chan with given brain"
   ([brain] (fn [speakup-chan]  ((load-string (str "(fn [x] " "(" (brainns-str brain) "/speakup "  "x"  ")" ")"  )) speakup-chan)  ) )
   ([brain speakup-chan] ((speakup-b brain) speakup-chan) ) )


;(defn connect-ab "Connect with given adapter and brain"
  ;[adapter brain nick host port group-or-chan greeting ]
    ;((load-string (str "(fn [nick host port group-or-chan greeting hear speakup respond] " "(" (adapterns-str adapter) "/connect "  "nick host port group-or-chan greeting hear speakup respond"  ")" ")"  )) nick host port group-or-chan greeting (hear-b brain) (speakup-b brain) (respond-b brain)) )








(defn connect-ab "Connect with given adapter and brain"
  [{:keys [adapter brain nick host port group-or-chan greeting] :as opts-ab} ]
    (let [hear (hear-b brain)
          speakup (speakup-b brain)
          respond (respond-b brain)
         ]
      (do   ;;; DEBUGGING (println "opts-conn: " {:nick nick :host host :port port :group-or-chan group-or-chan :greeting greeting :hear hear :speakup speakup :respond respond}  "\n" "(respond 'hello'): " (respond "hello"))
        ((load-string (str
        "(fn [{:keys [nick host port group-or-chan greeting hear speakup respond] :as opts-conn}] " 
         "(" (adapterns-str adapter) "/connect "  "opts-conn"  ")" ")"  )) 
        ;{:nick nick :host host :port port :group-or-chan group-or-chan :greeting greeting :hear hear :speakup speakup :respond respond} 
        
        (select-keys (locals-map) [:nick :host :port :group-or-chan :greeting :hear :speakup :respond])
        
          ) ) ) )





(defn get-default "Get the default config option for an adapter"
  [op adapter]
   (let [adapterns-s (adapterns-str adapter)
         adapter-op (load-string (str adapterns-s "/" op))
         global-op (load-string (str "bobtailbot.core" "/" op))
         use-global? (load-string (str adapterns-s "/" "use-global?"))
        ]
    (if use-global? (do global-op)(do adapter-op)) ) )





















;; Adapted from:
;; https://github.com/clojure/tools.cli#example-usage

(def cli-options
  [
   ["-a" "--adapter ADAPTER" "Adapter" :default adapter]
   ["-b" "--brain BRAIN" "Brain" :default brain]
   
   ["-g" "--greeting GREETING" "Greeting" :default (get-default "greeting" adapter)]
   ["-n" "--nick NICK" "Nick" :default (get-default "nick" adapter)]
   ["-H" "--host HOST" "Remote host"
    :default (get-default "host" adapter) ;; configured default host is localhost.
    :default-desc "localhost"
    :parse-fn #(InetAddress/getByName %)]
    
   ["-p" "--port PORT" "Port number"
    :default (get-default "port" adapter)
    :parse-fn #(Integer/parseInt %)
    :validate [#(< 0 % 0x10000) "Must be a number between 0 and 65536"]]
    
    ["-c" "--group-or-chan GROUP-OR-CHAN" "Group or channel" :default (get-default "group-or-chan" adapter)]

   ["-h" "--help" "Help" :default false]])



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
          {:exit-message (usage summary) :ok? true :options options}
      errors ; errors => exit with description of errors
          {:exit-message (error-msg errors)}
      ;; custom validation on arguments
      (and (= 1 (count arguments)) (#{"start" "stop" "status"} (first arguments)))
          {:action (first arguments) :options options}
      (= 0 (count arguments))
          {:action "start" :options options}
      :else ; failed custom validation => exit with usage summary
          {:exit-message (usage summary)})))


(defn dev-exit [status msg]
  (println msg)
  ;(System/exit status)
  (println "Would have exited")
  
  )



(defn exit [status msg]
  (println msg)
  (System/exit status)
  
  )












(defn dev-main [& args] ; to try it in lein repl
  (let [{:keys [action options exit-message ok?] :as value-map} (validate-args args)
         {:keys [brain adapter nick host port group-or-chan greeting ] :as opts-ab} options
         
 
                            ] 
          
          (do (clean-log) ; delete previous log entries. This is useful for testing.
              (setup-tlog) ; use Timbre for clojure.tools.logging
              
              ;;pick one
              ;(hush-log) ; log to file, not to stdout.
              (say-n-log) ; log to file and also to stdout.
              ;;;;
              
              
              ;; DEBUGGING (println "value-map: " value-map) ;; for debugging
              ;; DEBUGGING (println "brain : " brain)
              ;; DEBUGGING (println "adapter: " adapter)
              ;; DEBUGGING (println "opts-ab: " opts-ab)
              (if exit-message  (dev-exit (if ok? 0 1) exit-message) (do))
              (case action
                "start"  (connect-ab opts-ab )
                "stop"   "stop: unimplemented arg. Use 'quit' instead"
                "status" "status: unimplemented arg."
                 (str "No matching for action: " action)  ) )   )  )
                 


(defn -main [& args] ; don't run in lein repl.
  (do (apply dev-main args)
      (exit 0 "closing Bobtailbot ..")
      
       ))






