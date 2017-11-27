(ns bobtailbot.tools
(:require [clojure.edn :as edn]
          [clojure.string :as string :refer [trim]]
          [clojure.java.io :as io]
))

;; CREDIT:
;; https://spootnik.org/entries/2016/12/17/building-an-atomic-database-with-clojure/

(defn dump-to-path
  "Store a value's representation to a given path"
  [path value]
  (spit path (pr-str value)))

(defn load-from-path
  "Load a value from its representation stored in a given path.
   When reading fails, yield the empty string"
  [path]
  (try
    (let [string (edn/read-string (slurp path))] (if string string ""))
    (catch Exception e (println (.getMessage e)))))

(defn load-from-path-or-create
  [path]
  (try
    (if (.exists (io/as-file path)) (load-from-path path) (do (io/file path) ""))
    (catch Exception e (println (.getMessage e)))))

(defn persist-fn
  "Yields an atom watch-fn that dumps new states to a path"
  [path]
  (fn [_ _ _ state]
    (dump-to-path path state)))

(defn disk-atom
   "An atom that loads its initial state from a file and persists each new state
    to the same path. If an initial value is given, AND the file is empty, the initial value is stored in the file"
   ([path]
   (let [init  (load-from-path-or-create path)]
          (disk-atom path init)))
     
   ([path init]
   (let [state (atom init)]
     (if (empty? (load-from-path-or-create path)) (dump-to-path path init))
     (add-watch state :persist-watcher (persist-fn path))
     state)))
