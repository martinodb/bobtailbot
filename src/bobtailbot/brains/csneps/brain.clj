(ns bobtailbot.brains.csneps.brain
  "CSNePS as backend"
  (:require [clojure.string :as string]
            [clojure.core.async :as async 
               :refer [go-loop <! <!! >! >!!  close! chan pub sub]]
            
            
            
            ;https://nrepl.org/nrepl/hacking_on_nrepl.html
            [nrepl.core :as nrepl]
            
            ;[instaparse.core :as insta]
            ;[schema.core :as s]
            ))


;; IMPORTANT!!!
;; Change this prefix if you change this file's name (or path).
;;Also remember to change the ns declaration.
(def parent-ns "bobtailbot.brains.csneps")
(def this-ns-unqual "brain")
;;;;;;;
(def this-ns (str parent-ns "." this-ns-unqual))
(def ns-prefix (str this-ns "/"))
(def this-dir (str "./src/" (-> parent-ns (string/replace #"\." "/" ) (string/replace #"-" "_")) ))
(def dir-prefix (str this-dir "/" ))

;;;
(def last-utterance (atom {}))
;;;


(def init-alert-gen? (atom false))
(def alert-wanted? (atom false))

(defn question-mark? [text] (re-find  #"\?$" text) )

(def alert-generator

     (go-loop[]
             (if @alert-wanted? (do (<! (async/timeout 3000) )
                                    (reset! last-utterance
                                      {:type :alert , :text "chiming in every 3 seconds!"})))
             (recur)))



(def mytext-1 "hello, one")

(def mytext-2 "(str \"hello\")" )


(def let-example (let [text3 (str "(str \\\"" mytext-1 "\\\")") ]   (str text3) ))



(defn respond-sync-stub [text]
  (if (question-mark? text) "Nice question."
        (case text 
         "alert on"  (do (reset! alert-wanted? true)
                         (if (not @init-alert-gen?) alert-generator )
                         "alert is now on!"
                          )
         "alert off" (do (reset! alert-wanted? false)
                          "alert is now off!")
         "I see.")))

(defn respond-sync-csneps [text]
  (with-open
     ;actual csneps system (port is different each time):
     [conn (nrepl/connect  :port 34289)]
     
     ; minimal nrepl server, not csneps.
     ;[conn (nrepl/connect :port 7888)]
     
     
     (-> (nrepl/client conn 1000)
       
       
       
       
       (nrepl/message  {:op :eval :code text})
       
       
       nrepl/response-values
       
       
       first
       
       
       )))

(defn respond-sync-csneps-outstr [text]
  (with-out-str 
     (with-open
      ;actual csneps system (port is different each time):
      [conn (nrepl/connect  :port 34289)]
     
      ; minimal nrepl server, not csneps.
      ;[conn (nrepl/connect :port 7888)]
     
     
      (-> (nrepl/client conn 1000)
        
        
        
        
        (nrepl/message  {:op :eval :code text})
        
        
        nrepl/response-values
        
        
        first
        
        
        ))))




(defn respond-sync-csneps-ws
 "wrap with string quotes all expressions that don't have a leading parenthesis"
[text]
(cond
(= (first text) (first "(") ) (do (println (first text) " is a paren" ) (respond-sync-csneps text))
:else  (respond-sync-csneps (do  (println (first text) "is not a paren") (pr-str (str  text )))  )))


;; Only use for repl and similar, single-user interfaces. It's syncronous (blocking). 
(def respond respond-sync-csneps-ws)


;;; Only use "hear" and "speakup" for multi-user interfaces like irc. The bot may report events asyncronously, not just respond to questions.
(defn hear [text-in] (future 
                        (reset! last-utterance
                           {:type :response , :text (respond text-in)} )))


(defn speakup [speakup-chan] (add-watch last-utterance :utt-ready
                                          (fn [k r old-state new-state]
                                                (>!! speakup-chan (:text new-state) ))))

