; original: https://github.com/AdamBoxall/clojure-irc-client

(ns bobtailbot.irc
  (:gen-class)
  (:require [clojure.string :as string]
            [clojure.tools.cli :as cli]
            [clojure.core.async :as async]
            [com.gearswithingears.async-sockets :refer :all]))


(def connected (atom false))
(def curr-host (atom "127.0.0.1"))

(def ready-server-msg 
 (delay 
  (case @curr-host
  "127.0.0.1" ":Current Global Users:"
  "chat.freenode.net" "End of /MOTD command.")))


(defn write
  ([socket message]
   (write socket message false))
  ([socket message print?]
   (when print?
     (println message))
   (async/>!! (:out socket) (str message "\r"))))


(defn write-privmsg
  ([socket privmsg irc-channel ]
   (write-privmsg socket privmsg irc-channel false))
  ([socket privmsg irc-channel print?]
   (when print?
     (println privmsg))
   (async/>!! (:out socket) (str "PRIVMSG "  irc-channel " :" privmsg "\r"))))



(defn login-as-guest [socket nick]
  (println (str "Logging in as guest " nick))
  (write socket (str "NICK " nick))
  (write socket (str "USER " nick " 0 * :" nick))
  (println "done logging in")
  )

(defn input-listener [socket]
  (loop []
    (let [input (read-line)]
      (println input)
      
      (recur))))





(defn handle-line [socket line irc-channel hear-fn]
  (println line)
  (cond
    (re-find #"^ERROR :Closing Link:" line)
       (close-socket-client socket)
    (re-find #"^PING" line)
       (write socket (str "PONG " (re-find #":.*" line)) :print)
    (re-find (re-pattern @ready-server-msg) line)
      (swap! connected (constantly true))
    (re-find #"PRIVMSG" line)
       (let [
          msg-user (second (re-find #"^\:(\S+)\!" line))
          msg-content (second (re-find (re-pattern "^:.+:(.*)") line))
          ;;Geany goes crazy with this regex, so I use "re-pattern" instead.
          
          ;reply-msg (apply str (respond-fn msg-content))
          ]
              (cond 
                (re-find #"^quit" msg-content) (swap! connected (constantly false))
                 :else (do  (hear-fn msg-content);(write-privmsg socket reply-msg irc-channel :print)
                            )))))

(defn message-listener [socket irc-channel hear-fn]
  (async/go-loop []
    (when-let [line (async/<! (:in socket))]
      (handle-line socket line irc-channel hear-fn)
      (recur))))

(defn speaker-up [socket irc-channel speakup-fn]
  (let [speakup-chan (async/chan)]
    (do (speakup-fn speakup-chan)
        (async/go-loop []
             (let [privmsg (async/<! speakup-chan)]
             (write-privmsg socket privmsg irc-channel)
             (Thread/sleep 500) ; to avoid flood
             (recur))))))



(defn connect [nick host port irc-channel greeting hear-fn speakup-fn]
  (println "Connecting...")
  (swap! curr-host (constantly host))
  (try
    (let [socket (socket-client port host)]
      (println (str "Connected to " host ":" port))
       
      
       (message-listener socket irc-channel hear-fn)
       (Thread/sleep 1000)
       (login-as-guest socket nick)
       ;(Thread/sleep 500)
      
       (println (str "connected? :" @connected))
       (while (= @connected false) (Thread/sleep 100))
       (println (str "connected? :" @connected))
       
       (Thread/sleep 500)
       (write socket (str "JOIN " irc-channel) true)
       (Thread/sleep 1000)
       (write socket (str "PRIVMSG " irc-channel " :" greeting) true)
       (Thread/sleep 500)
       
       ;(speakup-fn socket irc-channel)
       (speaker-up socket irc-channel speakup-fn)
       
       (while (= @connected true) (Thread/sleep 100))
       (println "disconnecting now")
       (write socket "QUIT"))
    (catch Exception e
      (println (str "Failed to connect to " host ":" port "\n" (str e))))))


