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

(defrecord Triple [name affirm subj verb obj])


;https://www.compoundtheory.com/clojure-edn-walkthrough/
(def shop-edn-readers 
    { ;'bobtailbot.brains.general.brain.Order map->Order
      (symbol (str this-ns "." "Order")) map->Order
      (symbol (str this-ns "." "Customer")) map->Customer
      (symbol (str this-ns "." "Purchase")) map->Purchase
      (symbol (str this-ns "." "Discount")) map->Discount
      (symbol (str this-ns "." "Total")) map->Total
      (symbol (str this-ns "." "Promotion")) map->Promotion
    })




;https://www.compoundtheory.com/clojure-edn-walkthrough/
(def g-edn-readers 
    { (symbol (str this-ns "." "Triple")) map->Triple }
    )




(def default-verb-set (set [
  {:inf "walk", :past "walked", :pp "walked" , :er "walker", :ing "walking", :pres3 "walks"}
  {:inf "talk", :past "talked", :pp "talked" , :er "talker", :ing "talking", :pres3 "talks"}
  {:inf "breath", :past "breathed", :pp "breathed" , :er "breather", :ing "breathing", :pres3 "breathes"}
  {:inf "love", :past "loved", :pp "loved" , :er "lover", :ing "loving", :pres3 "loves"}
  {:inf "hate", :past "hated", :pp "hated" , :er "hater", :ing "hating", :pres3 "hates"}
  {:inf "kiss", :past "kissed", :pp "kissed" , :er "kisser", :ing "kissing", :pres3 "kisses"}
  {:inf "slap", :past "slapped", :pp "slapped" , :er "slapper", :ing "slapping", :pres3 "slaps"}
  {:inf "eat", :past "ate", :pp "eaten" , :er "eater", :ing "eating", :pres3 "eats"}
  {:inf "drink", :past "drank", :pp "drunk" , :er "drinker", :ing "drinking", :pres3 "drinks"}
  ]))

(def verb-set-edn-readers {})

(def verb-set (disk-ref (str dir-prefix "store/verb_set.edn") default-verb-set verb-set-edn-readers ))


(def Vinf (set (map :inf @verb-set)))
(def Vpast (set (map :past @verb-set)))
(def Vpp (set (map :pp @verb-set)))
(def Ver (set (map :er @verb-set)))
(def Ving (set (map :ing @verb-set)))
(def Vpres3 (set (map :pres3 @verb-set)))



(def default-noun-set (set [
  {:sing "thing", :plural "things"}
  {:sing "object", :plural "objects"}
  {:sing "animal", :plural "animals"}
  {:sing "person", :plural "people"}
  {:sing "man", :plural "men"}
  {:sing "woman", :plural "women"}
  ]))

(def noun-set-edn-readers {})
(def noun-set (disk-ref (str dir-prefix "store/noun_set.edn") default-noun-set noun-set-edn-readers ))
(def Nsimp-sg (set (map :sing @noun-set)))



(def default-adj-set (set [
  {:a "heavy"}
  {:a "tall"}
  {:a "hard"}
  {:a "good"}
  {:a "great"}

  ]))

(def adj-set-edn-readers {})
(def adj-set (disk-ref (str dir-prefix "store/adj_set.edn") default-adj-set adj-set-edn-readers ))
(def Adj (set (map :a @adj-set)))











