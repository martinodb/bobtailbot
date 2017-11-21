(ns bobtailbot.brains.quick-and-dirty.brain
  "Instantly create a rule-based DSL"
  (:require 
            [instaparse.core :as insta]
            [clojure.string :as string]
            [clojure.core.async :as async 
               :refer [go-loop <! <!! >! >!!  close! chan pub sub]]
            
            [schema.core :as s]
            
            [bobtailbot.irc :as irc]))


;; IMPORTANT!!!
;; Change this prefix if you change this file's name (or path).
;;Also remember to change the ns declaration.

(def parent-ns "bobtailbot.brains.quick-and-dirty")
(def this-ns-unqual "brain")


(def this-ns (str parent-ns "." this-ns-unqual))
(def ns-prefix (str this-ns "/"))

(def this-dir (str "./src/" (-> parent-ns (string/replace #"\." "/" ) (string/replace #"-" "_")) ))
(def dir-prefix (str this-dir "/" ))
;;;;;




(defn question-mark? [text] (re-find  #"\?$" text) )
(defn respond [text]
  (if (question-mark? text) "Nice question."  "I see.") )


(defn speakup [speakup-chan]
     (go-loop[] 
       (do 
           (<! (async/timeout 5000))
           (>! speakup-chan "chiming in every 5 seconds!" )
           (recur))))
