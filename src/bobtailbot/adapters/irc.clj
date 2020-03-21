; original: https://github.com/AdamBoxall/clojure-irc-client

(ns bobtailbot.adapters.irc
  (:gen-class)
  (:require 
   [taoensso.timbre :as timbre   :refer []]
   [bobtailbot.tools :as tools :refer [tim-ret]]
   
   [outpace.config :refer [defconfig]]
   [clojure.string :as string]
   [clojure.tools.cli :as cli]
   [clojure.core.async :as async]
   [com.gearswithingears.async-sockets :as ga :refer [socket-client  close-socket-client]]))



;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;;;;;;;; Adapter-specific chat config options. They override global options if "use-global?" is set to false. CLI options override all.
;;;;;;;;; All configs can be set in the file "config.edn"
;;;;;;;;; 
;;;Some irc hosts. Can't use defconfig for this.  In the config file, use the IP address directly.;;
(def irc--localhost "127.0.0.1")
(def irc--freenode "149.56.134.238") ;; nslookup chat.freenode.net
;;;;;;;
;;;;;;;
(defconfig use-global? false) ; true: use global configs. false: override global configs.
(defconfig nick "bobtailbot")
(defconfig host irc--localhost)
(defconfig port 6667)
(defconfig group-or-chan nick) ; eg: "bobtailbot" ; if a prefix such as "#" is needed, the adapter must add it.
(defconfig greeting "Hello.  Let's chat.")




;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;



(defn ip->hostname [ip]
  (condp = ip
    irc--freenode "chat.freenode.net"
    "localhost"
    )
  )



(def connected (atom false))
(def curr-host (atom irc--localhost))
(def default-ready-server-msg "\\s+001\\s+\\S+\\s+:Welcome")



(def ready-server-msg 
 (delay 
  (condp = @curr-host ;; don't use 'case' here, it only works with literal values.
    irc--localhost default-ready-server-msg
    irc--freenode default-ready-server-msg
    default-ready-server-msg 
    )))


(def sasl-succ-msg "^:\\S+\\s+903\\s+\\S+\\s+:SASL")



(defn write
  ([socket message]
   (write socket message false))
  ([socket message print?]
   (when print?
     (timbre/info message))
   (async/>!! (:out socket) (str message "\r"))))


(defn write-privmsg
  ([socket privmsg irc-channel ]
   (write-privmsg socket privmsg irc-channel false))
  ([socket privmsg irc-channel print?]
   (when print?
     (timbre/info privmsg))
   (async/>!! (:out socket) (str "PRIVMSG "  irc-channel " :" privmsg "\r"))))



(defn login-as-guest [socket nick]
  (timbre/info (str "Logging in as guest " nick))
  (write socket (str "NICK " nick))
  (write socket (str "USER " nick " 0 * :" nick))
  (timbre/info "done logging in")
  )

(defn login-as-user [socket nick]
  (let [nick-vec (string/split nick #"\\0")
        act-nick (first nick-vec)
        username (second nick-vec)
        ;password (nth nick-vec 2)
        ]
    
    (timbre/info (str "Logging in as user " act-nick))
    (write socket (str "NICK " act-nick))
    (write socket (str "USER " act-nick " " username " " (ip->hostname @curr-host) " :" act-nick))
    (write socket "AUTHENTICATE PLAIN"))
  )


(defn login [socket nick]
  (cond
    (re-find #"\\0" nick) (login-as-user socket nick)
    :else (login-as-guest socket nick) 
    )
  )

(defn input-listener [socket]
  (loop []
    (let [input (read-line)]
      (timbre/info input)
      
      (recur))))





(defn handle-line [socket line irc-channel hear-fn nick]
 (let [e-line (string/replace line (re-pattern "\\\"")  #(str "\\" %)  )] ; escape user double-quotes
  (try  (timbre/info "line: " line "\n" "e-line: " e-line) ; debugging.
        (cond
          (re-find #"^ERROR :Closing Link:" e-line)
          (ga/close-socket-client socket)
          
          (re-find #"^PING" e-line)
          (write socket (str "PONG " (re-find #":.*" e-line)) :print)
          
          (re-find #"^AUTHENTICATE +" e-line) ; nick must be "Bobtailbot\\0Bobtailbot\\0yourpasswordgoeshere"
          (write socket (str "AUTHENTICATE " (String. (.encode (java.util.Base64/getEncoder) (.getBytes nick "UTF-8")) "UTF-8")) :print)
          
          (re-find (re-pattern sasl-succ-msg) e-line)
          (timbre/info "done logging in as user")
          
          (re-find (re-pattern @ready-server-msg) e-line)
          (do (Thread/sleep 50) (swap! connected (constantly true)))
          
          (re-find #"PRIVMSG" e-line)
          (let [
                msg-user (second (re-find #"^\:(\S+)\!" e-line))
                msg-content (second (re-find (re-pattern ":[^:]+:(.*)") e-line)) ; 
                      ;;Scintilla can't handle the Clojure regex reader macro , so I use "re-pattern" instead.
                ]
            (cond 
              (re-find #"^quit" msg-content) (swap! connected (constantly false))
              :else (do
                                  ;(timbre/info "msg-user: " msg-user "\n" "msg-content: " msg-content) ; debugging
                      (hear-fn msg-content)
                      )))
          :else
          (timbre/info (str "handle-line: no matching clause in cond. Some other message kind."))
          )
        (catch Exception e
          (do (timbre/info "stacktrace: " (timbre/info e))
              (hear-fn 
               (str "handle-line: &caught exception: " (or (.getMessage e) "(no message)")
                    ))))) ) )

(defn message-listener [socket irc-channel hear-fn nick]
  (async/go-loop []
    (when-let [line (async/<! (:in socket))]
      (handle-line socket line irc-channel hear-fn nick)
      (recur))))

(defn speaker-up [socket irc-channel speakup-fn]
  (let [speakup-chan (async/chan)]
    (do (speakup-fn speakup-chan)
        (async/go-loop []
             (let [privmsg (async/<! speakup-chan)]
             (write-privmsg socket privmsg irc-channel)
             (Thread/sleep 500) ; to avoid flood
             (recur))))))



(defn connect [{:keys [nick host port group-or-chan greeting hear speakup ]}]; respond will be ignored.
  (timbre/info "Connecting...")
  (swap! curr-host (constantly host))
  (try
    (let [socket (ga/socket-client port host)
          irc-channel (str "#" group-or-chan)
           ]
      (timbre/info (str "Connected to " host ":" port))
       
      
       (message-listener socket irc-channel hear nick)
       (Thread/sleep 50)
       (login socket nick)
       (timbre/info (str "connected? :" @connected))
       (while (= @connected false) (Thread/sleep 100))
       (timbre/info (str "connected? :" @connected))
       (Thread/sleep 50)
       (write socket (str "JOIN " irc-channel) true)
       (Thread/sleep 50)
       (write socket (str "PRIVMSG " irc-channel " :" greeting) true)
       (Thread/sleep 50)
       (speaker-up socket irc-channel speakup)
       (while (= @connected true) (Thread/sleep 100))
       (timbre/info "disconnecting now")
       (write socket "QUIT")
       )
    (catch Exception e
      (timbre/info (str "Failed to connect to " host ":" port "\n" (str e))))))


