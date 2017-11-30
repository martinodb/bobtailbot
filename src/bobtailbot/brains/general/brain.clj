;;; CREDIT:
;;; https://github.com/cerner/clara-examples/blob/master/src/main/clojure/clara/examples/insta.clj

(ns bobtailbot.brains.general.brain
  "General chatbot"
  (:require [instaparse.core :as insta]
            [clara.rules.accumulators :as acc]
            [clara.rules :refer :all]
            
            ;; just for development
            [clara.tools.inspect :as cti :refer [inspect]]
            ;; /just for development
            
            
            [clojure.string :as string]
            [clojure.core.async :as async 
               :refer [go-loop <! <!! >! >!!  close! chan pub sub go]]
            
            [schema.core :as sc]
            
            
            ;[duratom.core :as dac :refer [duratom destroy]]
            [bobtailbot.tools :as tools :refer :all]
            
            
            [clojure.edn :as edn]))


;; IMPORTANT!!!
;; Change this prefix if you change this file's name (or path).
;;Also remember to change the ns declaration.
(def parent-ns "bobtailbot.brains.general")
(def this-ns-unqual "brain")
;;;;;


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


;https://www.compoundtheory.com/clojure-edn-walkthrough/
(def edn-readers 
    { 'bobtailbot.brains.example_shopping.brain.Order map->Order
      'bobtailbot.brains.example_shopping.brain.Customer map->Customer
      'bobtailbot.brains.example_shopping.brain.Purchase map->Purchase
      'bobtailbot.brains.example_shopping.brain.Discount map->Discount
      'bobtailbot.brains.example_shopping.brain.Total map->Total
      'bobtailbot.brains.example_shopping.brain.Promotion map->Promotion
     
    })




(def shopping-grammar
  (insta/parser  (slurp (str dir-prefix "grammar.ebnf")) :auto-whitespace :standard ))


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
                 :constraints [(list operator (symbol field) value)]
                 })
   :QCONDITION (fn [fact-type field operator value]
                {:type fact-type
                 :constraints [(list operator (symbol field) value)]
                 :fact-binding :?thing
                 })
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
                 
                 
    :NQUERY  (fn [name] (fn [session-name] (query session-name (if (.contains name ns-prefix) name (str ns-prefix name)))))
    :QUERY   (fn [& conditions]
                 {:name "anon-query"
                  :lhs conditions
                  
                  :params #{}
                  })
    :F-CUSTOMER (fn [status] (->Customer status))
    :F-ORDER    (fn [year month day] (->Order year month day))
    :F-PURCHASE (fn [cost item] (->Purchase cost item))
                 
                 })





(def default-fact-set
  (set [(->Customer "gold")
        (->Order 2013 "august" 20)
        (->Purchase 20 :gizmo)
        (->Purchase 120 :widget)
        (->Purchase 90 :widget)]) )


(def fact-set (disk-ref (str dir-prefix "store/fact_set.edn") default-fact-set edn-readers))


;; These rules may be stored in an external file or database.
(def default-rule-list

  "discount my-discount 15 when customer status is platinum;
   discount extra-discount 10 when customer status is gold and total value > 200;
   promotion free-widget-month free-widget when customer status is gold and order month is august;")



(def rule-list (disk-ref (str dir-prefix "store/rule_list.edn") default-rule-list edn-readers))



;;;; Rules written in Clojure and combined with externally-defined rules.

(defrule total-purchases
  [?total <- (acc/sum :cost) :from [Purchase]]
  =>
  (insert! (->Total ?total)))
  


;; event rules

(def last-utterance (atom {}))

(defrule alert-gizmo-purchase
  "Anyone who purchases a gizmo gets a free lunch."
  [Purchase (= item :gizmo)]
  =>
  (reset! last-utterance
                  {:type :alert , :text "someone bought a gizmo!"}))



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

(sc/defn ^:always-validate load-user-rules-safe :- [clara.rules.schema/Production]
  "Converts a business rule string into Clara productions. Safe version."
  [business-rules :- sc/Str]

  (let [parse-tree (shopping-grammar business-rules)]

    (when (insta/failure? parse-tree)
      (throw (ex-info (print-str parse-tree) {:failure parse-tree})))

    (insta/transform shopping-transforms parse-tree)))


(defn load-user-rules-unsafe 
  "Converts a business rule string into Clara productions. Unsafe version."
  [business-rules]

  (let [parse-tree (shopping-grammar business-rules)]

    (when (insta/failure? parse-tree)
      (throw (ex-info (print-str parse-tree) {:failure parse-tree})))

    (insta/transform shopping-transforms parse-tree)))

(def load-user-rules load-user-rules-safe)
(def load-user-facts load-user-rules-unsafe)










(def default-session (-> (mk-session 'bobtailbot.brains.example-shopping.brain (load-user-rules @rule-list))
                    ( #(apply insert %1 %2) @fact-set)
                    (fire-rules)))



(def curr-session (ref default-session))



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



(defn  respond-sync
  "Respond to text"
 [text]
  (let [parsetree  (shopping-grammar text)
         intype (first (first parsetree))]
   (cond 
     (= intype :NQUERY)
        (try
          (apply str
            ((first (insta/transform shopping-transforms parsetree)) @curr-session ))
          (catch Exception e (do (println (.getMessage e))
                                         "That's not a valid query." )))
       (= intype :QUERY )
         (try
           (do 
             (let [ new-rule-list (str @rule-list text)
                    new-session   (-> (mk-session (symbol this-ns)
                                        (load-user-rules new-rule-list))
                                        ( #(apply insert %1 %2) @fact-set)
                                        (fire-rules))
                    anon-query  (first (insta/transform shopping-transforms parsetree))]
                (apply str (query new-session anon-query))))
            (catch Exception e (do (println (.getMessage e)) "That's not a valid query." )))
       (= intype (or :DISCOUNT :PROMOTION)) (dosync (alter rule-list #(str % text))
                                              (let [new-session (-> (mk-session (symbol this-ns)
                                                (load-user-rules @rule-list))
                                                ( #(apply insert %1 %2) @fact-set)
                                                (fire-rules))]
                                                (ref-set curr-session new-session))
                                              (str "rules loaded: " (apply str (load-user-rules text))))
       (= intype (or :F-CUSTOMER :F-ORDER :F-PURCHASE))
                            (dosync  (alter fact-set #(into #{} (reduce conj % (load-user-facts text))))
                                    ;(alter fact-set #(into [] (concat % (load-user-facts text))))
                                    (let [new-session (-> @curr-session 
                                                        (#(apply insert %1 %2) @fact-set)
                                                        (fire-rules))]
                                          (ref-set curr-session new-session))
                                    (str "facts added: " (pr-str (load-user-facts text))))
        :else "unknown input")))



;;; Only use "hear" and "speakup" for multi-user interfaces like irc. The bot may report events asyncronously, not just respond to questions.
(defn hear [text-in] (future 
                        (reset! last-utterance
                           {:type :response , :text (respond-sync text-in)} )))


(defn speakup [speakup-chan] (add-watch last-utterance :utt-ready
                                          (fn [k r old-state new-state]
                                                (>!! speakup-chan (:text new-state) ))))

;; Only use for repl and similar, single-user interfaces. It's syncronous (blocking). 
(def respond respond-sync)
