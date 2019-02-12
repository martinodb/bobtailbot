(ns bobtailbot.brains.quick-and-dirty.brain
  "Only basic I/O functionality"
  (:require [clojure.string :as string]
            [clojure.core.async :as async 
               :refer [go-loop <! <!! >! >!!  close! chan pub sub]]
            
            ;[instaparse.core :as insta]
            ;[schema.core :as s]
            ))


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



(defn respond-sync [text]
  (if (question-mark? text) "Nice question."
        (case text 
         "alert on"  (do (reset! alert-wanted? true)
                         (if (not @init-alert-gen?) alert-generator )
                         "alert is now on!"
                          )
         "alert off" (do (reset! alert-wanted? false)
                          "alert is now off!")
         "I see.")))



;;; Only use "hear" and "speakup" for multi-user interfaces like irc. The bot may report events asyncronously, not just respond to questions.
(defn hear [text-in] (future 
                        (reset! last-utterance
                           {:type :response , :text (respond-sync text-in)} )))


(defn speakup [speakup-chan] (add-watch last-utterance :utt-ready
                                          (fn [k r old-state new-state]
                                                (>!! speakup-chan (:text new-state) ))))

;; Only use for repl and similar, single-user interfaces. It's syncronous (blocking). 
(def respond respond-sync)
