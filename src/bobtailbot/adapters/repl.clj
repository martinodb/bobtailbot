(ns bobtailbot.adapters.repl
  (:require [outpace.config :refer [defconfig]])
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
    (if greeting (do (println greeting)
                     (flush)))
    (print ">> ")
    (flush))
  (let [input (read-line)]
    (if-not (= input "quit")
     (do
       ;(println (try (respond-fn input)
                     ;(catch Exception e (do (str "Sorry: " e " - " (.getMessage e)))  )))
       (println (respond input)    )
       (recur nil respond))
     (do (println "Bye!")
         ;(System/exit 0)
         ))))


(defn connect [{:keys [nick host port group-or-chan greeting hear speakup respond]}] ;[nick host port group-or-chan greeting hear-fn speakup-fn respond-fn] ; only greeting and respond-fn matter; the rest will be ignored.
 (launch-repl greeting respond))
