(ns bobtailbot.adapters.repl
  (:require
   
   [taoensso.timbre :as timbre   :refer [log  trace  debug  info  warn  error  fatal  report logf tracef debugf infof warnf errorf fatalf reportf  spy get-env]]
   [bobtailbot.tools :as tools :refer [tim-ret tim-println] ]
   
   [outpace.config :refer [defconfig]]
   )
  (:gen-class)
  )





;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;;;;;;;; Adapter-specific chat config options. They override global options if "use-global?" is set to false. CLI options override all.
;;;;;;;;; All configs can be set in the file "config.edn"
(defconfig use-global? false) ; true: use global configs. false: override global configs.
(defconfig nick "bobtailbot")
(defconfig host "127.0.0.1")
(defconfig port 6667)
(defconfig group-or-chan nick) ; eg: "bobtailbot" ; if a prefix such as "#" is needed, the adapter must add it.
(defconfig greeting "Hello.  Let's chat.")
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;








(defn launch-repl [greeting respond]
  (do
    (if greeting (do (tim-println greeting)
                     (flush)))
    (print ">> ")
    (flush))
  (let [input (read-line)]
    (if-not (= input "quit")
     (do
       
       (try (tim-println (respond input)) 
            (catch Exception e (tim-println (str "Sorry: " e " - " (.getMessage e)))))
       (recur nil respond))
     (do (tim-println "Bye!")
         ;(System/exit 0)
         ))))


(defn connect [{:keys [ greeting respond]}]  ;;; only greeting and respond matter. The others (nick host port group-or-chan greeting) will be ignored. 
 (launch-repl greeting respond))
