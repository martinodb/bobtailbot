(defproject bobtailbot "0.1.0-SNAPSHOT"
  :description "A simple REPL chatbot written in Clojure"
  :url "https://github.com/martinodb/bobtailbot"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :main bobtailbot.core
  :dependencies [





;;;;;;;;;;;;;;;;;; ; From CSNePS.
                 [org.clojure/clojure "1.9.0"]
                 [org.clojure/core.match "0.2.1"]
                 [org.clojure/core.memoize "0.5.9"]
                 [org.clojure/math.numeric-tower "0.0.4"]
                 [org.clojure/math.combinatorics "0.1.4"]
                 [org.clojure/tools.trace "0.7.9"]
                 [org.clojure/tools.nrepl "0.2.3"]

;;;;;;;;;;;;;;;;;;

                 ;[org.clojure/tools.cli "0.3.5"]

                 [com.outpace/config "0.10.0"]
                 
                 [com.cerner/clara-rules "0.16.0"]
                 [instaparse "1.4.8"]

                 
                  [org.clojure/core.async "0.4.490"]
                 
                 [com.gearswithingears/async-sockets "0.1.0"]
                 
                 [org.clojure/tools.nrepl "0.2.3"] ; From CSNePS.
                 
                 
                 
                 [stylefruits/gniazdo "1.1.1"]
                 
                 
                 ;; https://github.com/Raynes/fs
                 ;; Tools to manipulate files and dirs
                 [me.raynes/fs "1.4.6"]
                 
                 ;;
                 [org.clojars.martinodb/zinc "1.0.0-SNAPSHOT"] ; clojars version
                 
                 
                 ]
                 
  :profiles
  {:dev {:source-paths ["dev" "src" "test"]
         :dependencies [ [org.clojure/tools.namespace "0.2.4"] ;from CSNePS
                         
         ]}}
                 
  :aliases {"config" ["run" "-m" "outpace.config.generate"]}
  :plugins [ 
             [lein-swank "1.4.5"] ; from CSNePS
             ]
  :repl-options {
     
     :print clojure.core/println 
     ;:print clojure.pprint/pprint
     :timeout 120000
     
     
     }
  
  :repositories [
      ["local" {:url ~(str (.toURI (java.io.File. "local-m2"))) :checksum :warn }  ]
        ]
  
  
  
  
  )


