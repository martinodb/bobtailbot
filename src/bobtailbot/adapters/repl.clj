(ns bobtailbot.adapters.repl
  (:gen-class)
  )

(defn launch-repl [greeting respond-fn]
  (do
    (if greeting (do (println greeting)
                     (flush)))
    (print ">> ")
    (flush))
  (let [input (read-line)]
    (if-not (= input "quit")
     (do
       (println (try (respond-fn input)
                     (catch Exception e (str "Sorry: " e " - " (.getMessage e)))))
       (recur nil respond-fn))
     (do (println "Bye!")
         ;(System/exit 0)
         ))))


(defn connect [nick host port group-or-chan greeting hear-fn speakup-fn respond-fn] ; only greeting and respond-fn matter; the rest will be ignored.
 (launch-repl greeting respond-fn))
