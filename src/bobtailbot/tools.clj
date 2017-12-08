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
