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
           
           [clojure.edn :as edn]
 
 
 
 ))
 
 
 
 (defn dump-to-path
  "Store a value's representation to a given path"
  [path value]
  (spit path (pr-str value)))

(defn load-from-path
  "Load a value from its representation stored in a given path.
   When reading fails, yield nil"
  [path]
  (try
    (edn/read-string (slurp path))
    (catch Exception _)))

(defn persist-fn
  "Yields an atom watch-fn that dumps new states to a path"
  [path]
  (fn [_ _ _ state]
    (dump-to-path path state)))

(defn file-backed-atom
   "An atom that loads its initial state from a file and persists each new state
    to the same path"
   [path]
   (let [init  (load-from-path path)
         state (atom init)]
     (add-watch state :persist-watcher (persist-fn path))
     state))
 
(def my-fact-list (file-backed-atom (str dir-prefix "store/fact_list.edn")))
 
 
 
 
 
 
 
 
 
 
 
 
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




;{:lhs [{:type bobtailbot.brains.example_shopping.brain.Discount, :constraints [], :fact-binding :?discount}], :params #{}, :name "bobtailbot.brains.example-shopping.brain/get-discounts", :doc "Returns the available discounts."}




;bobtailbot.brains.example-shopping.brain=> (inspect new-session)

;{:rule-matches {{:ns-name bobtailbot.brains.example-shopping.brain, :lhs [{:accumulator (clara.rules.accumulators/sum :cost), :from {:type bobtailbot.brains.example_shopping.brain.Purchase, :constraints []}, :result-binding :?total}], :rhs (do (insert! (->Total ?total))), :name "bobtailbot.brains.example-shopping.brain/total-purchases"} (#clara.tools.inspect.Explanation{:matches ({:fact 230, :condition {:accumulator (clara.rules.accumulators/sum :cost), :from {:type bobtailbot.brains.example_shopping.brain.Purchase, :constraints []}}, :facts-accumulated [#bobtailbot.brains.example_shopping.brain.Purchase{:cost 20, :item :gizmo} #bobtailbot.brains.example_shopping.brain.Purchase{:cost 120, :item :widget} #bobtailbot.brains.example_shopping.brain.Purchase{:cost 90, :item :widget}]}), :bindings {:?total 230}}), {:ns-name bobtailbot.brains.example-shopping.brain, :lhs [{:type bobtailbot.brains.example_shopping.brain.Purchase, :constraints [(= item :gizmo)]}], :rhs (do (reset! last-utterance {:type :alert, :text "someone bought a gizmo!"})), :name "bobtailbot.brains.example-shopping.brain/alert-gizmo-purchase", :doc "Anyone who purchases a gizmo gets a free lunch."} (#clara.tools.inspect.Explanation{:matches ({:fact #bobtailbot.brains.example_shopping.brain.Purchase{:cost 20, :item :gizmo}, :condition {:type bobtailbot.brains.example_shopping.brain.Purchase, :constraints [(= item :gizmo)]}}), :bindings {}}), {:name "my-discount", :lhs ({:type bobtailbot.brains.example_shopping.brain.Customer, :constraints [(clojure.core/= status "platinum")]}), :rhs (clara.rules/insert! (bobtailbot.brains.example-shopping.brain/->Discount "my-discount" 15))} (), {:name "extra-discount", :lhs ({:type bobtailbot.brains.example_shopping.brain.Customer, :constraints [(clojure.core/= status "gold")]} {:type bobtailbot.brains.example_shopping.brain.Total, :constraints [(clojure.core/> value 200)]}), :rhs (clara.rules/insert! (bobtailbot.brains.example-shopping.brain/->Discount "extra-discount" 10))} (#clara.tools.inspect.Explanation{:matches ({:fact #bobtailbot.brains.example_shopping.brain.Customer{:status "gold"}, :condition {:type bobtailbot.brains.example_shopping.brain.Customer, :constraints [(clojure.core/= status "gold")]}} {:fact #bobtailbot.brains.example_shopping.brain.Total{:value 230}, :condition {:type bobtailbot.brains.example_shopping.brain.Total, :constraints [(clojure.core/> value 200)]}}), :bindings {}}), {:name "free-widget-month", :lhs ({:type bobtailbot.brains.example_shopping.brain.Customer, :constraints [(clojure.core/= status "gold")]} {:type bobtailbot.brains.example_shopping.brain.Order, :constraints [(clojure.core/= month "august")]}), :rhs (clara.rules/insert! (bobtailbot.brains.example-shopping.brain/->Promotion "free-widget-month" :free-widget))} (#clara.tools.inspect.Explanation{:matches ({:fact #bobtailbot.brains.example_shopping.brain.Customer{:status "gold"}, :condition {:type bobtailbot.brains.example_shopping.brain.Customer, :constraints [(clojure.core/= status "gold")]}} {:fact #bobtailbot.brains.example_shopping.brain.Order{:year 2013, :month "august", :day 20}, :condition {:type bobtailbot.brains.example_shopping.brain.Order, :constraints [(clojure.core/= month "august")]}}), :bindings {}}), {:name "gold-summer-discount", :lhs ({:type bobtailbot.brains.example_shopping.brain.Customer, :constraints [(clojure.core/= status "gold")]} {:type bobtailbot.brains.example_shopping.brain.Order, :constraints [(clojure.core/= month "august")]}), :rhs (clara.rules/insert! (bobtailbot.brains.example-shopping.brain/->Discount "gold-summer-discount" 20))} (#clara.tools.inspect.Explanation{:matches ({:fact #bobtailbot.brains.example_shopping.brain.Customer{:status "gold"}, :condition {:type bobtailbot.brains.example_shopping.brain.Customer, :constraints [(clojure.core/= status "gold")]}} {:fact #bobtailbot.brains.example_shopping.brain.Order{:year 2013, :month "august", :day 20}, :condition {:type bobtailbot.brains.example_shopping.brain.Order, :constraints [(clojure.core/= month "august")]}}), :bindings {}})}, :query-matches {{:lhs [{:type bobtailbot.brains.example_shopping.brain.Discount, :constraints [], :fact-binding :?discount}], :params #{}, :name "bobtailbot.brains.example-shopping.brain/get-discounts", :doc "Returns the available discounts."} (#clara.tools.inspect.Explanation{:matches ({:fact #bobtailbot.brains.example_shopping.brain.Discount{:name "extra-discount", :percent 10}, :condition {:type bobtailbot.brains.example_shopping.brain.Discount, :constraints []}}), :bindings {:?discount #bobtailbot.brains.example_shopping.brain.Discount{:name "extra-discount", :percent 10}}} #clara.tools.inspect.Explanation{:matches ({:fact #bobtailbot.brains.example_shopping.brain.Discount{:name "gold-summer-discount", :percent 20}, :condition {:type bobtailbot.brains.example_shopping.brain.Discount, :constraints []}}), :bindings {:?discount #bobtailbot.brains.example_shopping.brain.Discount{:name "gold-summer-discount", :percent 20}}}), {:lhs [{:type bobtailbot.brains.example_shopping.brain.Promotion, :constraints [], :fact-binding :?discount}], :params #{}, :name "bobtailbot.brains.example-shopping.brain/get-promotions", :doc "Returns the available promotions."} (#clara.tools.inspect.Explanation{:matches ({:fact #bobtailbot.brains.example_shopping.brain.Promotion{:reason "free-widget-month", :type :free-widget}, :condition {:type bobtailbot.brains.example_shopping.brain.Promotion, :constraints []}}), :bindings {:?discount #bobtailbot.brains.example_shopping.brain.Promotion{:reason "free-widget-month", :type :free-widget}}}), #function[clojure.lang.AFunction/1] (#clara.tools.inspect.Explanation{:matches (), :bindings {}})}, :condition-matches {{:type bobtailbot.brains.example_shopping.brain.Purchase, :constraints [(= item :gizmo)]} (#bobtailbot.brains.example_shopping.brain.Purchase{:cost 20, :item :gizmo}), {:type bobtailbot.brains.example_shopping.brain.Discount, :constraints [], :fact-binding :?discount} (#bobtailbot.brains.example_shopping.brain.Discount{:name "extra-discount", :percent 10} #bobtailbot.brains.example_shopping.brain.Discount{:name "gold-summer-discount", :percent 20}), {:type bobtailbot.brains.example_shopping.brain.Promotion, :constraints [], :fact-binding :?discount} (#bobtailbot.brains.example_shopping.brain.Promotion{:reason "free-widget-month", :type :free-widget}), {:type bobtailbot.brains.example_shopping.brain.Customer, :constraints [(clojure.core/= status "platinum")]} (), {:type bobtailbot.brains.example_shopping.brain.Customer, :constraints [(clojure.core/= status "gold")]} (#bobtailbot.brains.example_shopping.brain.Customer{:status "gold"}), {:type bobtailbot.brains.example_shopping.brain.Total, :constraints [(clojure.core/> value 200)]} (#bobtailbot.brains.example_shopping.brain.Total{:value 230}), {:type bobtailbot.brains.example_shopping.brain.Order, :constraints [(clojure.core/= month "august")]} (#bobtailbot.brains.example_shopping.brain.Order{:year 2013, :month "august", :day 20})}, :insertions {{:ns-name bobtailbot.brains.example-shopping.brain, :lhs [{:accumulator (clara.rules.accumulators/sum :cost), :from {:type bobtailbot.brains.example_shopping.brain.Purchase, :constraints []}, :result-binding :?total}], :rhs (do (insert! (->Total ?total))), :name "bobtailbot.brains.example-shopping.brain/total-purchases"} ({:explanation #clara.tools.inspect.Explanation{:matches ({:fact 230, :condition {:accumulator (clara.rules.accumulators/sum :cost), :from {:type bobtailbot.brains.example_shopping.brain.Purchase, :constraints []}}, :facts-accumulated [#bobtailbot.brains.example_shopping.brain.Purchase{:cost 20, :item :gizmo} #bobtailbot.brains.example_shopping.brain.Purchase{:cost 120, :item :widget} #bobtailbot.brains.example_shopping.brain.Purchase{:cost 90, :item :widget}]}), :bindings {:?total 230}}, :fact #bobtailbot.brains.example_shopping.brain.Total{:value 230}}), {:ns-name bobtailbot.brains.example-shopping.brain, :lhs [{:type bobtailbot.brains.example_shopping.brain.Purchase, :constraints [(= item :gizmo)]}], :rhs (do (reset! last-utterance {:type :alert, :text "someone bought a gizmo!"})), :name "bobtailbot.brains.example-shopping.brain/alert-gizmo-purchase", :doc "Anyone who purchases a gizmo gets a free lunch."} (), {:name "my-discount", :lhs ({:type bobtailbot.brains.example_shopping.brain.Customer, :constraints [(clojure.core/= status "platinum")]}), :rhs (clara.rules/insert! (bobtailbot.brains.example-shopping.brain/->Discount "my-discount" 15))} (), {:name "extra-discount", :lhs ({:type bobtailbot.brains.example_shopping.brain.Customer, :constraints [(clojure.core/= status "gold")]} {:type bobtailbot.brains.example_shopping.brain.Total, :constraints [(clojure.core/> value 200)]}), :rhs (clara.rules/insert! (bobtailbot.brains.example-shopping.brain/->Discount "extra-discount" 10))} ({:explanation #clara.tools.inspect.Explanation{:matches ({:fact #bobtailbot.brains.example_shopping.brain.Customer{:status "gold"}, :condition {:type bobtailbot.brains.example_shopping.brain.Customer, :constraints [(clojure.core/= status "gold")]}} {:fact #bobtailbot.brains.example_shopping.brain.Total{:value 230}, :condition {:type bobtailbot.brains.example_shopping.brain.Total, :constraints [(clojure.core/> value 200)]}}), :bindings {}}, :fact #bobtailbot.brains.example_shopping.brain.Discount{:name "extra-discount", :percent 10}}), {:name "free-widget-month", :lhs ({:type bobtailbot.brains.example_shopping.brain.Customer, :constraints [(clojure.core/= status "gold")]} {:type bobtailbot.brains.example_shopping.brain.Order, :constraints [(clojure.core/= month "august")]}), :rhs (clara.rules/insert! (bobtailbot.brains.example-shopping.brain/->Promotion "free-widget-month" :free-widget))} ({:explanation #clara.tools.inspect.Explanation{:matches ({:fact #bobtailbot.brains.example_shopping.brain.Customer{:status "gold"}, :condition {:type bobtailbot.brains.example_shopping.brain.Customer, :constraints [(clojure.core/= status "gold")]}} {:fact #bobtailbot.brains.example_shopping.brain.Order{:year 2013, :month "august", :day 20}, :condition {:type bobtailbot.brains.example_shopping.brain.Order, :constraints [(clojure.core/= month "august")]}}), :bindings {}}, :fact #bobtailbot.brains.example_shopping.brain.Promotion{:reason "free-widget-month", :type :free-widget}}), {:name "gold-summer-discount", :lhs ({:type bobtailbot.brains.example_shopping.brain.Customer, :constraints [(clojure.core/= status "gold")]} {:type bobtailbot.brains.example_shopping.brain.Order, :constraints [(clojure.core/= month "august")]}), :rhs (clara.rules/insert! (bobtailbot.brains.example-shopping.brain/->Discount "gold-summer-discount" 20))} ({:explanation #clara.tools.inspect.Explanation{:matches ({:fact #bobtailbot.brains.example_shopping.brain.Customer{:status "gold"}, :condition {:type bobtailbot.brains.example_shopping.brain.Customer, :constraints [(clojure.core/= status "gold")]}} {:fact #bobtailbot.brains.example_shopping.brain.Order{:year 2013, :month "august", :day 20}, :condition {:type bobtailbot.brains.example_shopping.brain.Order, :constraints [(clojure.core/= month "august")]}}), :bindings {}}, :fact #bobtailbot.brains.example_shopping.brain.Discount{:name "gold-summer-discount", :percent 20}})}, :fact->explanations {#bobtailbot.brains.example_shopping.brain.Total{:value 230} [{:rule {:ns-name bobtailbot.brains.example-shopping.brain, :lhs [{:accumulator (clara.rules.accumulators/sum :cost), :from {:type bobtailbot.brains.example_shopping.brain.Purchase, :constraints []}, :result-binding :?total}], :rhs (do (insert! (->Total ?total))), :name "bobtailbot.brains.example-shopping.brain/total-purchases"}, :explanation #clara.tools.inspect.Explanation{:matches ({:fact 230, :condition {:accumulator (clara.rules.accumulators/sum :cost), :from {:type bobtailbot.brains.example_shopping.brain.Purchase, :constraints []}}, :facts-accumulated [#bobtailbot.brains.example_shopping.brain.Purchase{:cost 20, :item :gizmo} #bobtailbot.brains.example_shopping.brain.Purchase{:cost 120, :item :widget} #bobtailbot.brains.example_shopping.brain.Purchase{:cost 90, :item :widget}]}), :bindings {:?total 230}}}], #bobtailbot.brains.example_shopping.brain.Discount{:name "extra-discount", :percent 10} [{:rule {:name "extra-discount", :lhs ({:type bobtailbot.brains.example_shopping.brain.Customer, :constraints [(clojure.core/= status "gold")]} {:type bobtailbot.brains.example_shopping.brain.Total, :constraints [(clojure.core/> value 200)]}), :rhs (clara.rules/insert! (bobtailbot.brains.example-shopping.brain/->Discount "extra-discount" 10))}, :explanation #clara.tools.inspect.Explanation{:matches ({:fact #bobtailbot.brains.example_shopping.brain.Customer{:status "gold"}, :condition {:type bobtailbot.brains.example_shopping.brain.Customer, :constraints [(clojure.core/= status "gold")]}} {:fact #bobtailbot.brains.example_shopping.brain.Total{:value 230}, :condition {:type bobtailbot.brains.example_shopping.brain.Total, :constraints [(clojure.core/> value 200)]}}), :bindings {}}}], #bobtailbot.brains.example_shopping.brain.Promotion{:reason "free-widget-month", :type :free-widget} [{:rule {:name "free-widget-month", :lhs ({:type bobtailbot.brains.example_shopping.brain.Customer, :constraints [(clojure.core/= status "gold")]} {:type bobtailbot.brains.example_shopping.brain.Order, :constraints [(clojure.core/= month "august")]}), :rhs (clara.rules/insert! (bobtailbot.brains.example-shopping.brain/->Promotion "free-widget-month" :free-widget))}, :explanation #clara.tools.inspect.Explanation{:matches ({:fact #bobtailbot.brains.example_shopping.brain.Customer{:status "gold"}, :condition {:type bobtailbot.brains.example_shopping.brain.Customer, :constraints [(clojure.core/= status "gold")]}} {:fact #bobtailbot.brains.example_shopping.brain.Order{:year 2013, :month "august", :day 20}, :condition {:type bobtailbot.brains.example_shopping.brain.Order, :constraints [(clojure.core/= month "august")]}}), :bindings {}}}], #bobtailbot.brains.example_shopping.brain.Discount{:name "gold-summer-discount", :percent 20} [{:rule {:name "gold-summer-discount", :lhs ({:type bobtailbot.brains.example_shopping.brain.Customer, :constraints [(clojure.core/= status "gold")]} {:type bobtailbot.brains.example_shopping.brain.Order, :constraints [(clojure.core/= month "august")]}), :rhs (clara.rules/insert! (bobtailbot.brains.example-shopping.brain/->Discount "gold-summer-discount" 20))}, :explanation #clara.tools.inspect.Explanation{:matches ({:fact #bobtailbot.brains.example_shopping.brain.Customer{:status "gold"}, :condition {:type bobtailbot.brains.example_shopping.brain.Customer, :constraints [(clojure.core/= status "gold")]}} {:fact #bobtailbot.brains.example_shopping.brain.Order{:year 2013, :month "august", :day 20}, :condition {:type bobtailbot.brains.example_shopping.brain.Order, :constraints [(clojure.core/= month "august")]}}), :bindings {}}}]}}
