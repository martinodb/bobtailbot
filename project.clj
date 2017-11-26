(defproject martinodb/bobtailbot "0.1.0-SNAPSHOT"
  :description "Bobtailbot chat bot"
  :url "https://github.com/martinodb/bobtailbot"
  :license {:name "Apache License Version 2.0"
            :url "https://www.apache.org/licenses/LICENSE-2.0"}
  :main bobtailbot.core
  :dependencies [[org.clojure/clojure "1.8.0"] ;[org.clojure/clojure "1.7.0"]
                 [prismatic/schema "1.1.7"] ;[prismatic/schema "1.1.6"] 
                 
                 [com.cerner/clara-rules "0.16.0"]
                 [instaparse "1.4.8"]
                 [com.outpace/config "0.10.0"]
                 
                 [org.clojure/tools.cli "0.3.5"]
                 [org.clojure/core.async "0.2.395"]
                 [com.gearswithingears/async-sockets "0.1.0"]
                 
                 ]
  :profiles {:dev { :source-paths ["dev" "src" "test"]
                    :dependencies [[org.clojure/tools.namespace "0.2.11"]
                                  [org.clojure/math.combinatorics "0.1.3"]
                                  [org.clojure/data.fressian "0.2.1"]]}
             :provided {:dependencies [[org.clojure/clojurescript "1.7.170"]]}}
  :aliases {"config" ["run" "-m" "outpace.config.generate"]}
  :plugins [[lein-codox "0.9.0" :exclusions [org.clojure/clojure]]
            [lein-javadoc "0.2.0" :exclusions [org.clojure/clojure]]
            [lein-cljsbuild "1.1.3" :exclusions [org.clojure/clojure]]
            [lein-figwheel "0.5.2" :exclusions [org.clojure/clojure]]]
  :codox {:namespaces [clara.rules clara.rules.dsl clara.rules.accumulators
                       clara.rules.listener clara.rules.durability
                       clara.tools.inspect clara.tools.tracing
                       clara.tools.fact-graph]
          :metadata {:doc/format :markdown}}
  :javadoc-opts {:package-names "clara.rules"}
  :source-paths ["src/main/clojure"]
  :resource-paths []
  :test-paths ["src/test/clojure" "src/test/common"]
  :java-source-paths ["src/main/java"]
  :javac-options ["-target" "1.6" "-source" "1.6"]
  :clean-targets ^{:protect false} ["resources/public/js" "target"]
  :hooks [leiningen.cljsbuild]
  :cljsbuild {:builds [;; Simple mode compilation for tests.
                       {:id "figwheel"
                        :source-paths ["src/test/clojurescript" "src/test/common"]
                        :figwheel true
                        :compiler {:main "clara.test"
                                   :output-to "resources/public/js/simple.js"
                                   :output-dir "resources/public/js/out"
                                   :asset-path "js/out"
                                   :optimizations :none}}

                       {:id "simple"
                        :source-paths ["src/test/clojurescript" "src/test/common"]
                        :compiler {:output-to "target/js/simple.js"
                                   :optimizations :whitespace}}

                       ;; Advanced mode compilation for tests.
                       {:id "advanced"
                        :source-paths ["src/test/clojurescript" "src/test/common"]
                        :compiler {:output-to "target/js/advanced.js"
                                   :optimizations :advanced}}]

              :test-commands {"phantom-simple" ["phantomjs"
                                                "src/test/js/runner.js"
                                                "src/test/html/simple.html"]

                              "phantom-advanced" ["phantomjs"
                                                  "src/test/js/runner.js"
                                                  "src/test/html/advanced.html"]}}

  :repl-options {;; The large number of ClojureScript tests is causing long compilation times
                 ;; to start the REPL.
                 :timeout 120000}
  
  ;; Factoring out the duplication of this test selector function causes an error,
  ;; perhaps because Leiningen is using this as uneval'ed code.
  ;; For now just duplicate the line.
  :test-selectors {:default (complement (fn [x]
                                          (some->> x :ns ns-name str (re-matches #"^clara\.generative.*"))))
                   :generative (fn [x] (some->> x :ns ns-name str (re-matches #"^clara\.generative.*")))}
  
  :scm {:name "git"
        :url "https://github.com/martinodb/bobtailbot"}
  :pom-addition [:developers [:developer
                              [:id "martinodb"]
                              [:name "Martinodb"]
                              [:url "https://github.com/martinodb"]]]
  ;:deploy-repositories [["snapshots" {:url ""  :creds :gpg}]]
                                      )
