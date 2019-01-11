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
            [clojure.core.async :as async :refer [go-loop <! <!! >! >!!  close! chan pub sub go]]
            
            [clojure.walk :as walk :refer [postwalk]]
            
            [schema.core :as sc]
            
            
            ;[duratom.core :as dac :refer [duratom destroy]]
            [bobtailbot.tools :as tools :refer :all]
            
            [clojure.set :as set]
            
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




(defrecord Triple [name affirm subj verb obj])


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


(defn Vinf [] (set (map :inf @verb-set)))
(defn Vpast [] (set (map :past @verb-set)))
(defn Vpp [] (set (map :pp @verb-set)))
(defn Ver [] (set (map :er @verb-set)))
(defn Ving [] (set (map :ing @verb-set)))
(defn Vpres3 []  (set (map :pres3 @verb-set)))


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
(defn  Nsimp-sg [] (set (map :sing @noun-set)))



(def default-adj-set (set [
  {:a "heavy" :comp "heavier" :sup "heaviest"}
  {:a "tall" :comp "taller" :sup "tallest"}
  {:a "hard" :comp "harder" :sup "hardest"}
  {:a "good" :comp "better" :sup "best"}
  {:a "great" :comp "greater" :sup "greatest"}

  ]))

(def adj-set-edn-readers {})
(def adj-set (disk-ref (str dir-prefix "store/adj_set.edn") default-adj-set adj-set-edn-readers ))
(defn Adj [] (set (map :a @adj-set)))





