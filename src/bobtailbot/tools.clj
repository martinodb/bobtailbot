(ns bobtailbot.tools
(:require 
 
 [taoensso.timbre :as timbre   :refer [log  trace  debug  info  warn  error  fatal  report logf tracef debugf infof warnf errorf fatalf reportf  spy get-env]]
 ;[taoensso.timbre.appenders.core :as appenders]
 
 [clojure.edn :as edn]
 [clojure.string :as string :refer [trim]]
 [clojure.java.io :as io]
 [me.raynes.fs :as fs]
 [clojure.pprint :as pp :refer [pprint]]
 ))



(defn tim-ret "timbre/info text and then return it"
  [text]
  (do (timbre/info text) text))

(defn tim-println "timbre/info text items and then print them"
  [& texts]
  (do (timbre/info texts) (apply println texts)))



;; CREDIT:
;; https://spootnik.org/entries/2016/12/17/building-an-atomic-database-with-clojure/



;; IMPORTANT!!!
;; Change this prefix if you change this file's name (or path).
;;Also remember to change the ns declaration.
(def parent-ns "bobtailbot")
(def this-ns-unqual "tools")
;;;;;


(def this-ns (str parent-ns "." this-ns-unqual))
(def ns-prefix (str this-ns "/"))
(def this-dir (str "./src/" (-> parent-ns (string/replace #"\." "/" ) (string/replace #"-" "_")) ))
(def dir-prefix (str this-dir "/" ))



;(let [file-name "path/to/whatever.txt"]
  ;(make-parents file-name)
  ;(spit file-name "whatever"))



;;; by Justin Smith.
;;; https://gist.github.com/noisesmith/3490f2d3ed98e294e033b002bc2de178
(defmacro locals-map [] (into {} (for [[sym val] &env] [(keyword (name sym)) sym])))
;;; Another option. This one doesn't capture the local environment, it requires explicit arguments:
;; https://github.com/clj-commons/useful/blob/master/src/flatland/useful/map.clj#L6
(let [transforms {:keys keyword
                  :strs str
                  :syms identity}]
  (defmacro keyed
      "Create a map in which, for each symbol S in vars, (keyword S) is a
  key mapping to the value of S in the current scope. If passed an optional
  :strs or :syms first argument, use strings or symbols as the keys instead."
    ([vars] `(keyed :keys ~vars))
    ([key-type vars]
       (let [transform (comp (partial list `quote)
                             (transforms key-type))]
         (into {} (map (juxt transform identity) vars))))))







(defn dump-to-path-records
  "Store a value's representation to a given path. Use this for records. It looks ugly but it works."
  [path value]
  (do (io/make-parents path)
      (spit path (pr-str value))) )




(defn dump-to-path-no-records
  "Store a value's representation to a given path. Don't use this for records, only for maps. It looks good but it can't handle records."
  [path value]
  (do (io/make-parents path)
  (pprint value (io/writer path)) )  )
  


(def dump-to-path dump-to-path-no-records)



(defn load-from-path
  "Load a value from its representation stored in a given path.
   When reading fails, yield the empty string"
  [path edn-readers]
  (try
     (let [string (edn/read-string 
                    {:readers edn-readers}
                    (slurp path))]
          (if string string ""))
    (catch Exception e (timbre/info (.getMessage e)))))

(defn load-from-path-or-create
  ([path edn-readers] 
     (load-from-path-or-create path "" edn-readers) )
  ([path init edn-readers]
   (try
     (if (.exists (io/as-file path)) (load-from-path path edn-readers) (do (dump-to-path path init) (load-from-path-or-create path init edn-readers)))
     (catch Exception e (timbre/info (.getMessage e)))) ) )

(defn persist-fn
  "Yields an atom watch-fn that dumps new states to a path"
  [path]
  (fn [_ _ _ state]
    (dump-to-path path state)))

(defn disk-atom
   "An atom that loads its initial state from a file and persists each new state
    to the same path. If an initial value is given, AND the file is empty, the initial value is stored in the file"
   ([path edn-readers]
   (let [init  (load-from-path-or-create path edn-readers)]
          (disk-atom path init)))
     
   ([path init edn-readers]
   (let [state (atom init)
          filecont (load-from-path-or-create path edn-readers)]
     (if (empty? filecont) (dump-to-path path init) (reset! state filecont))
     (add-watch state :persist-watcher (persist-fn path))
     state)))

(defn disk-ref
   "A ref that loads its initial state from a file and persists each new state
    to the same path. If an initial value is given, AND the file is empty, the initial value is stored in the file"
   ([path edn-readers]
   (let [init  (load-from-path-or-create path edn-readers)]
          (disk-ref path init edn-readers)))
     
   ([path init edn-readers]
   (let [state (ref init)
         filecont (load-from-path-or-create path edn-readers)]
     (if (empty? filecont) (dump-to-path path init) (dosync (ref-set state filecont)))
     (add-watch state :persist-watcher (persist-fn path))
     state)))
