(ns bobtailbot.brains.zinc.brain
  "CSNePS as backend"
  (:require
   [taoensso.timbre :as timbre   :refer []]
   [bobtailbot.tools :as tools :refer [tim-ret]]
   
   [clojure.string :as string]
   [clojure.core.async :as async :refer [go-loop <! <!! >! >!!  close! chan pub sub]]

   ;[csneps.core.snuser :as s]
   
   [zinc.core.snuser :as s :refer [adopt-rule adopt-rules allTerms  ask askif askifnot askwh assert assert! assertAll clearkb defineCaseframe defineSlot defineTerm defineType describe-terms exit find-term goaltrace  krnovice list-focused-inference-tasks list-variables list-terms listkb   nogoaltrace quit unassert] ]
   [zinc.MOD-logging :as Mlogg :refer [wdm wtim wlog wdm2 wtim2 wlog2]] ;; added by martinodb.
   
   
            ;[instaparse.core :as insta]
            ;[schema.core :as sc]
   )
     
     
     (:require [zinc.MOD-logging :as Mlogg :refer [wdm wtim wlog wdm2 wtim2 wlog2]] ;; added by martinodb.
            [zinc.core.contexts :as ct]
            [zinc.core.caseframes :as cf]
            [zinc.core.relations :as slot]
            [zinc.core :as zinc]
            [zinc.core.build :as build]
            [zinc.snip :as snip]
            [zinc.gui :as gui]
            [zinc.utils.ontology :as onto-tools]
            [clojure.tools.cli :refer [parse-opts]]
            [reply.main])
     
      (:use clojure.stacktrace)
     
      (:refer-clojure :exclude [+ - * / < <= > >= == not= assert find load exit quit])
     
     
     
     
     
     
     
     (:use [clojure.pprint :only (cl-format)]
        [clojure.core.memoize :only (memo-clear!)]
        [clojure.walk]
        [zinc.core.caseframes :only (list-caseframes sameFrame description)]
        [zinc.demo :only (demo)]
        [clojure.set :only (union difference)]
        [zinc.core.relations :only (list-slots)]
        [zinc.core.contexts :only (currentContext defineContext listContexts setCurrentContext remove-from-context)]
        
        ;[zinc.core.build :only (find *PRECISION* defrule unassert rewrite-propositional-expr)]
        [zinc.core.build :only (find *PRECISION* defrule rewrite-propositional-expr)] ; remove 'unassert'.
        
        [zinc.core :only (showTypes list-types semantic-type-of)]
        [zinc.core.printer :as zcp :only (writeKBToTextFile)]
        [zinc.snip :only (definePath pathsfrom cancel-infer-of cancel-infer-from cancel-focused-infer adopt unadopt attach-primaction ig-debug-all)]
        [zinc.core.arithmetic]
        [zinc.util]
        [zinc.debug :only (debug set-debug-nodes set-debug-features)])
       
       (:import [edu.buffalo.csneps.util CountingLatch])
            )



