;;; CREDIT:
;;; https://github.com/cerner/clara-examples/blob/master/src/main/clojure/clara/examples/insta.clj

(ns bobtailbot.brains.example-shopping.brain
  "Instantly create a rule-based DSL"
  (:require [instaparse.core :as insta]
            [clara.rules.accumulators :as acc]
            [clara.rules :refer :all]
            
            ;;[clara.tools.inspect :as cti]
            
            [clojure.string :as string]
            [clojure.core.async :as async 
               :refer [go-loop <! <!! >! >!!  close! chan pub sub]]
            
            [schema.core :as s]
            
            [bobtailbot.irc :as irc]))


;; IMPORTANT!!!
;; Change this prefix if you change this file's name (or path).
;;Also remember to change the ns declaration.

(def parent-ns "bobtailbot.brains.example-shopping")
(def this-ns-unqual "brain")


(def this-ns (str parent-ns "." this-ns-unqual))
(def ns-prefix (str this-ns "/"))

(def this-dir (str "./src/" (-> parent-ns (string/replace #"\." "/" ) (string/replace #"-" "_")) ))
(def dir-prefix (str this-dir "/" ))





;;;; Facts used in the examples below.

(defrecord Order [year month day])

(defrecord Customer [status])

(defrecord Purchase [cost item])

(defrecord Discount [name percent])

(defrecord Total [value])

(defrecord Promotion [reason type])

(def shopping-grammar
  ;(insta/parser  (slurp "./resources/grammars/shopping/shopping_grammar.ebnf") :auto-whitespace :standard )
  (insta/parser  (slurp (str dir-prefix "grammar.ebnf")) :auto-whitespace :standard )
  )


(def operators {"is" `=
                ">" `>
                "<" `<
                "=" `=})

(def fact-types
  {"customer" Customer
   "total" Total
   "order" Order})

(def shopping-transforms
  {:NUMBER #(Integer/parseInt %)
   :OPERATOR operators
   :FACTTYPE fact-types
   :CONDITION (fn [fact-type field operator value]
                {:type fact-type
                 :constraints [(list operator (symbol field) value)]})

   ;; Convert promotion strings to keywords.
   :PROMOTIONTYPE keyword

   :DISCOUNT (fn [name percent & conditions]
               {:name name
                :lhs conditions

                :rhs `(insert! (->Discount ~name ~percent))})

   :PROMOTION (fn [name promotion-type & conditions]
                {:name name
                 :lhs conditions
                 :rhs `(insert! (->Promotion ~name ~promotion-type))})
                 
                 
    :QUERY  (fn [name] (fn [session-name] (query session-name (if (.contains name ns-prefix) name (str ns-prefix name)))))
   ;; :QUERY (fn [name] (partial (fn [name2 session-name2] (query session-name2 name2)) name))
    
    ; This one ONLY WORKS IN THE REPL, after reloading, otherwise it says session-01 is undefined.
    ;; :QUERY (fn [name] (query session-01 name))
                 
                 })


(def default-fact-list
    [(->Customer "gold")
     (->Order 2013 "august" 20)
     (->Purchase 20 :gizmo)
     (->Purchase 120 :widget)
     (->Purchase 90 :widget)])

(def fact-list (atom default-fact-list))




;; These rules may be stored in an external file or database.
(def default-rule-list

  "discount my-discount 15 when customer status is platinum;
   discount extra-discount 10 when customer status is gold and total value > 200;
   promotion free-widget-month free-widget when customer status is gold and order month is august;")


(def rule-list (atom default-rule-list))



;;;; Rules written in Clojure and combined with externally-defined rules.

(defrule total-purchases
  [?total <- (acc/sum :cost) :from [Purchase]]
  =>
  (insert! (->Total ?total)))
  


;; event rules

(def input-chan (chan))
(def our-pub (pub input-chan :msg-type))

(def output-chan (chan))
(sub our-pub :alert-purchase-gizmo output-chan)


(defrule alert-gizmo-purchase
  "Anyone who purchases a gizmo gets a free lunch."
  [Purchase (= item :gizmo)]
  =>
  (>!! input-chan {:msg-type :alert-purchase-gizmo :text "someone buyed a gizmo!"}))






;; named queries

(defquery get-discounts
  "Returns the available discounts."
  []
  [?discount <- Discount])

(defquery get-promotions
  "Returns the available promotions."
  []
  [?discount <- Promotion])

;;;; Example code to load and validate rules.

(s/defn ^:always-validate load-user-rules :- [clara.rules.schema/Production]
  "Converts a business rule string into Clara productions."
  [business-rules :- s/Str]

  (let [parse-tree (shopping-grammar business-rules)]

    (when (insta/failure? parse-tree)
      (throw (ex-info (print-str parse-tree) {:failure parse-tree})))

    (insta/transform shopping-transforms parse-tree)))



    



(def default-session-01 (-> (mk-session 'bobtailbot.brains.example-shopping.brain (load-user-rules @rule-list))
                    ( #(apply insert %1 %2) @fact-list)
                    (fire-rules)))


(def session-01-a (atom default-session-01))
;(def session-01 default-session-01)



;; this one is useful for the REPL
(def text-01 (str "query " ns-prefix "get-discounts"))
(def text-02 "query get-discounts")

(def text-rule-01 "discount gold-summer-discount 20 when customer status is gold and order month is august;")



;;https://groups.google.com/d/msg/clara-rules/CFvJQGwelo0/NYBMmV9hFAAJ
#_("Will's answer is correct. You'll find the related API documentation here: http://www.clara-rules.org/apidocs/0.12.0/clojure/clara.rules.html#var-retract
We don't have any immediate plans to dynamically add or remove rules from a session, although I could see this happening in the future. I know of at least one use case that handles this by keeping a list of facts they have inserted, and re-building a new session dynamically and inserted those facts into a new session, while the previous gets garbage collected. 
Dynamic rules is something I wouldn't mind adding to Clara, although that comes with some downside. For instance, we'd have to track all facts that didn't match any rule at all (keeping them in memory), whereas today facts that match nothing are simply garbage collected.
-Ryan
")

;(comment 'bobtailbot.insta-clara-shopping)

(defn respond
  "Respond to text"
  [text]
  (let [parsetree  (shopping-grammar text)]
     (case (first (first parsetree))
       :QUERY  ((first (insta/transform shopping-transforms parsetree)) @session-01-a )
       (or :DISCOUNT :PROMOTION) 
           (do (swap! rule-list #(str % text))
           (let [session-03 (-> (mk-session (symbol this-ns) (load-user-rules @rule-list))
                                     ( #(apply insert %1 %2) @fact-list)
                                     (fire-rules))]               
                  (swap! session-01-a (constantly session-03)))
                  
               (str "rules loaded: " (apply str (load-user-rules text))) )
        "unknown input")
     
     
     
     
     
      ))


(defn speakup [speakup-chan]
  (go-loop []
    (let [{:keys [text]} (<! output-chan)]
      (>! speakup-chan text )
      (recur))))


;(defn irc-speakup [socket irc-channel]
     ;(go-loop[] 
       ;(do 
           ;(<! (async/timeout 10000))
           ;(irc/write-privmsg socket "chiming in, smartly, every 10 seconds!" irc-channel)
           ;(recur))))