;; <<<<<<< HEAD




;;;;;;;;;;
;; EMBEDDING. TO RUN THROUGH A LEIN PROJECT ALIAS (A PLUGIN IS NOT NEEDED):
;; EXAMPLE:
;; " lein embed "path/to/csneps/project/..../csneps" "./src/bobtailbot/brains/csneps/embed"  "


;https://rosettacode.org/wiki/Walk_a_directory/Recursively#Clojure
;;(use '[clojure.java.io])
;; DOING: [clojure.java.io :as io] ; file -> io/file


(defn walk [dirpath pattern]
  (do (filter #(re-matches pattern (.getName %))
                 (file-seq (io/file dirpath)))))



;; this-dir is "/home/martin/Documentos/programming/chatterbots/FOSS-clojure/bots/Bobtailbot/bobtailbot/src/bobtailbot"
;(def default-csneps-orig-repo "/home/martin/Documentos/programming/chatterbots/FOSS-clojure/bots/Bobtailbot/backends-for-embedding/CSNePS")
; (def default csneps-destination "/home/martin/Documentos/programming/chatterbots/FOSS-clojure/bots/Bobtailbot/bobtailbot/src/bobtailbot/brains/csneps/emb")

;; default original repo, unqualified name.
(def default-ruqn "CSNePS")
;; The corresponding string for default-ruqn in bobtailbot.brains
(def drbn "csneps")



(def default-orig (str this-dir "/../../../" "backends-for-embedding/" default-ruqn))
(def default-dest (str this-dir "/brains/" drbn "/emb"))

;; default regex replacement string to find namespace names to modify. Notice double slash escape because it's for re-pattern.
(def dfregs  (str drbn "\\.")  ) ; for "csneps" -> "csneps\\."


; /CSNePS/src/clj/

;; Namespace to prepend:
(def dppend-ns (str "bobtailbot.brains." drbn ".emb" "." default-ruqn ".src.clj" ".") )

;; prepend dppend-ns to every occurrence of the old ns in a string:
(defn embns [st] (string/replace st (re-pattern dfregs) (str dppend-ns dfregs) ) )

;bobtailbot.core=> (ns bobtailbot.tools )
;nil
;bobtailbot.tools=> (embns "csneps.core.snuser")
;"bobtailbot.brains.csneps.emb.CSNePS.src.clj.csneps.core.snuser"
;bobtailbot.tools=> 



;; accepts a file and does embnsfn to its content.
(defn embnsffile  [f] (-> f slurp embns (spit (.getPath f))))

;; does embnsffile to every file in a given dir
(defn rec-embnsffile
  ""
  ([] (rec-embnsffile default-dest ))
  ([dir](map embnsffile (walk dir (re-pattern ".*\\.clj") ))) 

  )




 





;(declare println-clj-filenames)
;(declare txt-fn)
;(declare rec-proj-txt-fn)



(defn println-clj-filenames
  "print recursively the names of .clj files in a given dir"
  ([] (println-clj-filenames "src"))
  ([dir](map #(timbre/info (.getPath %)) (walk dir #".*\.clj"))) 

  )

(defn txt-fn 
"rename given file adding .BKP.txt to its name"
[file]
(let [filename (.getPath file)]
  (fs/rename filename (str filename ".BKP.txt"))))


(defn rec-proj-txt-fn
  "rename  .project  files in dir adding  .BKP.txt"
  ([] (rec-proj-txt-fn  default-dest))
  ([dir](map txt-fn (walk dir #"\.project"))) )












(defn embed-backend
"Creates a copy of a whole external repo of some FOSS third-party backend inside this repo, renaming namespaces accordingly. Used for CSNePS"
 ([] (do (timbre/info "TODO: the embed tool is not working yet!")
       (embed-backend default-orig default-dest)))
 ([orig dest] (do
                  (timbre/info "TODO: the embed tool is not working yet!!")
                  (println-clj-filenames orig)
                  
                  (timbre/info "copying dirs..")
                   ;;(fs/copy-dir-into orig (str dest "/CSNePS")) ; copies the *contents* of orig into dest.
                    (fs/copy-dir orig dest) ; copies orig into dest.
                  ;;In both cases, if orig is aleady there, I think it overwrites it.
                  ;;
                  ;;rename project files to .clj.txt
                   (rec-proj-txt-fn )
                  ;; rename namespaces
                   (rec-embnsffile)
                  
                  
                    ))
 
 
)


;;;;;;;;;;;





;;; FUNCTIONS WITH REGEXES

;; (map #(timbre/info (.getPath %)) (walk "src" #".*\.clj"))



;=======
;>>>>>>> parent of cc6d8d6... add a stub for the embed tool, to run CSNePS embedded. This tool will copy-paste an intact CSNePS repo into the bobtailbot repo, changing namespaces accordingly, and maybe renaming project.clj to project.clj.txt or something like that. It should work not only for CSNePS but also for any other backend written in Clojure
