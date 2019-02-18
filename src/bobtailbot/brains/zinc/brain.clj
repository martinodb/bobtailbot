(ns bobtailbot.brains.zinc.brain
  "CSNePS as backend"
  (:require [clojure.string :as string]
            [clojure.core.async :as async 
               :refer [go-loop <! <!! >! >!!  close! chan pub sub]]
            
            
            
            
            
            ;[csneps.core.snuser :as s]
            [zinc.core.snuser :as s :refer [adopt-rule adopt-rules allTerms  ask askif askifnot askwh assert assert! assertAll clearkb defineCaseframe defineSlot defineTerm defineType describe-terms exit find-term goaltrace  krnovice list-focused-inference-tasks list-variables list-terms listkb load  nogoaltrace quit unassert] ]
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
        [zinc.core.printer :only (writeKBToTextFile)]
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







(def last-utterance (atom {}))
;;;



(def mode (atom {:hear true :speakup true }))




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










(defn respond-raw [text] "evaluate commands in this ns"
(->> text (#(str "(do (in-ns '" this-ns ")" % ")"))
          (#(try (load-string %) (catch Exception e (str "caught exception: " (.getMessage e))) ) ) 
          (#(or % "OK")) ))








;; Only use for repl and similar, single-user interfaces. It's syncronous (blocking). 
;(def respond respond-stub)
(def respond respond-raw)





(defn hear-normal [text-in] (future 
                        (reset! last-utterance
                           {:type :response , :text (respond text-in)} )))


(defn speakup-top [speakup-chan] (add-watch last-utterance :utt-ready
                                          (fn [k r old-state new-state]
                                                (if (= (:speakup @mode) true) (>!! speakup-chan (:text new-state) ) nil ))))
                                                


(defn hear-top "top-level hear function"
 [text-in]
 (cond
  (= text-in "bot listen") (do (swap! mode assoc :hear true) (swap! mode assoc :speakup true) (reset! last-utterance {:type :response , :text "OK, listening"}))
  (= text-in "bot standby") (do (reset! last-utterance {:type :response , :text "OK, standby mode. Say 'bot listen' to wake up"}) (swap! mode assoc :hear false) (swap! mode assoc :speakup false)  )
  (= (:hear @mode) true) (hear-normal text-in)
  (= (:hear @mode) false) nil
  :else (do (println "problem with hear-top"))
   ))



;;; Only use "hear" and "speakup" for multi-user interfaces like irc. The bot may report events asyncronously, not just respond to questions.
(def hear hear-top)
(def speakup speakup-top )

