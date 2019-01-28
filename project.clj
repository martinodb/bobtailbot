;;;
;;;
;;;
;;;
;;;


(defproject bobtailbot "0.1.0-SNAPSHOT"
  :description "A simple REPL chatbot written in Clojure"
  :url "https://github.com/martinodb/bobtailbot"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :main bobtailbot.core
  :dependencies [
                 ;[org.clojure/clojure "1.8.0"] ; My tested version.




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

                 ;[prismatic/schema "1.1.7"] It's not used.





                 
                 ;[clj-http "2.0.0"]
                 ;[hickory "0.5.4"]
                 
                 [com.outpace/config "0.10.0"]
                 
                 [com.cerner/clara-rules "0.16.0"]
                 [instaparse "1.4.8"]

                 

                 
                 ;[org.clojure/core.async "0.2.395"]; this one breaks with Clojure "1.9.0"
                  [org.clojure/core.async "0.4.490"]
                 
                 [com.gearswithingears/async-sockets "0.1.0"]
                 
                 ;[duratom "0.3.5"]
                 
                 
                 ;[org.clojure/tools.nrepl "0.2.13"]
                 ;[nrepl "0.4.0"]
                 
                 ;[nrepl "0.5.3"] ;My best tested version.
                 [org.clojure/tools.nrepl "0.2.3"] ; From CSNePS.
                 
                 
                 
                 [stylefruits/gniazdo "1.1.1"]
                 
                 ;[aleph "0.4.6"] ; Below we avoid conflict. do "lein deps :tree" to see why.
                 ;[aleph "0.4.6" :exclusions [org.clojure/tools.logging]]
                 
                 ; aleph seems better. Remove gniazdo later.
                 
                 ;these are required in the aleph websocket demo.
                 ;[gloss "0.2.6"]
                 ;[compojure "1.6.1"]
                 
                 ;; https://github.com/Raynes/fs
                 ;; Tools to manipulate files and dirs
                 [me.raynes/fs "1.4.6"]
                 
                 ;;
                 ;;
                 ;;
                 ;;
                 ;;
                 [org.clojars.martinodb/zinc "1.0.0-SNAPSHOT"] ; clojars version
                 
                 
                 ]
                 
  :profiles
  {:dev {:source-paths ["dev" "src" "test"]
         :dependencies [ ;[org.clojure/tools.namespace "0.2.11"]
                         [org.clojure/tools.namespace "0.2.4"] ;from CSNePS
                         
         ]}}
                 
  :aliases {"config" ["run" "-m" "outpace.config.generate"]}
  :plugins [ 
             ;[lein-cljsbuild "1.1.3" :exclusions [org.clojure/clojure]] ;;only for development
             ;[lein-cljsbuild "1.1.7"]
             [lein-swank "1.4.5"] ; from CSNePS
             ]
  :repl-options {
     
     :print clojure.core/println 
     ;:print clojure.pprint/pprint
     :timeout 120000
     
     
     }
  
  :repositories [
      ["local" {:url ~(str (.toURI (java.io.File. "local-m2"))) :checksum :warn }  ]
      ;["local-CSNePS" {:url ~(str (.toURI (java.io.File. "../backends-for-embedding/CSNePS/local_maven_repo"))) :checksum :warn} ]
      ;["FreeHEP" {:url "http://java.freehep.org/maven2" :checksum :warn } ] ;from CSNePS
      ;["jpedal" {:url  "http://maven.geomajas.org" :checksum :warn } ] ; from CSNePS
      
        ]
  
  
  
  ;;;from CSNePS;;;;;;;;;
  
  ;:source-paths ["src/clj/"]
  ;:source-path "src/clj/"
  ;:java-source-paths ["src/jvm/"] ;leiningen 2 compat.
  ;:java-source-path "src/jvm/" ;leiningen 1.x compat.
  
  
  ;:jvm-opts ["-server"]
    
  ;;;;;;;;;;;;;;;;;;;;
  
  
  
  
  ;;;
  
  ;https://stackoverflow.com/questions/17035529/use-leiningen-with-local-m2-repository
  ;
  ;https://www.spacjer.com/blog/2015/03/23/leiningen-working-with-local-repository/
  ;https://github.com/technomancy/leiningen/blob/stable/sample.project.clj#L248
  ;:local-repo "local-m2"
  
  
  
  )


