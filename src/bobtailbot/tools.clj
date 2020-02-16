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






;=======
;>>>>>>> parent of cc6d8d6... add a stub for the embed tool, to run CSNePS embedded. This tool will copy-paste an intact CSNePS repo into the bobtailbot repo, changing namespaces accordingly, and maybe renaming project.clj to project.clj.txt or something like that. It should work not only for CSNePS but also for any other backend written in Clojure