(defn ebnify-wtail [coll] (apply str (map #(str "'" %1 "'"   " | ") coll)) )
(defn ebnify-notail [coll] (str (apply str (map #(str "'" %1 "'"   " | ") (butlast coll))) " " "'" (last coll) "'" " " ) )

; example:
; (apply str (map #(str "'" %1 "'"   " |") Vpres3))
; "'walks' |'loves' |'eats' |'hates' |'drinks' |'breathes' |'talks' |'kisses' |'slaps' |"


(defn g-grammar-1-annex [] (str
" <VtraInfOrPresNon3> = "  "( " (ebnify-notail (Vinf))  " ); "


;"\n VtraPast = "  "(" (ebnify-notail Vpast)  "); \n"
;"\n VtraPP = "  "(" (ebnify-notail Vpp)  "); \n"
;"\n VtraER = "  "(" (ebnify-notail Ver)  "); \n"

" GERUNDtra = "  "( " (ebnify-notail (Ving))  " ); "
" VtraPres3 = "  "( " (ebnify-notail (Vpres3))  " ); "

" <Nsimp-sg> = "  "( " (ebnify-notail (Nsimp-sg))  " ); "

" <Adj> = "  "( " (ebnify-notail (Adj))  " ); "


"\n"

))



(defn raw-g-grammar-1 [] (slurp (str dir-prefix "g-grammar-1.ebnf")) )
(defn raw-g-grammar-1-w-annex [] (str (raw-g-grammar-1) (g-grammar-1-annex)))


(def grammar-martintest
  (insta/parser  (slurp (str dir-prefix "grammar-martintest.ebnf")) :auto-whitespace :standard ))

(defn g-grammar-1 []
  (insta/parser (raw-g-grammar-1-w-annex)  :auto-whitespace :standard ))




(def g-grammar g-grammar-1)
;(def g-grammar grammar-martintest)


(defn parsed-voc-map [parsetree] (read-string (apply str (rest (nth (first parsetree) 2 )))))
;bobtailbot.brains.general.brain=> (apply str (rest (nth (first (g-grammar-1 "add verb {:my taylor  :is  rich };")) 2 )))
;"{:my taylor  :is  rich }"




(def g-ts-operators {"equals" `=
                "greater than" `>
                ">" `>
                "lower than" `<
                "<" `<
                "=" `=})


(defn NNPkw->str [kw] (-> kw name (string/replace #"_" " ")))

(defn seq->str
  ([seq] (seq->str seq ""))
  ([seq header] (case (count seq)
                      0 ""
                      1 (str (first seq) ".")
                      2 (str header (first seq) " and " (second seq) ".")
                      (recur (rest seq) (str (first seq) ", ")))))



(declare gram-voc)
(defn rem-tp [text] (string/replace text #"[\?|\!]+\s*\z" "" ))

(defn words-in [text] (into #{} (re-seq #"\S+" (rem-tp text))) )
(defn NNPtk-in [text] (into #{} (re-seq #"[A-Z]\S+" (rem-tp text))))
(defn vars-in [text] (into #{} (re-seq #"\?\S+" (rem-tp text))))

(defn poss-voc-in [text] (set/difference (words-in text)(NNPtk-in text) (vars-in text)))
(defn unk-words [text](set/difference (poss-voc-in text) (gram-voc)) )





(def g-transforms
  {:NUMBER #(Integer/parseInt %)
   :TS-OPERATOR g-ts-operators
   :ANON-RULE-notest (fn [fact & facts]
                       {:ns-name (symbol this-ns)
                        :name (str ns-prefix "my-anon-rule")
                        :lhs facts
                        :rhs `(insert! ~fact)})
                 
    :TRIP-FACT-IND2 (fn [t-subj t-verb t-obj]
                      `(->Triple "my-fact" true ~t-subj ~t-verb ~t-obj ) )
    :PRENEG-TRIP-FACT-IND2 (fn [t-subj t-verb t-obj]
                             `(->Triple "my-fact" false ~t-subj ~t-verb ~t-obj ))
    :EMBNEG-TRIP-FACT-IND2 (fn [t-subj t-verb-inf t-obj]
                              (let [t-verb-pres3 (:pres3 (first (filter #(= (:inf %) t-verb-inf ) @verb-set)))]
                                `(->Triple "my-fact" false ~t-subj ~t-verb-pres3 ~t-obj )))
    :R-TRIP-FACT-IND2 (fn [t-subj t-verb t-obj]
                      {:type Triple
                       :constraints [(list '= true 'affirm)
                                     (list '= t-subj 'subj)
                                     (list '= t-verb 'verb)
                                     (list '= t-obj 'obj)
                                       ]
                        })
    :NEG-R-TRIP-FACT-IND2 (fn [t-subj t-verb t-obj]
                      {:type Triple
                       :constraints [(list '= false 'affirm)
                                     (list '= t-subj 'subj)
                                     (list '= t-verb 'verb)
                                     (list '= t-obj 'obj)
                                       ]
                        })
    :Q-TRIP-FACT-IND2 (fn [t-subj t-verb t-obj]
                      {:type Triple
                       :constraints [(list '= true 'affirm)
                                     (list '= t-subj 'subj)
                                     (list '= t-verb 'verb)
                                     (list '= t-obj 'obj)
                                       ]
                       :fact-binding :?#thing
                        })
    :PRENEG-Q-TRIP-FACT-IND2 (fn [t-subj t-verb t-obj]
                              {:type Triple
                               :constraints [(list '= false 'affirm)
                                             (list '= t-subj 'subj)
                                             (list '= t-verb 'verb)
                                             (list '= t-obj 'obj) ]
                                :fact-binding :?#thing })
    :EMBNEG-Q-TRIP-FACT-IND2 (fn [t-subj t-verb-inf t-obj]
                               (let [t-verb-pres3 (:pres3 (first (filter #(= (:inf %) t-verb-inf ) @verb-set)))]
                                  { :type Triple
                                    :constraints [(list '= false 'affirm)
                                                 (list '= t-subj 'subj)
                                                 (list '= t-verb-pres3 'verb)
                                                 (list '= t-obj 'obj) ]
                                    :fact-binding :?#thing }))
    
    
    ;CAREFUL: this only works because the Triple record only has one boolean (affirm).
    :Q-NOT-FACTS   (fn [map] (postwalk #(if (or (= % true) (= % false)) (not %) % ) map)) 
    :R-NOT-FACTS   (fn [map] (postwalk #(if (or (= % true) (= % false)) (not %) % ) map)) 
    :NOT-FACTS   (fn [map] (postwalk #(if (or (= % true) (= % false)) (not %) % ) map)) 
    
    :Q-PREAFF-FACTS identity
    :R-PREAFF-FACTS identity
    :PREAFF-FACTS identity
    
    
    :AND-FACTS vector
    :UNVAR symbol
    
    :NNP (fn [& NNP-tokens] (keyword (string/join "_" NNP-tokens)))
    
    :VtraPres3 identity
    ;:VtraPres3 keyword
    
    :VtraInf identity
    
    
    :ANON-RULE identity
    :QUERY identity
    :YNQUESTION identity
                 
    :NQUERY  (fn [name] (fn [session-name] (query session-name (if (.contains name ns-prefix) name (str ns-prefix name)))))
    :QUERY-notest   (fn [& facts]
                     {:name "anon-query"
                      :lhs facts
                      :params #{}
                       })
    :YNQUESTION-notest   (fn [& facts]
                     {:name "anon-query"
                      :lhs facts
                      :params #{}
                       })
                 })



(defn joinNNPstr [NNP] (string/join " " (rest NNP)))

(defn g-rephrase-from-tree [parsetree]
 (let [actual-ptree (first parsetree)
       intype (first actual-ptree)]
    (cond
      (= intype :T-DOES-QUESTION)
        (let [t-subj (joinNNPstr (second actual-ptree))
              t-verb-inf (second (nth actual-ptree 2))
              t-verb-pres3 (:pres3 (first (filter #(= (:inf %) t-verb-inf ) @verb-set))) 
              t-obj (joinNNPstr (nth actual-ptree 3))]
           (str t-subj " " t-verb-pres3 " " t-obj "?"))
      (= intype :T-WHO-QUESTION)
        (let [t-verb  (second (second actual-ptree))
              t-obj (joinNNPstr (nth actual-ptree 2))]
           (str "match ?x " t-verb " " t-obj ))
      (= intype :T-WHOM-QUESTION)
        (let [t-subj  (joinNNPstr (second actual-ptree))
              t-verb-inf (second (nth actual-ptree 2))
              t-verb-pres3 (:pres3 (first (filter #(= (:inf %) t-verb-inf ) @verb-set)))]
           (str "match " t-subj " " t-verb-pres3 " " "?x" ))
      :else "g-rephrase-from-tree failed" )))





(def g-default-fact-set
  (set [
         (->Triple "fact-1" true :Joe_Smith "loves"  :Liz_Taylor)
         ]
        ) )

(def g-fact-set (disk-ref (str dir-prefix "store/g_fact_set.edn") g-default-fact-set g-edn-readers))




(def g-default-rule-list

  "Gary Cooper loves ?x when ?x loves Gary Cooper ;")

(def g-rule-list (disk-ref (str dir-prefix "store/g_rule_list.edn") g-default-rule-list g-edn-readers))



(def last-utterance (atom {}))



(defrule Joe-Smith-loves-back
  [Triple (= ?x subj)(= "loves" verb)(= "Joe Smith" obj)]
  =>
  (do (insert! (->Triple "my-Joe-fact" true "Joe Smith" "loves" ?x))
      (println "Joe is loved by, and loves back: " ?x)))




(sc/defn ^:always-validate g-load-user-rules-safe :- [clara.rules.schema/Production]
  "Converts a business rule string into Clara productions. Safe version."
  [business-rules :- sc/Str]

  (let [parse-tree ((g-grammar) business-rules)]

    (when (insta/failure? parse-tree)
      (throw (ex-info (print-str parse-tree) {:failure parse-tree})))

    (insta/transform g-transforms parse-tree)))


(defn g-load-user-rules-unsafe 
  "Converts a business rule string into Clara productions. Unsafe version."
  [business-rules]

  (let [parse-tree ((g-grammar) business-rules)]

    (when (insta/failure? parse-tree)
      (throw (ex-info (print-str parse-tree) {:failure parse-tree})))

    (insta/transform g-transforms parse-tree)))

(def g-load-user-rules g-load-user-rules-safe)
(def g-load-user-facts g-load-user-rules-unsafe)



(def g-default-session (-> (mk-session (symbol this-ns) (g-load-user-rules @g-rule-list))
                    ( #(apply insert %1 %2) @g-fact-set)
                    (fire-rules)))

(def g-curr-session (ref g-default-session))





;;https://groups.google.com/d/msg/clara-rules/CFvJQGwelo0/NYBMmV9hFAAJ
#_("Will's answer is correct. You'll find the related API documentation here: http://www.clara-rules.org/apidocs/0.12.0/clojure/clara.rules.html#var-retract
We don't have any immediate plans to dynamically add or remove rules from a session, although I could see this happening in the future. I know of at least one use case that handles this by keeping a list of facts they have inserted, and re-building a new session dynamically and inserted those facts into a new session, while the previous gets garbage collected. 
Dynamic rules is something I wouldn't mind adding to Clara, although that comes with some downside. For instance, we'd have to track all facts that didn't match any rule at all (keeping them in memory), whereas today facts that match nothing are simply garbage collected.
-Ryan
")


;;silly reason.
(declare get-ans-vars)
(declare remove-iitt)
(declare get-who)

(def negating (atom false))



(defn g-respond-sync

[text]
(let  [ 
        cleantext (remove-iitt text)
        negtext (str "it's false that " cleantext)
        yntext (str cleantext " ?")
        
        parsetree  ((g-grammar) text)
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
       (or (= intype :TRIP-FACT-IND2 ) (= intype :PRENEG-TRIP-FACT-IND2) (= intype :EMBNEG-TRIP-FACT-IND2) (= intype :NOT-FACTS ) (= intype :PREAFF-FACTS ))
         (cond 
          (= (g-respond-sync yntext) "Yes, that's right.") (do "I know, right.")
          (= (g-respond-sync yntext) "Definitely not, that's false.") (do "That's impossible.")
          (= (g-respond-sync yntext) "Not that I know of.") (dosync  (alter g-fact-set #(into #{} (reduce conj % (map eval (g-load-user-facts text)))))
                                                              (let [new-session (-> @g-curr-session 
                                                                                 (#(apply insert %1 %2) @g-fact-set)
                                                                                 (fire-rules))]
                                                                    (ref-set g-curr-session new-session))
                                                              ;(str "facts added: " (pr-str (g-load-user-facts text)))
                                                               (do "OK, got it."))
          :else "Oops, a bug in my respond function"  )
          
       (= intype :AND-FACTS)
        (dosync  (alter g-fact-set #(into #{} (reduce conj % (map eval (first (g-load-user-facts text))))))
                                    (let [new-session (-> @g-curr-session 
                                                        (#(apply insert %1 %2) @g-fact-set)
                                                        (fire-rules))]
                                          (ref-set g-curr-session new-session))
                                    (str "facts added: " (pr-str (first (g-load-user-facts text)))))
       (= intype :QUERY )
         (try
           (do 
             (let [ new-rule-list (str @g-rule-list text)
                    new-session   (-> (mk-session (symbol this-ns)
                                        (g-load-user-rules new-rule-list))
                                        ( #(apply insert %1 %2) @g-fact-set)
                                        (fire-rules))
                    anon-query  (first (insta/transform g-transforms parsetree))
                    raw-query-result  (query new-session anon-query)
                    raw-query-result-set (into #{} raw-query-result)
                    ;raw-query-result-str (apply str raw-query-result)
                    raw-query-result-set-str (apply str raw-query-result-set)]
                    (str 
                          "satisfiers: " (string/join "  " (get-ans-vars raw-query-result-set-str)) "    " 
                          ;"raw query result (no duplicates):  " raw-query-result-set-str 
                          )))
            (catch Exception e (do (println (.getMessage e)) "That's not a valid query." )))
       (= intype :YNQUESTION )
         (try
           (do 
             (let [ new-rule-list (str @g-rule-list text)
                    new-session   (-> (mk-session (symbol this-ns)
                                        (g-load-user-rules new-rule-list))
                                        ( #(apply insert %1 %2) @g-fact-set)
                                        (fire-rules))
                    anon-query  (first (insta/transform g-transforms parsetree))
                    raw-query-result  (query new-session anon-query)
                    ;raw-query-result-set (into #{} raw-query-result)
                    raw-query-result-str (apply str raw-query-result)
                    ;raw-query-result-set-str (apply str raw-query-result-set)
                    
                    
                    
                    ]
                    ;(println "raw-query-result-str: " raw-query-result-str)
                    (cond
                      (and (= raw-query-result-str "")  @negating ) (do (reset! negating false)  "Not that I know of.")
                      (and (= raw-query-result-str "")  (not @negating) ) (do 
                                                                             (reset! negating true)
                                                                             (let [neg-qr (g-respond-sync negtext)]
                                                                                   (if (= neg-qr "Yes, that's right.") 
                                                                                     (do  (reset! negating false)  "Definitely not, that's false.")
                                                                                     
                                                                                     (do (reset! negating false)  "Not that I know of."))))
                       
                       :else "Yes, that's right."
                       
                       
                       
                       )
                       
                       ))
            (catch Exception e (do (println (.getMessage e)) "That's not a valid query." )))
       
       (= intype :T-DOES-QUESTION) (g-respond-sync (g-rephrase-from-tree parsetree))
       (= intype :T-WHO-QUESTION) (get-who (g-respond-sync (g-rephrase-from-tree parsetree)))
       (= intype :T-WHOM-QUESTION) (get-who (g-respond-sync (g-rephrase-from-tree parsetree)))
       
       (= intype :ANON-RULE) (dosync (alter g-rule-list #(str % text ";"))
                                              (let [new-session (-> (mk-session (symbol this-ns)
                                                (g-load-user-rules @g-rule-list))
                                                ( #(apply insert %1 %2) @g-fact-set)
                                                (fire-rules))]
                                                (ref-set g-curr-session new-session))
                                              
                                              ;(str "rules loaded: " (apply str (g-load-user-rules text)))
                                              "OK, got it. That's a rule."
                                              
                                              )
       
      :else "unknown input"
       )))




(defn g-respond-sync-top [text]
(let [ukw (unk-words text)]
 (cond 
   (empty? ukw) (g-respond-sync text)
   :else (str "I don't know these words: " (string/join ", " ukw))
   )
))






;;; Only use "hear" and "speakup" for multi-user interfaces like irc. The bot may report events asyncronously, not just respond to questions.
(defn g-hear [text-in] (future 
                        (reset! last-utterance
                           {:type :response , :text (g-respond-sync text-in)} )))


(defn g-speakup [speakup-chan] (add-watch last-utterance :utt-ready
                                          (fn [k r old-state new-state]
                                                (>!! speakup-chan (:text new-state) ))))

;; Only use for repl and similar, single-user interfaces. It's syncronous (blocking). 
(def g-respond g-respond-sync-top)




(def hear g-hear)
(def speakup g-speakup)
(def respond g-respond)

(defn get-ans-vars [raw-q-result] (re-seq #"\:\?[\S&&[^\#]]+\s+[\S&&[^\{\}]]+" raw-q-result))
;(defn get-ans-vars [raw-q-result] (re-seq #"\:\?\S+\s+\"[a-zA-z0-9_\-\s]*\"" raw-q-result))

(defn remove-iitt [text] (string/replace text #"is it true that" ""  ))

(defn get-who [x-str] (->> x-str (re-seq #"\:\?x\s+\:(\S+)" ) (map second) (map #(clojure.string/replace % "_" " ")) (seq->str)  ))

(defn gram-voc [] (into #{} (map #(if (second %) (second %) (nth % 2)) (re-seq #"\"([a-z\']+)\"|\'([a-z]+)\'" (raw-g-grammar-1-w-annex)))))
