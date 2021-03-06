(defproject bobtailbot "0.1.0-SNAPSHOT"
  :description "A simple REPL chatbot written in Clojure"
  :url "https://github.com/martinodb/bobtailbot"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :main bobtailbot.core
  ;:aot [bobtailbot.core]
  
  :dependencies [
                 [org.clojure/clojure "1.10.1"]

                 [org.clojure/tools.cli "1.0.194"]

                 [com.outpace/config "0.13.2"]
                 
                 [com.cerner/clara-rules "0.20.0"]
                 
                 [instaparse "1.4.10"]
                 
                 [org.clojure/core.async "1.0.567"]
                 
                 [com.gearswithingears/async-sockets "0.1.0"]
                 
                 
                 ;; https://github.com/Raynes/fs
                 ;; Tools to manipulate files and dirs
                 [me.raynes/fs "1.4.6"]
                 
                 ;;
                 [org.clojars.martinodb/zinc "1.0.0-SNAPSHOT"] ; clojars version
                 
                 [com.taoensso/timbre "4.10.0"] ; logging.
                 
                 
                 ]
                 
  :profiles
  {:dev {:source-paths ["dev" "src" "test"]
         :dependencies [ ;[org.clojure/tools.namespace "0.2.10"]
                        [org.clojure/tools.namespace "1.0.0"]
                        ]
         :plugins [  [lein-ancient "0.6.15"]  ]    }
   :uberjar {:aot :all} 
   }


  :aliases {"config" ["run" "-m" "outpace.config.generate"]}
  :plugins [ 
             ]
  :repl-options {
     
     :print clojure.core/println 
     ;:print clojure.pprint/pprint
     :timeout 250000
     
     }
  
  ; :repositories [ ["local" {:url ~(str (.toURI (java.io.File. "local-m2"))) :checksum :warn }  ]   ]
  
  
  
  
  )


