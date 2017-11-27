(ns bobtailbot.tools
(:require [clojure.edn :as edn]))

;; CREDIT:
;; https://spootnik.org/entries/2016/12/17/building-an-atomic-database-with-clojure/

(defn dump-to-path
  "Store a value's representation to a given path"
  [path value]
  (spit path (pr-str value)))

(defn load-from-path
  "Load a value from its representation stored in a given path.
   When reading fails, yield nil"
  [path]
  (try
    (edn/read-string (slurp path))
    (catch Exception _)))

(defn persist-fn
  "Yields an atom watch-fn that dumps new states to a path"
  [path]
  (fn [_ _ _ state]
    (dump-to-path path state)))

(defn disk-atom
   "An atom that loads its initial state from a file and persists each new state
    to the same path"
   ([path]
   (let [init  (load-from-path path)]
          (disk-atom path init)))
     
   ([path init]
   (let [state (atom init)]
     (dump-to-path path init)
     (add-watch state :persist-watcher (persist-fn path))
     state)))