(defn ebnify-wtail [coll] (apply str (map #(str "'" %1 "'"   " | ") coll)) )
(defn ebnify-notail [coll] (str (apply str (map #(str "'" %1 "'"   " | ") (butlast coll))) " " "'" (last coll) "'" " " ) )

; example:
; (apply str (map #(str "'" %1 "'"   " |") Vpres3))
; "'walks' |'loves' |'eats' |'hates' |'drinks' |'breathes' |'talks' |'kisses' |'slaps' |"


(def g-grammar-1-annex (str
" VtraInfOrPresNon3 = "  "( " (ebnify-notail Vinf)  " ); "


;"\n VtraPast = "  "(" (ebnify-notail Vpast)  "); \n"
;"\n VtraPP = "  "(" (ebnify-notail Vpp)  "); \n"
;"\n VtraER = "  "(" (ebnify-notail Ver)  "); \n"

" GERUNDtra = "  "( " (ebnify-notail Ving)  " ); "
" VtraPres3 = "  "( " (ebnify-notail Vpres3)  " ); "

" <Nsimp-sg> = "  "( " (ebnify-notail Nsimp-sg)  " ); "

" <Adj> = "  "( " (ebnify-notail Adj)  " ); "


"\n"

))



(def raw-g-grammar-1 (slurp (str dir-prefix "g-grammar-1.ebnf")) )
(def raw-g-grammar-1-w-annex (str raw-g-grammar-1 g-grammar-1-annex))


(def grammar-martintest
  (insta/parser  (slurp (str dir-prefix "grammar-martintest.ebnf")) :auto-whitespace :standard ))

(def g-grammar-1
  (insta/parser raw-g-grammar-1-w-annex  :auto-whitespace :standard ))

(def shop-grammar
  (insta/parser  (slurp (str dir-prefix "shop-grammar.ebnf")) :auto-whitespace :standard ))




;(def g-grammar shop-grammar)
(def g-grammar g-grammar-1)
;(def g-grammar grammar-martintest)


(defn parsed-voc-map [parsetree] (read-string (apply str (rest (nth (first parsetree) 2 )))))
;bobtailbot.brains.general.brain=> (apply str (rest (nth (first (g-grammar-1 "add verb {:my balls  :are  fresh };")) 2 )))
;"{:my balls  :are  fresh }"





(def shop-operators {"is" `=
                ">" `>
                "<" `<
                "=" `=})


(def g-ts-operators {"equals" `=
                "greater than" `>
                ">" `>
                "lower than" `<
                "<" `<
                "=" `=})



(def shop-fact-types
  {"customer" Customer
   "total" Total
   "order" Order})

(def shop-transforms
  {:NUMBER #(Integer/parseInt %)
   :OPERATOR shop-operators
   :FACTTYPE shop-fact-types
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



(def g-transforms
  {:NUMBER #(Integer/parseInt %)
   :TS-OPERATOR g-ts-operators
   :ANON-RULE-notest (fn [fact & facts]
                       {:ns-name (symbol this-ns)
                        :name (str ns-prefix "my-anon-rule")
                        :lhs facts
                        :rhs `(insert! ~fact)})
                 
    :TRIP-FACT-IND2 (fn [t-subj t-verb t-obj]
                      `(->Triple "my-fact" true ~t-subj ~t-verb ~t-obj )
                       )
    :R-TRIP-FACT-IND2 (fn [t-subj t-verb t-obj]
                      {:type Triple
                       :constraints [(list '= t-subj 'subj)
                                     (list '= t-verb 'verb)
                                     (list '= t-obj 'obj)
                                       ]
                        })
    :Q-TRIP-FACT-IND2 (fn [t-subj t-verb t-obj]
                      {:type Triple
                       :constraints [(list '= t-subj 'subj)
                                     (list '= t-verb 'verb)
                                     (list '= t-obj 'obj)
                                       ]
                       :fact-binding :?thing
                        })
    :UNVAR symbol
    :NNP #(str %1 " " %2)
    :VtraPres3 identity
    :ANON-RULE identity
    :QUERY identity
                 
    :NQUERY  (fn [name] (fn [session-name] (query session-name (if (.contains name ns-prefix) name (str ns-prefix name)))))
    :QUERY-notest   (fn [& facts]
                     {:name "anon-query"
                      :lhs facts
                      :params #{}
                       })
                 })





(def shop-default-fact-set
  (set [(->Customer "gold")
        (->Order 2013 "august" 20)
        (->Purchase 20 :gizmo)
        (->Purchase 120 :widget)
        (->Purchase 90 :widget)]) )


(def shop-fact-set (disk-ref (str dir-prefix "store/shop_fact_set.edn") shop-default-fact-set shop-edn-readers))




(def g-default-fact-set
  (set [(->Triple "fact-1" true "Joe Smith" "loves"  "Liz Taylor")]
        ) )

(def g-fact-set (disk-ref (str dir-prefix "store/g_fact_set.edn") g-default-fact-set g-edn-readers))








;; These rules may be stored in an external file or database.
(def shop-default-rule-list

  "discount my-discount 15 when customer status is platinum;
   discount extra-discount 10 when customer status is gold and total value > 200;
   promotion free-widget-month free-widget when customer status is gold and order month is august;")

(def shop-rule-list (disk-ref (str dir-prefix "store/shop_rule_list.edn") shop-default-rule-list shop-edn-readers))


(def g-default-rule-list

  "Gary Cooper loves ?x when ?x loves Gary Cooper ;")

(def g-rule-list (disk-ref (str dir-prefix "store/g_rule_list.edn") g-default-rule-list g-edn-readers))




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




;;; just trying a few things
(defrule shop-alert-purchase-inc-21
  ""
  [Purchase (= (inc cost) 21)]
  =>
  (do (reset! last-utterance {:type :alert , :text "someone spent 20 dollars"})
      (println "20 dollars spent")))

(defrule shop-alert-purchase-plus-2-22-test
  ""
  [Purchase (= ?cost cost)]
  [:test (= (+ 2 ?cost) 22)]
  =>
  (do (reset! last-utterance {:type :alert , :text "test confirms someone spent 20 dollars"})
      (println "by test, 20 dollars spent")))

(defrule Joe-Smith-loves-back
  [Triple (= ?x subj)(= "loves" verb)(= "Joe Smith" obj)]
  =>
  (do (insert! (->Triple "my-Joe-fact" true "Joe Smith" "loves" ?x))
      (println "Joe is loved by, and loves back: " ?x)))


;;;


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

(sc/defn ^:always-validate shop-load-user-rules-safe :- [clara.rules.schema/Production]
  "Converts a business rule string into Clara productions. Safe version."
  [business-rules :- sc/Str]

  (let [parse-tree (shop-grammar business-rules)]

    (when (insta/failure? parse-tree)
      (throw (ex-info (print-str parse-tree) {:failure parse-tree})))

    (insta/transform shop-transforms parse-tree)))


(defn shop-load-user-rules-unsafe 
  "Converts a business rule string into Clara productions. Unsafe version."
  [business-rules]

  (let [parse-tree (shop-grammar business-rules)]

    (when (insta/failure? parse-tree)
      (throw (ex-info (print-str parse-tree) {:failure parse-tree})))

    (insta/transform shop-transforms parse-tree)))

(def shop-load-user-rules shop-load-user-rules-safe)
(def shop-load-user-facts shop-load-user-rules-unsafe)







(sc/defn ^:always-validate g-load-user-rules-safe :- [clara.rules.schema/Production]
  "Converts a business rule string into Clara productions. Safe version."
  [business-rules :- sc/Str]

  (let [parse-tree (g-grammar business-rules)]

    (when (insta/failure? parse-tree)
      (throw (ex-info (print-str parse-tree) {:failure parse-tree})))

    (insta/transform g-transforms parse-tree)))


(defn g-load-user-rules-unsafe 
  "Converts a business rule string into Clara productions. Unsafe version."
  [business-rules]

  (let [parse-tree (g-grammar business-rules)]

    (when (insta/failure? parse-tree)
      (throw (ex-info (print-str parse-tree) {:failure parse-tree})))

    (insta/transform g-transforms parse-tree)))

(def g-load-user-rules g-load-user-rules-safe)
(def g-load-user-facts g-load-user-rules-unsafe)





(def shop-default-session (-> (mk-session (symbol this-ns) (shop-load-user-rules @shop-rule-list))
                    ( #(apply insert %1 %2) @shop-fact-set)
                    (fire-rules)))

(def shop-curr-session (ref shop-default-session))



(def g-default-session (-> (mk-session (symbol this-ns) (g-load-user-rules @g-rule-list))
                    ( #(apply insert %1 %2) @g-fact-set)
                    (fire-rules)))

(def g-curr-session (ref g-default-session))




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


;;silly reason.
(declare get-ans-vars)


(defn g-respond-sync

[text]
(let  [parsetree  (g-grammar text)
         intype (first (first parsetree))]
   (cond 
     (= intype :ADD-VOCAB)
       (let [voctype (second (second (first parsetree)))]
         (cond
            (= voctype "verb") 
               (do
               (dosync (alter verb-set #(into #{} (conj % (parsed-voc-map parsetree  )))))
               (pr-str (parsed-voc-map parsetree  ))
               )
            (= voctype "noun") 
               (do
               (dosync (alter noun-set #(into #{} (conj % (parsed-voc-map parsetree  )))))
               (pr-str (parsed-voc-map parsetree  ))
               )
            (= voctype "adj") 
               (do
               (dosync (alter adj-set #(into #{} (conj % (parsed-voc-map parsetree  )))))
               (pr-str (parsed-voc-map parsetree  ))
               )
            :else "unknown vocabulary type"
             ))
      (= intype :TRIP-FACT-IND2)
        (dosync  (alter g-fact-set #(into #{} (reduce conj % (map eval (g-load-user-facts text)))))
                                    (let [new-session (-> @g-curr-session 
                                                        (#(apply insert %1 %2) @g-fact-set)
                                                        (fire-rules))]
                                          (ref-set g-curr-session new-session))
                                    (str "facts added: " (pr-str (g-load-user-facts text))))
       (= intype :QUERY )
         (try
           (do 
             (let [ new-rule-list (str @g-rule-list text)
                    new-session   (-> (mk-session (symbol this-ns)
                                        (g-load-user-rules new-rule-list))
                                        ( #(apply insert %1 %2) @g-fact-set)
                                        (fire-rules))
                    anon-query  (first (insta/transform g-transforms parsetree))
                    raw-query-result (apply str (query new-session anon-query))]
                    (str (apply str (get-ans-vars raw-query-result)) " \n\n "
                          "raw query result:\n " raw-query-result )   ))
            (catch Exception e (do (println (.getMessage e)) "That's not a valid query." )))
       (= intype :ANON-RULE) (dosync (alter g-rule-list #(str % text))
                                              (let [new-session (-> (mk-session (symbol this-ns)
                                                (g-load-user-rules @g-rule-list))
                                                ( #(apply insert %1 %2) @g-fact-set)
                                                (fire-rules))]
                                                (ref-set g-curr-session new-session))
                                              (str "rules loaded: " (apply str (g-load-user-rules text))))
       
      :else "unknown input"
       )))



(defn  shop-respond-sync
  "Respond to text"
 [text]
  (let [parsetree  (shop-grammar text)
         intype (first (first parsetree))]
   (cond 
     (= intype :NQUERY)
        (try
          (apply str
            ((first (insta/transform shop-transforms parsetree)) @shop-curr-session ))
          (catch Exception e (do (println (.getMessage e))
                                         "That's not a valid query." )))
       (= intype :QUERY )
         (try
           (do 
             (let [ new-rule-list (str @shop-rule-list text)
                    new-session   (-> (mk-session (symbol this-ns)
                                        (shop-load-user-rules new-rule-list))
                                        ( #(apply insert %1 %2) @shop-fact-set)
                                        (fire-rules))
                    anon-query  (first (insta/transform shop-transforms parsetree))]
                (apply str (query new-session anon-query))))
            (catch Exception e (do (println (.getMessage e)) "That's not a valid query." )))
       (= intype (or :DISCOUNT :PROMOTION)) (dosync (alter shop-rule-list #(str % text))
                                              (let [new-session (-> (mk-session (symbol this-ns)
                                                (shop-load-user-rules @shop-rule-list))
                                                ( #(apply insert %1 %2) @shop-fact-set)
                                                (fire-rules))]
                                                (ref-set shop-curr-session new-session))
                                              (str "rules loaded: " (apply str (shop-load-user-rules text))))
       (= intype (or :F-CUSTOMER :F-ORDER :F-PURCHASE))
                            (dosync  (alter shop-fact-set #(into #{} (reduce conj % (shop-load-user-facts text))))
                                    (let [new-session (-> @shop-curr-session 
                                                        (#(apply insert %1 %2) @shop-fact-set)
                                                        (fire-rules))]
                                          (ref-set shop-curr-session new-session))
                                    (str "facts added: " (pr-str (shop-load-user-facts text))))
        :else "unknown input")))



;;; Only use "hear" and "speakup" for multi-user interfaces like irc. The bot may report events asyncronously, not just respond to questions.
;(defn shop-hear [text-in] (future 
                        ;(reset! last-utterance
                           ;{:type :response , :text (shop-respond-sync text-in)} )))


;(defn shop-speakup [speakup-chan] (add-watch last-utterance :utt-ready
                                          ;(fn [k r old-state new-state]
                                                ;(>!! speakup-chan (:text new-state) ))))

;;; Only use for repl and similar, single-user interfaces. It's syncronous (blocking). 
;(def shop-respond shop-respond-sync)



;;; Only use "hear" and "speakup" for multi-user interfaces like irc. The bot may report events asyncronously, not just respond to questions.
(defn g-hear [text-in] (future 
                        (reset! last-utterance
                           {:type :response , :text (g-respond-sync text-in)} )))


(defn g-speakup [speakup-chan] (add-watch last-utterance :utt-ready
                                          (fn [k r old-state new-state]
                                                (>!! speakup-chan (:text new-state) ))))

;; Only use for repl and similar, single-user interfaces. It's syncronous (blocking). 
(def g-respond g-respond-sync)




;(def hear shop-hear)
;(def speakup shop-speakup)
;(def respond shop-respond)

(def hear g-hear)
(def speakup g-speakup)
(def respond g-respond)


(defn get-ans-vars [raw-q-result] (re-seq #"\:\?\S+\s+\"[a-zA-z0-9_\-\s]*\"" raw-q-result))
