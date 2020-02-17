;;; CREDIT:
;;; https://github.com/cerner/clara-examples/blob/master/src/main/clojure/clara/examples/insta.clj

(ns bobtailbot.brains.general.brain
  "General chatbot"
  (:require 
   [taoensso.timbre :as timbre   :refer [log  trace  debug  info  warn  error  fatal  report logf tracef debugf infof warnf errorf fatalf reportf  spy get-env]]
   
   [instaparse.core :as insta]
   [clara.rules.accumulators :as acc]
   [clara.rules :refer :all]
   
            ;; just for development
   [clara.tools.inspect :as cti :refer [inspect]]
            ;; /just for development
   
   
   [clojure.string :as string]
   [clojure.core.async :as async :refer [go-loop <! <!! >! >!!  close! chan pub sub go]]
   
   [clojure.walk :as walk :refer [postwalk]]
   
   [schema.core :as sc]
   
   
   [bobtailbot.tools :as tools :refer [tim-ret dump-to-path dump-to-path-records load-from-path-or-create ]]
   
   [clojure.set :as set]
   
   [clojure.edn :as edn]))



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


(declare dump-to-path-rlg)




(defrecord Triple [name affirm subj verb obj])


;https://www.compoundtheory.com/clojure-edn-walkthrough/
(def g-edn-readers 
    { (symbol (str this-ns "." "Triple")) map->Triple }
    )




(def default-verb-set (set [
  {:inf "xxexamplefy", :past "xxexamplefied", :pp "xxexamplefied" , :er "xxexamplefier", :ing "xxexamplefying", :pres3 "xxexamplefies"}
  
  {:inf "walk", :past "walked", :pp "walked" , :er "walker", :ing "walking", :pres3 "walks"}
  {:inf "talk", :past "talked", :pp "talked" , :er "talker", :ing "talking", :pres3 "talks"}
  {:inf "breath", :past "breathed", :pp "breathed" , :er "breather", :ing "breathing", :pres3 "breathes"}
  {:inf "like", :past "liked", :pp "liked" , :er "liker", :ing "liking", :pres3 "likes"}
  {:inf "love", :past "loved", :pp "loved" , :er "lover", :ing "loving", :pres3 "loves"}
  {:inf "hate", :past "hated", :pp "hated" , :er "hater", :ing "hating", :pres3 "hates"}
  {:inf "kiss", :past "kissed", :pp "kissed" , :er "kisser", :ing "kissing", :pres3 "kisses"}
  {:inf "slap", :past "slapped", :pp "slapped" , :er "slapper", :ing "slapping", :pres3 "slaps"}
  {:inf "eat", :past "ate", :pp "eaten" , :er "eater", :ing "eating", :pres3 "eats"}
  {:inf "drink", :past "drank", :pp "drunk" , :er "drinker", :ing "drinking", :pres3 "drinks"}
  ]))

(def verb-set-edn-readers {})

;(def verb-set (disk-ref (str data-dir-prefix "store/verb_set.edn") default-verb-set verb-set-edn-readers ))


(defn get-verb-set [] (load-from-path-or-create (str data-dir-prefix "store/verb_set.edn") default-verb-set verb-set-edn-readers ))
(defn set-verb-set [verb-set] (dump-to-path-rlg (str data-dir-prefix "store/verb_set.edn") verb-set  )) ; "set the verb set". Don't confuse "set", the verb, with "set" the noun.





(defn Vinf [] (set (map :inf (get-verb-set))))
(defn Vpast [] (set (map :past (get-verb-set))))
(defn Vpp [] (set (map :pp (get-verb-set))))
(defn Ver [] (set (map :er (get-verb-set))))
(defn Ving [] (set (map :ing (get-verb-set))))
(defn Vpres3 []  (set (map :pres3 (get-verb-set))))



(def default-noun-set (set [
  {:sing "xxthing", :plural "xxthings"}
  
  {:sing "thing", :plural "things"}
  {:sing "object", :plural "objects"}
  {:sing "animal", :plural "animals"}
  {:sing "person", :plural "people"}
  {:sing "man", :plural "men"}
  {:sing "woman", :plural "women"}
  ]))

(def noun-set-edn-readers {})

(defn get-noun-set [] (load-from-path-or-create (str data-dir-prefix "store/noun_set.edn") default-noun-set noun-set-edn-readers ))
(defn set-noun-set [noun-set] (dump-to-path-rlg (str data-dir-prefix "store/noun_set.edn") noun-set ))

