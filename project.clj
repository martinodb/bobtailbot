(defproject bobtailbot "0.1.0-SNAPSHOT"
  :description "A simple REPL chatbot written in Clojure"
  :url "https://github.com/martinodb/bobtailbot"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :main bobtailbot.core
  :dependencies [[org.clojure/clojure "1.8.0"]
                 
                 ;[clj-http "2.0.0"]
                 ;[hickory "0.5.4"]
                 
                 [com.outpace/config "0.10.0"]
                 
                 [com.cerner/clara-rules "0.16.0"]
                 [instaparse "1.4.8"]
                 [prismatic/schema "1.1.7"]
                 
                 [org.clojure/tools.cli "0.3.5"]
                 [org.clojure/core.async "0.2.395"]
                 [com.gearswithingears/async-sockets "0.1.0"]
                 
                 ;[duratom "0.3.5"]
                 
                 
                 ;[org.clojure/tools.nrepl "0.2.13"]
                 ;[nrepl "0.4.0"]
                 [nrepl "0.5.3"]
                 
                 [stylefruits/gniazdo "1.1.1"]
                 
                 ]
                 
  :profiles
  {:dev {:source-paths ["dev" "src" "test"]
         :dependencies [[org.clojure/tools.namespace "0.2.11"]]}}
                 
  :aliases {"config" ["run" "-m" "outpace.config.generate"]}
  :plugins [ [lein-cljsbuild "1.1.3" :exclusions [org.clojure/clojure]] ;;only for development
             ;[lein-cljsbuild "1.1.7"]
             ] 
                 )