;;; The following snippet should get the ns and dir for this brain and its data without modification:
;;;;;
(def D (symbol ::XXXDummy))
(def this-ns-sym (-> #'D meta :ns .name))
(def this-ns-str (str this-ns-sym))
(def this-ns-unqual-str (re-find  (re-pattern "[^\\.]+$") this-ns-str))
(def parent-ns-str (re-find  (re-pattern ".*(?=\\.)") this-ns-str))
(def parent-ns parent-ns-str)
(def this-dir-tail (re-find (re-pattern ".*(?=\\/[^\\/]+$)")  (-> #'D meta :file)))
(def this-dir (str "./src/" this-dir-tail))
(def data-this-dir (str "./data/" this-dir-tail))
;;;;;;;;;;;;
;;; Actually used:
(def this-ns this-ns-str) ; eg: "bobtailbot.brains.general.brain"
(def ns-prefix (str this-ns "/")) ; eg: "bobtailbot.brains.general.brain/"
(def dir-prefix (str this-dir "/" )) ; eg: "./src/bobtailbot/brains/general/"
(def data-dir-prefix (str data-this-dir "/")) ; eg: "./data/bobtailbot/brains/general/"
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(def unsup-repl "Sorry, this option is not supported in syncronous UIs like repl, only in asyncronous ones like IRC")

(def default-kb-file (str data-dir-prefix "store/dkbf.sneps")) ; default kb file (factory settings).
(def curr-kb-file (str data-dir-prefix "store/ckbf.sneps")) ; current kb file (changes saved here by default).

(defn load-from-store "load <filename> from zinc brain store"
[filename]
(s/load (str data-dir-prefix "store/" filename))
)


(def last-utterance (atom {}))
;;;



(def mode (atom {:hear true , :speakup true , :resp-mode :raw}))




(def init-alert-gen? (atom false))
(def alert-wanted? (atom false))
(defn question-mark? [text] (re-find  #"\?$" text) )
(def alert-generator

     (go-loop[]
             (if @alert-wanted? (do (<! (async/timeout 3000) )
                                    (reset! last-utterance
                                      {:type :alert , :text "chiming in every 3 seconds!"})))
             (recur)))






(defn respond-stub [text]
  (if (question-mark? text) "Nice question."
        (case text 
         "alert on"  (do (reset! alert-wanted? true)
                         (if (not @init-alert-gen?) alert-generator )
                         "alert is now on!"
                          )
         "alert off" (do (reset! alert-wanted? false)
                          "alert is now off!")
         "I see.")))





(defn respond-raw "evaluate commands in this ns" [text] 
(->> text (#(str "(do (in-ns '" this-ns ")" % ")"))
          (#(try (load-string %) (catch Exception e (str "caught exception: " (.getMessage e))) ) ) 
          (#(or % "OK")) ))



(defn respond-top "top-level respond function"
[text]
(cond
  (contains? #{"bot listen" "bot standby" "bot deaf" "bot mute"} text) (do unsup-repl) ; in async UIs these options are managed in hear-top. In sync UIs like repl they are not supported.
  (= text "bot resp-mode raw") (do (swap! mode assoc :resp-mode :raw) "OK, raw resp-mode. Say 'bot resp-mode <mode-name>' to switch"   )
  (= text "bot resp-mode stub") (do (swap! mode assoc :resp-mode :stub) "OK, stub resp-mode. Say 'bot resp-mode <mode-name>' to switch"  )
  
  (= text "bot save") (do (respond-raw "(zcp/writeKBToTextFile curr-kb-file )") )
  (= text "bot load default") (do (respond-raw "(s/load default-kb-file )"))
  (= text "bot clearkb") (do (respond-raw "(s/clearkb true)") )
  (= text "bot load") (do (respond-raw "(s/load curr-kb-file )"))
  
  
  (= (:resp-mode @mode) :raw) (respond-raw text)
  (= (:resp-mode @mode) :stub) (respond-stub text)
  :else "Oops! Problem with my respond function"
  ))




(def respond respond-top) ;; Only use 'respond' for repl and similar, single-user interfaces. It's syncronous (blocking). 

(defn hear-normal [text-in] (future 
                        (reset! last-utterance
                           {:type :response , :text (respond text-in)} )))


(defn speakup-top [speakup-chan] (add-watch last-utterance :utt-ready
                                          (fn [k r old-state new-state]
                                                (if (= (:speakup @mode) true) (>!! speakup-chan (:text new-state) ) nil ))))


(defn hear-top "top-level hear function"
 [text]
 (cond
  (= text "bot listen") (do (swap! mode assoc :hear true) (swap! mode assoc :speakup true) (reset! last-utterance {:type :response , :text "OK, listening"}) nil)
  (= text "bot standby") (do (reset! last-utterance {:type :response , :text "OK, standby mode. Say 'bot listen' to wake up"}) (swap! mode assoc :hear false) (swap! mode assoc :speakup false) nil )
  (= text "bot deaf") (do (reset! last-utterance {:type :response , :text "OK, deaf mode. Say 'bot listen' to go back to normal"}) (swap! mode assoc :hear false) nil  )
  (= text "bot mute") (do (reset! last-utterance {:type :response , :text "OK, mute mode. Say 'bot listen' to go back to normal"})  (swap! mode assoc :speakup false) nil )
  
  
  (= (:hear @mode) true) (hear-normal text)
  (= (:hear @mode) false) nil
  :else (do (timbre/info "problem with hear-top"))
   ))




;;; Only use "hear" and "speakup" for multi-user interfaces like irc. The bot may report events asyncronously, not just respond to questions.
(def hear hear-top)
(def speakup speakup-top )