(defn  Nsimp-sg [] (set (map :sing (get-noun-set))))



(def default-adj-set (set [
  {:a "xxexamply" :comp "xxexamplier" :sup "xxexampliest"}
  
  {:a "heavy" :comp "heavier" :sup "heaviest"}
  {:a "tall" :comp "taller" :sup "tallest"}
  {:a "hard" :comp "harder" :sup "hardest"}
  {:a "good" :comp "better" :sup "best"}
  {:a "great" :comp "greater" :sup "greatest"}

  ]))

(def adj-set-edn-readers {})

(defn get-adj-set [] (load-from-path-or-create (str data-dir-prefix "store/adj_set.edn") default-adj-set adj-set-edn-readers ))
(defn set-adj-set [adj-set] (dump-to-path-rlg (str data-dir-prefix "store/adj_set.edn") adj-set  ))


(defn Adj [] (set (map :a (get-adj-set))))





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


;(def grammar-martintest
  ;(insta/parser  (slurp (str dir-prefix "grammar-martintest.ebnf")) :auto-whitespace :standard ))

(defn g-grammar-1 []
  (insta/parser (raw-g-grammar-1-w-annex)  :auto-whitespace :standard ))


(def g-grammar-1-atom (atom (g-grammar-1)))
(defn g-grammar-1-a-fn [] (do @g-grammar-1-atom))
(defn reload-g-grammar-1 [] (reset! g-grammar-1-atom (g-grammar-1) ))

(def g-grammar g-grammar-1-a-fn)
(def reload-g-grammar reload-g-grammar-1)


(defn dump-to-path-rlg "dump to path and reload grammar" [path value] (do 
                            (dump-to-path path value)
                            (reload-g-grammar)) )


(defn parsed-voc-map [parsetree] (read-string (apply str (rest (nth (first parsetree) 2 )))))
;bobtailbot.brains.general.brain=> (apply str (rest (nth (first (g-grammar-1 "add verb {:my taylor  :is  rich };")) 2 )))
;"{:my taylor  :is  rich }"




(def g-ts-operators {"equals" `=
                "greater than" `>
                ">" `>
                "lower than" `<
                "<" `<
                "=" `=})


