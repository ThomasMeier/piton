(ns piton.live
  (:require [piton.core :as p])
  (:gen-class))

(defn -main
  "Run from production environment command-line"
  [dburl dbuser dbpass command & args]
  (case command
    "migrate" (apply p/migrate dburl dbuser dbpass args)
    "seed" (apply p/seed dburl dbuser dbpass args)
    "rollback" (apply p/rollback dburl dbuser dbpass args)))
