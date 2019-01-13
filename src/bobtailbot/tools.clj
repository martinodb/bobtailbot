(ns bobtailbot.tools
(:require [clojure.edn :as edn]
          [clojure.string :as string :refer [trim]]
          [clojure.java.io :as io]
          [me.raynes.fs :as fs]
))

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




(defn dump-to-path
  "Store a value's representation to a given path"
  [path value]
  (spit path (pr-str value)))

(defn load-from-path
  "Load a value from its representation stored in a given path.
   When reading fails, yield the empty string"
  [path edn-readers]
  (try
     (let [string (edn/read-string 
                    {:readers edn-readers}
                    (slurp path))]
          (if string string ""))
    (catch Exception e (println (.getMessage e)))))

(defn load-from-path-or-create
  [path edn-readers]
  (try
    (if (.exists (io/as-file path)) (load-from-path path edn-readers) (do (io/file path) ""))
    (catch Exception e (println (.getMessage e)))))

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




;;;;;;;;;;
;; EMBEDDING. TO RUN THROUGH A LEIN PROJECT ALIAS (A PLUGIN IS NOT NEEDED):
;; EXAMPLE:
;; " lein embed "path/to/csneps/project/..../csneps" "./src/bobtailbot/brains/csneps/embed"  "


;https://rosettacode.org/wiki/Walk_a_directory/Recursively#Clojure
;;(use '[clojure.java.io])
;; DOING: [clojure.java.io :as io] ; file -> io/file
 
(defn walk [dirpath pattern]
  (doall (filter #(re-matches pattern (.getName %))
                 (file-seq (io/file dirpath)))))

(declare println-clj-filenames)

;; this-dir is "/home/martin/Documentos/programming/chatterbots/FOSS-clojure/bots/Bobtailbot/bobtailbot/src/bobtailbot"
;(def default-csneps-orig-repo "/home/martin/Documentos/programming/chatterbots/FOSS-clojure/bots/Bobtailbot/backends-for-embedding/CSNePS")
; (def default csneps-destination "/home/martin/Documentos/programming/chatterbots/FOSS-clojure/bots/Bobtailbot/bobtailbot/src/bobtailbot/brains/csneps/emb")


(def default-orig (str this-dir "/../../../" "backends-for-embedding/CSNePS"))

(def default-dest (str this-dir "/brains/csneps/emb"))


(defn embed-backend
"Creates a copy of a whole external repo of some FOSS third-party backend inside this repo, renaming namespaces accordingly. Used for CSNePS"
 ([] (do (println "TODO: the embed tool is not working yet!")
       (embed-backend default-orig default-dest)))
 ([orig dest] (do (println "TODO: the embed tool is not working yet!!")
                  (println-clj-filenames orig)
                  
                  ))
 
 
)


;;;;;;;;;;;





;;; FUNCTIONS WITH REGEXES

;; (map #(println (.getPath %)) (walk "src" #".*\.clj"))

(defn println-clj-filenames
  "print recursively the names of .clj files in a given dir"
  ([] (println-clj-filenames "src"))
  ([dir](map #(println (.getPath %)) (walk dir #".*\.clj"))) 

  )