(defn NNPkw2str "transform keyword NNPs into strings" [kw] (-> kw name (string/replace #"_" " ")))

(defn seq2str "Ex: (but double quotes) ['Anna' 'Carol'] => 'Anna and Carol' "
  ([seq] (seq2str seq ""))
  ([seq header] (case (count seq)
                  0 ""
                  1 (str (first seq) ".")
                  2 (str header (first seq) " and " (second seq) ".")
                  (recur (rest seq) (str (first seq) ", ")))))

(declare gram-voc)
(defn rem-tp "remove trailing punctuation" [text] (string/replace text #"[\?|\!]+\s*\z" "" ))

(defn words-in "Return the words in the text as a set"
  [text] (into #{} (re-seq #"\S+" (rem-tp text))) )

(defn NNPtk-in "Return the name tokens in the text as a set"
  [text] (into #{} (re-seq #"[A-Z]\S+" (rem-tp text))))

(defn vars-in "Return the var names in the text as a set"
   [text] (into #{} (re-seq #"\?\S+" (rem-tp text))))

(defn poss-voc-in "Return possible new vocabulary entries as a set (remove proper names and vars)"
  [text] (set/difference (words-in text)(NNPtk-in text) (vars-in text)))

(defn unk-words "Return unknown words as a set" [text](set/difference (poss-voc-in text) (gram-voc)) )



(defn negate "find booleans in input rormap (a record or map) and negate them" [rormap] 
(postwalk #(if (or (= % true) (= % false)) (not %) % ) rormap)   )

(defn conjugate-pres3 "Vinf->Vpres3" [vinf]
  (:pres3 (first (filter #(= (:inf %) vinf) (get-verb-set)))))


(def g-transforms-base "without :FACTS "
  {:NUMBER #(Integer/parseInt %)

   :UNVAR symbol
   :NNP (fn [& NNP-tokens] (keyword (string/join "_" NNP-tokens)))

   :VtraPres3 identity
   
   :VtraInf identity
   
   :TS-OPERATOR g-ts-operators
   
   :ANON-RULE-notest (fn [fact & facts]
                       {:ns-name (symbol this-ns)
                        :name (str ns-prefix (gensym "my-anon-rule"))
                        :lhs facts
                        :rhs `(insert! ~fact)})
   
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
                              (let [t-verb-pres3 (conjugate-pres3 t-verb-inf)]
                                { :type Triple
                                 :constraints [(list '= false 'affirm)
                                               (list '= t-subj 'subj)
                                               (list '= t-verb-pres3 'verb)
                                               (list '= t-obj 'obj) ]
                                 :fact-binding :?#thing }))
   
   :D-TRIP-FACT-IND2 (fn [t-subj t-verb-inf t-obj]
                       (let [t-verb-pres3 (conjugate-pres3 t-verb-inf)]
                         {:type Triple
                          :constraints [(list '= true 'affirm)
                                        (list '= t-subj 'subj)
                                        (list '= t-verb-pres3 'verb)
                                        (list '= t-obj 'obj)]
                          :fact-binding :?#thing}))
   
   :NEG-D-TRIP-FACT-IND2 (fn [t-subj t-verb-inf t-obj]
                           (let [t-verb-pres3 (conjugate-pres3 t-verb-inf)]
                             {:type Triple
                              :constraints [(list '= false 'affirm)
                                            (list '= t-subj 'subj)
                                            (list '= t-verb-pres3 'verb)
                                            (list '= t-obj 'obj)]
                              :fact-binding :?#thing}))
   
   :WHO-TRIP-FACT-IND2 (fn [t-verb t-obj]
                         {:type Triple
                          :constraints [(list '= true 'affirm)
                                        (list '= '?x 'subj)
                                        (list '= t-verb 'verb)
                                        (list '= t-obj 'obj)]
                          :fact-binding :?#thing})
   
   :WHOM-TRIP-FACT-IND2 (fn [t-subj t-verb-inf]
                          (let [t-verb-pres3 (conjugate-pres3 t-verb-inf)]
                            {:type Triple
                             :constraints [(list '= true 'affirm)
                                           (list '= t-subj 'subj)
                                           (list '= t-verb-pres3 'verb)
                                           (list '= '?x 'obj)]
                             :fact-binding :?#thing}))
   
   ;CAREFUL: this only works because the Triple record only has one boolean (affirm).
   :Q-NOT-FACTS   negate 
   :R-NOT-FACTS  negate
   ; :NOT-FACTS  negate
   
   :Q-PREAFF-FACTS identity
   :R-PREAFF-FACTS identity
   ; :PREAFF-FACTS identity
   
   
   :AND-FACTS vector

   
   
   :ANON-RULE identity
   :QUERY identity
   :YNQUESTION identity
   :NEG-YNQUESTION identity
   

   
   
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
   :NEG-YNQUESTION-notest   (fn [& facts]
                              {:name "anon-query"
                               :lhs (map negate facts)
                               :params #{}
                               })
   :T-DOES-QUESTION   (fn [ dfact]
                        {:name "anon-query"
                         :lhs [dfact]
                         :params #{}})
   :NEG-T-DOES-QUESTION (fn [dfact]
                          {:name "anon-query"
                           :lhs [dfact]
                           :params #{}})
   :T-WHO-QUESTION   (fn [wfact]
                       {:name "anon-query"
                        :lhs [wfact]
                        :params #{}})
   :T-WHOM-QUESTION (fn [wfact]
                      {:name "anon-query"
                       :lhs [wfact]
                       :params #{}})
   })

(def g-transforms-mkst  "treats :FACTS normally, as a statement"
  (conj
   g-transforms-base
   {:FACTS identity
    :ANON-FACT identity
    :TRIP-FACT-IND2 (fn [t-subj t-verb t-obj]
                      `(->Triple "my-fact" true ~t-subj ~t-verb ~t-obj))
    :PRENEG-TRIP-FACT-IND2 (fn [t-subj t-verb t-obj]
                             `(->Triple "my-fact" false ~t-subj ~t-verb ~t-obj))
    :EMBNEG-TRIP-FACT-IND2 (fn [t-subj t-verb-inf t-obj]
                             (let [t-verb-pres3 (conjugate-pres3 t-verb-inf)]
                               `(->Triple "my-fact" false ~t-subj ~t-verb-pres3 ~t-obj)))
    :NOT-FACTS  negate
    :PREAFF-FACTS identity}
   ))

(def g-transforms-ckst "treats :FACTS as a yn-question, to check statements"
  (conj
   g-transforms-base
   {:FACTS identity
    :ANON-FACT (fn [& facts]
                 {:name "anon-query"
                  :lhs facts
                  :params #{}})
    :TRIP-FACT-IND2 (fn [t-subj t-verb t-obj]
                      {:type Triple
                       :constraints [(list '= true 'affirm)
                                     (list '= t-subj 'subj)
                                     (list '= t-verb 'verb)
                                     (list '= t-obj 'obj)]
                       :fact-binding :?#thing})
    :PRENEG-TRIP-FACT-IND2 (fn [t-subj t-verb t-obj]
                             {:type Triple
                              :constraints [(list '= false 'affirm)
                                            (list '= t-subj 'subj)
                                            (list '= t-verb 'verb)
                                            (list '= t-obj 'obj)]
                              :fact-binding :?#thing})
    :EMBNEG-TRIP-FACT-IND2 (fn [t-subj t-verb-inf t-obj]
                             (let [t-verb-pres3 (conjugate-pres3 t-verb-inf)]
                               {:type Triple
                                :constraints [(list '= false 'affirm)
                                              (list '= t-subj 'subj)
                                              (list '= t-verb-pres3 'verb)
                                              (list '= t-obj 'obj)]
                                :fact-binding :?#thing}))
    :NOT-FACTS  negate
    :PREAFF-FACTS identity
    }))

(def g-transforms g-transforms-mkst)


(def fact-file-p (str data-dir-prefix "store/g_fact_set.edn"))
(def rule-file-p (str data-dir-prefix "store/g_rule_list.edn"))


(def g-default-fact-set
  (set [
         (->Triple "example-fact" true :Example_Person_One "xxexamplefies"  :Example_Person_Two)
         ]
        ) )


(defn get-g-fact-set []   (load-from-path-or-create    fact-file-p    g-default-fact-set    g-edn-readers))
;(defn set-g-fact-set [g-fact-set]   (dump-to-path-records    fact-file-p   g-fact-set))




(def g-fact-set-atom (atom (get-g-fact-set)))

(defn reload-g-fact-set [] (reset! g-fact-set-atom (get-g-fact-set)))

(defn dump-to-path-rlf "dump to path and reload facts"
  [path value]
  (do (dump-to-path-records path value)
      (reload-g-fact-set)))

(defn set-g-fact-set [g-fact-set]   (dump-to-path-rlf  fact-file-p  g-fact-set ))





(def g-default-rule-list   "Example Person One xxexamplefies ?x when ?x xxexamplefies Example Person One ;")
(defn get-g-rule-list []   (load-from-path-or-create    rule-file-p    g-default-rule-list    g-edn-readers))

(defn get-g-rules-ptree []
  ((g-grammar) (get-g-rule-list) ))
(defn get-g-rules-transformed []
  (insta/transform g-transforms (get-g-rules-ptree)))
(def g-rules-tr-atom (atom (get-g-rules-transformed)))
(defn reload-g-rules-tr [] (reset! g-rules-tr-atom (get-g-rules-transformed)) )
(defn dump-to-path-rlr "dump to path and reload rules"
  [path value]
  (do (dump-to-path path value)
      (reload-g-rules-tr)))
(defn set-g-rule-list [g-rule-list]   (dump-to-path-rlr    rule-file-p   g-rule-list))


(def last-utterance (atom {}))

;(defrule Joe-Smith-loves-back
  ;[Triple (= ?x subj)(= "loves" verb)(= "Joe Smith" obj)]
  ;=>
  ;(do (insert! (->Triple "my-Joe-fact" true "Joe Smith" "loves" ?x))
      ;(timbre/info "Joe is loved by, and loves back: " ?x)))

(sc/defn ^:always-validate g-load-user-rules :- [clara.rules.schema/Production]
  "Converts a business rule string into Clara productions."
  [business-rules :- sc/Str]

  (let [parse-tree ((g-grammar) business-rules)]

    (when (insta/failure? parse-tree)
      (throw (ex-info (print-str parse-tree) {:failure parse-tree})))

    (insta/transform g-transforms parse-tree)))

(defn g-load-user-facts
  "Converts facts into Clara record entries."
  [facts]

  (let [parse-tree ((g-grammar) facts)]

    (when (insta/failure? parse-tree)
      (throw (ex-info (print-str parse-tree) {:failure parse-tree})))

    (insta/transform g-transforms-mkst parse-tree)))




(def g-default-session 
  (-> (mk-session 
       (symbol this-ns) 
       ;;(g-load-user-rules   (get-g-rule-list))
       @g-rules-tr-atom
       )
      ( #(apply insert %1 %2) (get-g-fact-set))
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

(declare get-ans-vars-rtxt)
(declare get-ans-vars-rvec)




(declare get-who)




(def ans-yes "Yes, that's right.")
(def ans-ikr "I know, right.")       
(def ans-no "Definitely not, that's false.")      
(def ans-imp "That's impossible.")    
(def ans-dunno "Not that I know of.")       
(def ans-okgotit "OK, got it.")      
(def ans-ok-rule "OK, got it. That's a rule.")

(defn ans-oops [fname] (str "Oops, a bug in the function: " fname )  )
(def ans-oops-rf (ans-oops "respond"))

(def ans-invalid-query "That's not a valid query.")
(def ans-invalid-fact "That's not a valid fact.")   
(def ans-sorrywhat "Sorry, I didn't get that.")
(def ans-contradiction "There's a contradiction! The answer to that question is both yes and no.")

;; using (timbre/spy) for debugging
(defn g-respond-sync-yes-dunno-ptreetr "respond to yes/no question, negated yes/no question, does-question or negated does-question, with either 'yes' or 'dunno'. Question must be in ptreetr form (parsed and transformed)"
  [ptreetr]
  (try
    (do
      (let [anon-query (timbre/spy (first ptreetr))
            new-tr-rules (timbre/spy (conj @g-rules-tr-atom anon-query))
            new-session   (-> (mk-session
                               (symbol this-ns)
                               new-tr-rules
                               )
                              (#(apply insert %1 %2) (get-g-fact-set))
                              (fire-rules))
            raw-query-result  (timbre/spy (query new-session anon-query))
            raw-query-result-str (timbre/spy (apply str raw-query-result))]

        (if (= raw-query-result-str "")
          (do ans-dunno)
          (do ans-yes))))
    (catch Exception e (do (timbre/info (.getMessage e)) ans-invalid-query))))


(defn g-respond-sync-yndq-ptree "answer yes/no question, negated yes/no question, does-question or negated does-question, in ptree form (parsed)"
  [ptree]
  (try
    (do
      (let [ptreetr (insta/transform g-transforms ptree)
            neg-ptreetr (negate ptreetr)
            query-ans (g-respond-sync-yes-dunno-ptreetr ptreetr)
            negquery-ans (g-respond-sync-yes-dunno-ptreetr neg-ptreetr)]
        (do 
          (timbre/info "calling g-respond-sync-yndq-ptree ...  " "ptree: " ptree ", ptreetr: " ptreetr ", neg-ptreetr: " neg-ptreetr
                   ", query-ans: " query-ans ", negquery-ans: " negquery-ans)
          (cond
            (and (= query-ans ans-dunno)  (= negquery-ans ans-dunno)) (do  ans-dunno)
            (and (= query-ans ans-dunno)  (= negquery-ans ans-yes)) (do  ans-no)
            (and (= query-ans ans-yes)  (= negquery-ans ans-dunno)) (do  ans-yes)
            (and (= query-ans ans-yes)  (= negquery-ans ans-yes)) (do  ans-contradiction)

            :else (ans-oops "g-respond-sync-yndq-ptree")))))
    (catch Exception e (do (timbre/info (.getMessage e)) ans-invalid-query))))

(defn g-respond-sync-ckst-ptree "check statement, in ptree form (parsed)"
  [ptree]
  (try
    (do
      (let [ptreetr (insta/transform g-transforms-ckst ptree)
            neg-ptreetr (negate ptreetr)
            query-ans (g-respond-sync-yes-dunno-ptreetr ptreetr)
            negquery-ans (g-respond-sync-yes-dunno-ptreetr neg-ptreetr)]
        (do (timbre/info "calling g-respond-sync-ckst-ptree ...  " "ptree: " ptree ", ptreetr: " ptreetr ", neg-ptreetr: " neg-ptreetr 
                     ", query-ans: " query-ans ", negquery-ans: " negquery-ans)
           (cond
             (and (= query-ans ans-dunno)  (= negquery-ans ans-dunno)) (do  ans-dunno)
             (and (= query-ans ans-dunno)  (= negquery-ans ans-yes)) (do  ans-no)
             (and (= query-ans ans-yes)  (= negquery-ans ans-dunno)) (do  ans-yes)
             (and (= query-ans ans-yes)  (= negquery-ans ans-yes)) (do  ans-contradiction)

             :else (ans-oops "g-respond-sync-ckst-ptree")) ) 
        
        
        ))
    (catch Exception e (do (timbre/info (.getMessage e)) ans-invalid-query))))

(defn g-respond-sync-mkst-ptree "make a statement, in ptree form (parsed)"
  [ptree]
  (try
    
    (let [ptreetr (insta/transform g-transforms-mkst ptree)
          ev-ptreetr (timbre/spy (map eval ptreetr))
          ;anon-fact (timbre/spy (first ev-ptreetr) )
          ;new-fact-set (timbre/spy (conj @g-fact-set-atom anon-fact))
          new-fact-set (timbre/spy (reduce conj @g-fact-set-atom ev-ptreetr))
          ]
      (timbre/spy (set-g-fact-set new-fact-set))
      
      )
      (let [new-session  (timbre/spy (-> @g-curr-session
                                         (#(apply insert %1 %2) @g-fact-set-atom)
                                         (fire-rules))) ]
        (timbre/spy  (dosync (ref-set g-curr-session new-session) ans-okgotit)
                      )
        )        
    (catch Exception e (do (timbre/info (.getMessage e)) ans-invalid-fact))))










(defn g-respond-sync-who-ptree "answer WHO-question, in ptree form (parsed)"
  [ptree]
  (try
    (let [ptreetr (timbre/spy (insta/transform g-transforms ptree))
          anon-query (timbre/spy (first ptreetr))
          new-tr-rules (timbre/spy (conj @g-rules-tr-atom anon-query))
          new-session   (-> (mk-session
                             (symbol this-ns)
                             new-tr-rules)
                            (#(apply insert %1 %2) (get-g-fact-set))
                            (fire-rules))
          raw-query-result  (timbre/spy (query new-session anon-query))
          raw-query-result-set (timbre/spy (into #{} raw-query-result))
          who (timbre/spy (get-who raw-query-result-set))
          
          ]
      who)
    (catch Exception e (do (timbre/info
                            "g-respond-sync-who-ptree failed. Error message:"
                            (.getMessage e)
                            ", ptree: " ptree )
                           ans-invalid-query))))



(defn g-respond-sync-whom-ptree "answer WHOM-question, in ptree form (parsed)"
  [ptree]
  (try
    (let [ptreetr (timbre/spy (insta/transform g-transforms ptree))
          anon-query (timbre/spy  (first ptreetr))
          new-tr-rules (timbre/spy (conj @g-rules-tr-atom anon-query))
          new-session   (-> (mk-session
                             (symbol this-ns)
                             new-tr-rules)
                            (#(apply insert %1 %2) (get-g-fact-set))
                            (fire-rules))
          raw-query-result  (timbre/spy (query new-session anon-query))
          raw-query-result-set (timbre/spy (into #{} raw-query-result))
          whom  (timbre/spy (get-who  raw-query-result-set))
          
          ]
      whom)
    (catch Exception e (do (timbre/info
                            "g-respond-sync-whom-ptree failed. Error message:"
                            (.getMessage e)
                            ", ptree: " ptree)
                           ans-invalid-query))))


; (defn g-respond-sync-query "respond to a simple query of the form 'match ?x ..' with a response of the form 'satisfiers: ..'"
;   [qtext]
;   (try
;     (do
;       (let [parsetree  (timbre/spy ((g-grammar) qtext))
;             new-rule-list (timbre/spy (str (get-g-rule-list) qtext))
;             new-session   (-> (mk-session (symbol this-ns) (g-load-user-rules new-rule-list))
;                               (#(apply insert %1 %2) (get-g-fact-set))
;                               (fire-rules))
;             anon-query  (timbre/spy (first (insta/transform g-transforms parsetree)))
;             raw-query-result  (query new-session anon-query)
;             raw-query-result-set (timbre/spy (into #{} raw-query-result))
            
;             ans-vars-txt (timbre/spy (get-ans-vars raw-query-result-set))
;             ]
;         ans-vars-txt
;         ))
;     (catch Exception e (do (timbre/info (.getMessage e)) ans-invalid-query))))

(defn g-respond-sync-query-ptree "respond to a simple query of the form 'match ?x ..' with a response of the form 'satisfiers: ..'"
  [ptree]
  (try
    (do
      (let [;parsetree  (timbre/spy ((g-grammar) qtext))
            ptreetr (timbre/spy (insta/transform g-transforms ptree))
            anon-query (timbre/spy  (first ptreetr))
            new-tr-rules (timbre/spy (conj @g-rules-tr-atom anon-query))
            new-session   (-> (mk-session (symbol this-ns) new-tr-rules)
                              (#(apply insert %1 %2) (get-g-fact-set))
                              (fire-rules))
            raw-query-result  (query new-session anon-query)
            raw-query-result-set (timbre/spy (into #{} raw-query-result))

            ans-vars-txt (timbre/spy (get-ans-vars raw-query-result-set))]
        ans-vars-txt))
    (catch Exception e (do (timbre/info (.getMessage e)) ans-invalid-query))))


;(def g-respond-sync-query g-respond-sync-query-rtxt)


(defn g-respond-sync

  [text]
  (let  [parsetree  ((g-grammar) text)
         intype (first (first parsetree))]
    (cond
      (= intype :FACTS)
      (let [ckst-ptree-r (g-respond-sync-ckst-ptree parsetree)
            ]
        (cond
          (= ckst-ptree-r ans-yes) (do ans-ikr)
          (= ckst-ptree-r ans-no) (do ans-imp)
          (= ckst-ptree-r ans-dunno) (g-respond-sync-mkst-ptree  parsetree)
          :else (ans-oops "g-respond-sync")))

      ; (= intype :AND-FACTS)
      ; (dosync
      ;  (->>  (reduce conj (get-g-fact-set) (map eval (first (g-load-user-facts text))))
      ;        (into #{})
      ;        (set-g-fact-set))
      ;  (let [new-session
      ;        (-> @g-curr-session
      ;            (#(apply insert %1 %2) (get-g-fact-set))
      ;            (fire-rules))]
      ;    (ref-set g-curr-session new-session))
      ;  (str "facts added: " (pr-str (first (g-load-user-facts text)))))

      ;(= intype :QUERY)  (g-respond-sync-query text)
      (= intype :QUERY)  (g-respond-sync-query-ptree parsetree)
      
      (= intype :YNQUESTION)      (g-respond-sync-yndq-ptree parsetree)
      (= intype :NEG-YNQUESTION)  (g-respond-sync-yndq-ptree parsetree)
      (= intype :T-DOES-QUESTION) (g-respond-sync-yndq-ptree parsetree)
      (= intype :NEG-T-DOES-QUESTION) (g-respond-sync-yndq-ptree parsetree)
      
      (= intype :T-WHO-QUESTION) (g-respond-sync-who-ptree parsetree)
      (= intype :T-WHOM-QUESTION) (g-respond-sync-whom-ptree parsetree)

      (= intype :ANON-RULE)
      (do (->>  (str (get-g-rule-list) text ";")
                (set-g-rule-list))
          (let [new-session
                (-> (mk-session (symbol this-ns) @g-rules-tr-atom  )
                    (#(apply insert %1 %2) (get-g-fact-set))
                    (fire-rules))]
            (dosync (ref-set g-curr-session new-session)))
          ans-ok-rule)

      :else (do
              (timbre/info
               "ERROR in g-respond-sync.. parsetree: " parsetree ", intype: " intype)
              ans-sorrywhat) )))









(defn add-voc "add vocabulary"
  [text]
    (let [voc-exp (edn/read-string text)
          vtype (:add-voc-type voc-exp)
          voc-entry (:content voc-exp)
          
          i-verb-set (get-verb-set)
          i-noun-set (get-noun-set)
          i-adj-set (get-adj-set)
          ]
       (cond
         (= vtype :verb)   
             (dosync (if (contains? i-verb-set voc-entry) 
                            (str "I already know this verb: " (:inf voc-entry))
                            (do (->> (conj i-verb-set voc-entry) (into #{}) (set-verb-set) ) (str "Verb added: " (:inf voc-entry)) ) )    )
         (= vtype :noun)
             (dosync (if (contains? i-noun-set voc-entry) 
                            (str "I already know this noun: " (:sing voc-entry))
                            (do (->> (conj i-noun-set voc-entry) (into #{}) (set-noun-set) ) (str "Noun added: " (:sing voc-entry)) ) )    )
         (= vtype :adj)
             (dosync (if (contains? i-adj-set voc-entry) 
                            (str "I already know this adjective: " (:a voc-entry))
                            (do (->> (conj i-adj-set voc-entry) (into #{}) (set-adj-set) ) (str "Adjective added: " (:a voc-entry)) ) )    )
          :else (str "I don't know this word type: " vtype)
         )
           ))





(defn g-respond-sync-top [text]
(let [ukw (unk-words text)
          ]
 (timbre/info "text: " text)
 (cond
    (= text "Forget all facts") (do   (set-g-fact-set g-default-fact-set   )
                                          (let [new-session (-> @g-curr-session 
                                                             (#(apply insert %1 %2) (get-g-fact-set))
                                                             (fire-rules))]
                                             (dosync (ref-set g-curr-session new-session)) )
                                          (do "OK, all facts forgotten."))
   (= text "Forget all rules") (do (timbre/info "forgetting all rules..")
                                   (set-g-rule-list g-default-rule-list )
                                   (let [new-session (-> (mk-session (symbol this-ns) (g-load-user-rules (get-g-rule-list)))
                                                         ( #(apply insert %1 %2) (get-g-fact-set))
                                                          (fire-rules))]
                                       (dosync (ref-set g-curr-session new-session))  )
                                   "OK, all rules forgotten."  )
   
   (re-find (re-pattern "(?i)&caught") text) (str "There was a problem: " text)
   (re-find (re-pattern "(?i)^\\s*(:?hello|hi|hey|howdy|what's\\s+up|how\\s+are\\s+you)") text) "Hello! I understand simple sentences of the form SVO, such as 'Anna likes Bob Smith', and rules like '?x likes ?y when ?y likes ?x'. Give it a try!"
   (:add-voc-type (edn/read-string text))  (add-voc  text)
   (empty? ukw) (g-respond-sync text)
   :else (str "I don't know these words: " (string/join ", " ukw))
   )
))


;;; example, typing this on irc:  {:add-voc-type :verb , :content {:inf \"admire\", :past \"admired\", :pp \"admired\", :er \"admirer\", :ing \"admiring\", :pres3 \"admires\"} }
;;; Becomes this in verb_set.edn: {:inf "admire", :past "admired", :pp "admired", :er "admirer", :ing "admiring", :pres3 "admires"}
;;; Note: if you eliminate a verb from vocab but it remains in a rule, instaparse gives an error and the bot breaks. I have to fix that.

;;; Only use "hear" and "speakup" for multi-user interfaces like irc. The bot may report events asyncronously, not just respond to questions.



(defn g-speakup [speakup-chan] (add-watch last-utterance :utt-ready
                                          (fn [k r old-state new-state]
                                                (>!! speakup-chan (:text new-state) ))))

;; Only use for repl and similar, single-user interfaces. It's syncronous (blocking). 
;(def g-respond g-respond-sync-top)
(defn g-respond [text] (try (g-respond-sync-top text) (catch Exception e (str "caught exception: " (.getMessage e))) ) )


(defn g-hear [text] (future 
                        (reset! last-utterance
                           {:type :response , :text (g-respond text)} )))





(def hear g-hear)
(def speakup g-speakup)
(def respond g-respond)





(defn get-ans-vars-rvec
  "(returns a vector of maps) Ex(but double quotes):
  [{:?x :Bob_Smith, :?y :Anna}]"
  [raw-query-result-set]
  (timbre/spy (map #(dissoc % :?#thing) raw-query-result-set)))

(defn get-ans-vars
  "(returns a string) Ex(but double quotes):
  ('satisfiers: :?x :Bob_Smith, :?y :Anna')"
  [raw-query-result-set]
  (let [ans-vars-vec (get-ans-vars-rvec raw-query-result-set)]
    (str   "satisfiers: " (pr-str ans-vars-vec))))



;;; #"\:\?x\s+\:(\S+)"  ---> (re-pattern "\\:\\?x\\s+\\:(\\S+)")
;;; #"\"([a-z\']+)\"|\'([a-z]+)\'"  --> (re-pattern "\\\"([a-z\\']+)\\\"|\\'([a-z]+)\\'")  ;;;; we also need spaces! --> (re-pattern "\\\"([a-z\\'\\s]+)\\\"|\\'([a-z\\s]+)\\'")
;(defn get-who [x-str] (->> x-str (re-seq (re-pattern "\\:\\?x\\s+\\:(\\S+)") ) (map second) (map #(clojure.string/replace % "_" " ")) (seq->str)  ))


(defn get-who
  "Called by functions that answer who-questions.
  Takes a map set and returns the value of the key :?x"
  [raw-query-result-set]
  (let [who-kw-vec (timbre/spy (map :?x raw-query-result-set))
        who-str-vec (timbre/spy (map NNPkw2str who-kw-vec))
        who-str  (timbre/spy (seq2str who-str-vec))
        ]
    who-str
    )
  
  )

(defn gram-voc [] (set/union #{"it" "case"}  (into #{} (map #(if (second %) (second %) (nth % 2)) (re-seq (re-pattern "\\\"([a-z']+)\\\"|'([a-z]+)'")  (raw-g-grammar-1-w-annex)))))   )
