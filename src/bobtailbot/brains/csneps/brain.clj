(ns bobtailbot.brains.csneps.brain
  "CSNePS as backend"
  (:require [clojure.string :as string]
            [clojure.core.async :as async 
               :refer [go-loop <! <!! >! >!!  close! chan pub sub]]
            
            
            
            ;https://nrepl.org/nrepl/hacking_on_nrepl.html
            [nrepl.core :as nrepl]
            
            [gniazdo.core :as ws]
            
            
            [csneps.core.snuser :as s]
            
            
            ;[instaparse.core :as insta]
            ;[schema.core :as sc]
            ))


;; IMPORTANT!!!
;; Change this prefix if you change this file's name (or path).
;;Also remember to change the ns declaration.
(def parent-ns "bobtailbot.brains.csneps")
(def this-ns-unqual "brain")
;;;;;;;
(def this-ns (str parent-ns "." this-ns-unqual))
(def ns-prefix (str this-ns "/"))
(def this-dir (str "./src/" (-> parent-ns (string/replace #"\." "/" ) (string/replace #"-" "_")) ))
(def dir-prefix (str this-dir "/" ))






;;;
;;; HOST AND PORTS OF RUNNING CSNEPS SERVER
;;; FOR BOTH NREPL AND WSSOCKETS
;;;

;(def ws-port "10000")
(def ws-port "8080")
(def ws-host "localhost")

(def df-nrepl-port 37785)
(def nrepl-host "localhost")









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



(def mytext-1 "hello, one")

(def mytext-2 "(str \"hello\")" )


(def let-example (let [text3 (str "(str \\\"" mytext-1 "\\\")") ]   (str text3) ))



(defn respond-sync-stub [text]
  (if (question-mark? text) "Nice question."
        (case text 
         "alert on"  (do (reset! alert-wanted? true)
                         (if (not @init-alert-gen?) alert-generator )
                         "alert is now on!"
                          )
         "alert off" (do (reset! alert-wanted? false)
                          "alert is now off!")
         "I see.")))

;(defn respond-sync-csneps [text]
  ;(with-open
     ;;actual csneps system (port is different each time):
     ;[conn (nrepl/connect  :port df-nrepl-port)]
     
     ;; minimal nrepl server, not csneps.
     ;;[conn (nrepl/connect :port 7888)]
     
     
     ;(-> (nrepl/client conn 1000)
       
       
       
       
       ;(nrepl/message  {:op :eval :code (str text)})
       
       
       ;nrepl/response-values
       
       
       ;first
       
       
       ;)))


(defn just-one "pick the first element if it's a coll, leave the coll as is if it's more"
  [indorcoll]
    (if (coll? indorcoll)
           (if (= (count indorcoll) 1) (first indorcoll) indorcoll)
        indorcoll ))




; https://clojuredocs.org/clojure.core/with-out-str
(defmacro BAD-with-out-str-data-map
  [& body]
  `(let [s# (new java.io.StringWriter)]
     (binding [*out* s#]
       (let [r# (just-one ~body)]
         {:result r#
          :str    (str s#)}))))

(defmacro with-out-str-data-map
  [& body]
  `(let [s# (new java.io.StringWriter)]
     (binding [*out* s#]
       (let [r#  ~@body]
         {:result r#
          :str    (str s#)}))))



(defmacro stringed
  [& body]
  `(let [r#  ~@body]
     (str r#)
      ))




;;pick one:
(def ws-connect-message (str "ws://" ws-host ":" ws-port "/echo" ))
;(def ws-connect-message (str "ws://" ws-host ":" ws-port "/chat" ))
;(def ws-connect-message (str "ws://" ws-host ":" ws-port "/socket" ))
;(def ws-connect-message (str "ws://" ws-host ":" ws-port "/wssocket" ))

(def echo-fn #(prn 'received %))



(defn defaultwssfn "default websocket conn maker function"[]
  (delay (ws/connect ws-connect-message
    :on-receive echo-fn)))


;(def defaultwss
  ;(ws/connect ws-connect-message
    ;:on-receive echo-fn))

; default websocket connection
(def defaultwss (future (defaultwssfn)))



;send websocket message
(defn send-msg-ws
     ([msg] (ws/send-msg defaultwss msg))
     ([msg socket] (ws/send-msg socket msg) )
     )

; close websocket
(defn close-ws
    ([] (ws/close defaultwss)  )
    ([socket] (ws/close socket) )
    )






(defn respond-sync-csneps-nrepl-outstr
     ([text]   (respond-sync-csneps-nrepl-outstr  text  df-nrepl-port)   )
     
     ([text nport] (with-open   [conn (nrepl/connect  :port nport)]
                       (-> (nrepl/client conn 1000)
                           (nrepl/message  {:op :eval :code text})
                            
                            ;(nrepl/response-seq 200)
                            (nrepl/response-values)
                            
                           ; (stringed)
                           ;(prn)
                           ;((fn [z ] (if (empty? z) "hum" z)))
                           ;((fn [y ] (string/escape (str y) {\: "--" , \! "_BANG_"})))
                           ;((fn [x] (if (string? x)  (fn [y] (string/escape y {\: "--" , \! "_BANG_"})) x)))
                           ;(fn [z] (if z z "hum"))
                          
                           ;println ; debug
                           ;first 
                           ;println ; debug
                           ;(with-out-str-data-map)
                           
                           ))) )




(defn respond-sync-csneps-nrepl-outstr-combined 
   ([text]   (respond-sync-csneps-nrepl-outstr-combined  text  df-nrepl-port))
   ([text nport] (let [text2-bis (respond-sync-csneps-nrepl-outstr text)
                       text2 (if text2-bis text2-bis "text2-bis empty")
                       text3  (str (if (not (string/blank? (:str text2 )))
                                      (str (:str text2) "\n")
                                      "" )
                                   (:result text2))]
                    (do  (prn "text2: " text2 ", text3: " text3 ", (read-string text3): " "(read-string text3)" ) ;debug
                         (if (string/blank? text3) "ok"  text3 ;(read-string text3)  ;(read-string text3)
                          )
                         
                         
                           )  ) ) )


(defn respond-sync-csneps-nrepl-wrapstring
  "wrap with string quotes all expressions that don't have a leading parenthesis"
  ([text]   (respond-sync-csneps-nrepl-wrapstring  text  df-nrepl-port))
  ([text nport]   (if (= (first text) (first "(") ) 
                         (do (println (first text) " is a paren" )
                             (respond-sync-csneps-nrepl-outstr-combined text nport))
                      (do (println (first text) "is not a paren")
                          (respond-sync-csneps-nrepl-outstr-combined (pr-str (str  text )) nport )))))


;; Only use for repl and similar, single-user interfaces. It's syncronous (blocking). 
(def respond respond-sync-csneps-nrepl-wrapstring)


;;; Only use "hear" and "speakup" for multi-user interfaces like irc. The bot may report events asyncronously, not just respond to questions.
(defn hear [text-in] (future 
                        (reset! last-utterance
                           {:type :response , :text (respond text-in)} )))


(defn speakup [speakup-chan] (add-watch last-utterance :utt-ready
                                          (fn [k r old-state new-state]
                                                (>!! speakup-chan (:text new-state) ))))

