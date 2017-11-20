; original: https://github.com/AdamBoxall/clojure-irc-client

(ns bobtailbot.irc
  (:gen-class)
  (:require [clojure.string :as string]
            [clojure.tools.cli :as cli]
            [clojure.core.async :as async]
            [com.gearswithingears.async-sockets :refer :all]))

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


(defn handle-line [socket line irc-channel respond-fn]
  (println line)
  (cond
    (re-find #"^ERROR :Closing Link:" line)
       (close-socket-client socket)
    (re-find #"^PING" line)
       (write socket (str "PONG " (re-find #":.*" line)) :print)
    (re-find #"PRIVMSG" line)
       (let [
          msg-user (second (re-find #"^\:(\S+)\!" line))
          msg-content (second (re-find (re-pattern "^:.+:(.*)") line))
          ;;Geany goes crazy with this regex, so I use "re-pattern" instead.
          
          reply-msg (apply str (respond-fn msg-content))]
              (cond 
                (re-find #"^quit" msg-content) (write socket "QUIT")
                 :else (do (write-privmsg socket reply-msg irc-channel :print)
                            (Thread/sleep 2000))))))

(defn message-listener [socket irc-channel respond-fn]
  (async/go-loop []
    (when-let [line (async/<! (:in socket))]
      (handle-line socket line irc-channel respond-fn)
      (recur))))

(defn connect [nick host port irc-channel greeting respond-fn speakup-fn]
  (println "Connecting...")
  (try
    (let [socket (socket-client port host)]
      (println (str "Connected to " host ":" port))
       (login-as-guest socket nick)
      
       (message-listener socket irc-channel respond-fn)
      
       ;(Thread/sleep 7000)
       (Thread/sleep 15000)
       
       (write socket (str "JOIN " irc-channel) true)
       (Thread/sleep 1000)
       (write socket (str "PRIVMSG " irc-channel " :" greeting) true)
       
       (speakup-fn socket irc-channel)
       
       (Thread/sleep 30000)
       ;(write socket "QUIT")
       
       (Thread/sleep 1000)
       
       
       
       ; it's a loop, so put it last.
       ;(input-listener socket)
       
       )
    (catch Exception e
      (println (str "Failed to connect to " host ":" port)))))





;(def cli-usage
  ;[["-n" "--nick NICK" "Nickname" :default "examplenickdonotuse"]
   ;["-h" "--host HOST" "Hostname" :default "127.0.0.1"]
   ;["-p" "--port PORT" "Port number" :parse-fn #(Integer/parseInt %) :default 6667]])

;(defn -main [& args]
  ;(let [{:keys [options summary]} (cli/parse-opts args cli-usage)
           ;]
    ;(if (or (not (:nick options)) (not (:port options)) (not (:host options)))
      ;(println summary)
      ;(do 
        ;(connect
          ;(-> options :nick string/trim)
          ;(-> options :host string/trim)
          ;(:port options))

      ;)
;)))


