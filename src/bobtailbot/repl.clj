(ns bobtailbot.repl
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


