(ns scratch
 (:require [bobtailbot.brains.example-shopping.brain :refer :all]
           [user :as u :refer :all]
 
           [instaparse.core :as insta]
           [clara.rules.accumulators :as acc]
           [clara.rules :refer :all]
            
           ;; just for development
           [clara.tools.inspect :as cti :refer [inspect]]
           ;; /just for development
            
            
           [clojure.string :as string]
           [clojure.core.async :as async 
              :refer [go-loop <! <!! >! >!!  close! chan pub sub go]]
            
           [schema.core :as sc]
            
           [bobtailbot.irc :as irc]
           
           
           ;[duratom.core :as dac :refer [duratom destroy]]
           
           [clojure.edn :as edn]))




;(let [ new-rule-list (str @rule-list "match customer status is gold;") new-session (-> (mk-session (symbol this-ns) (load-user-rules new-rule-list)) ( #(apply insert %1 %2) @fact-list) (fire-rules))](apply str ((first (insta/transform shopping-transforms parsetree)) new-session )))

(def my-default-fact-list [(->Customer "gold") (->Order 2013 "august" 20) (->Purchase 20 :gizmo) (->Purchase 120 :widget)  (->Purchase 90 :widget)])



;; For Hexchat:

#_(
hi
query get-discounts
discount gold-summer-discount 20 when customer status is gold and order month is august;
match customer status is gold;
)



;; for the repl:

#_(ns bobtailbot.brains.example-shopping.brain) ;; and (ns bobtailbot.core) to go back.

;(require '[clara.tools.inspect :as cti :refer [inspect]])

(def d-parsetree (shopping-grammar default-rule-list))

(def nq-parsetree (shopping-grammar "query get-discounts"))


#_(load-user-rules default-rule-list)

(def p-text "match customer status is gold;")
(def p-parsetree (shopping-grammar p-text))

(def p-new-rule-list (str @rule-list p-text))

(def p-new-session (-> (mk-session (symbol this-ns) (load-user-rules p-new-rule-list)) ( #(apply insert %1 %2) @fact-list) (fire-rules)))

(def p-my-query  (first (insta/transform shopping-transforms p-parsetree)))


#_(apply str ((first (insta/transform shopping-transforms p-parsetree)) p-new-session ))
#_(load-user-rules p-new-rule-list) ; the failure was here. Now it's not.
#_(insta/transform shopping-transforms (shopping-grammar new-rule-list))
#_(shopping-grammar new-rule-list) ; the failure was here too.


;; to use a private function
;; http://christophermaier.name/2011/04/30/not-so-private-clojure-functions/
#_(def get-productions-from-namespace #'clara.macros/get-productions-from-namespace)




#_(require    '[clara.rules.engine :as eng] '[clara.rules.memory :as mem] '[clara.rules.compiler :as com]  '[clara.rules.dsl :as dsl] '[cljs.analyzer :as ana] '[cljs.env :as env] '[schema.core :as sc]      '[clara.rules.schema :as schema] '[clojure.set :as s])

