(ns piton.core
  (:require [clojure.java.jdbc :as db]
            [clojure.java.io :as io]
            [clojure.edn]
            [clojure.set]
            [clojure.string]))

(def piton-list
  "Get the list of things to migrate and seed from
  file generated by the lein plugin. This data
  is formatted as such:

  {:migrations [file-name1 file-name2]
   :seeds [file-name1 file-name2]}
   :mig-path classpath
   :seed-path classpath"
  (when-let [piton-edn (io/resource "piton.edn")]
    (clojure.edn/read-string (slurp piton-edn))))

(def batch-id
  (quot (System/currentTimeMillis) 1000))

(defn- piton-table-exists?
  [db-spec]
  (some
   #(= "piton_migrations"
       (.toLowerCase (:table_name %)))
   (db/with-db-metadata [md db-spec]
     (db/metadata-result
      (.getTables md nil nil nil
                  (into-array ["TABLE" "VIEW"]))))))

(defn- piton-table
  "Creates the piton table if it doesn't exist"
  [db-spec]
  (when-not (piton-table-exists? db-spec)
    (db/execute! db-spec
      [(db/create-table-ddl
        :piton_migrations
        [:batch "int"]
        [:file_name "varchar(200)"]
        [:is_seed "bool"])])))

(defn- piton-table-write
  [db-spec batch-id file-name is-seed]
  (db/insert! db-spec :piton_migrations
    {:batch batch-id
     :file_name file-name
     :is_seed is-seed}))

(defn- piton-table-delete
  [db-spec batch-id]
  (db/delete! db-spec :piton_migrations
    ["batch = ?" batch-id]))

(defn applied-pitons
  "Get a list of migrations/seeds already applied.
  Since we are checking for applied pitons every time
  we do anything in this program, we will also be
  using this as the 'entry point' and checking if the
  database exists or not."
  [db-spec is-seed]
  (piton-table db-spec)
  (db/query db-spec
    ["select file_name from piton_migrations where is_seed = ?" is-seed]))

(defn to-be-applied
  "Diff the applied from the list of yet-to-be applied"
  [db-spec seed?]
  (let [applied (applied-pitons db-spec seed?)
        mig-files (if seed?
                    (:seeds piton-list)
                    (:migrations piton-list))]
    (sort
     (clojure.set/difference
      (set mig-files)
      (set (map #(get-in % [:file_name]) applied))))))

(defn- apply-sql-to-db
  "Takes sql str and executes"
  [db-spec sql]
  (db/execute! db-spec [sql]))

(defn slurp-mig-file
  [file seeding?]
  (slurp
   (io/resource
    (str
     ((if seeding? :seed-path :mig-path) piton-list)
     "/" file))))

(defn- extract-sql-and-apply
  "Takes a file, slurps, and runs a simple
  lexer to pick out the relevant sql"
  [file db-spec seeding? rollback?]
  (apply-sql-to-db
   db-spec
   ((if rollback? second first)
    (clojure.string/split
     (slurp-mig-file file seeding?)
     #"-- rollback")))
  (if rollback?
    (do
      (db/delete! db-spec :piton_migrations
       ["file_name = ?" file])
      (println "Rolled back piton\t" file))
    (do
      (db/insert! db-spec :piton_migrations
       {:batch batch-id :file_name file :is_seed seeding?})
      (println "Applied piton\t" file))))

;; Punchline
(defn do-rollback
  "Perform rollback; generic (by batch)
  or name-based"
  ([db-spec seed?]
   (doseq [file (db/query
                 db-spec
                 ["select file_name
                   from piton_migrations
                   where is_seed = ?
                   and batch = (select MAX(batch)
                               from piton_migrations)
                   order by file_name desc"
                   seed?])]
     (extract-sql-and-apply
      (:file_name file)
      db-spec seed? true)))

  ([db-spec seed? mig-name]
   (doseq [file (db/query
                 db-spec
                 ["select file_name
                  from piton_migrations
                  where file_name = ?
                  and is_seed = ?"
                  (str mig-name ".sql")
                  seed?])]
     (extract-sql-and-apply
      (:file_name file)
      db-spec seed? true))))

(defn do-migration
  "Migrate all the things or just some of the things"
  ([db-spec]
   (doseq [file (to-be-applied db-spec false)]
     (extract-sql-and-apply file db-spec false false)))

  ([db-spec mig-name]
   (doseq [file (to-be-applied db-spec false)
           :when (= (str mig-name ".sql") file)]
     (extract-sql-and-apply file db-spec false false))))

(defn do-seed
  "Plant things in the database"
  ([db-spec]
   (doseq [file (to-be-applied db-spec true)]
     (extract-sql-and-apply file db-spec true false)))

  ([db-spec seed-name]
   (doseq [file (to-be-applied db-spec true)
           :when (= (str seed-name ".sql") file)]
     (extract-sql-and-apply file db-spec true false))))


;; Non-mandatory entry point for most cases, including plugin
;; Note that db-spec can just be a jdbc connection map instead
;; of the string that's built from a generate dburl and user
;; and password
(defn migrate
  [dburl dbuser dbpass & migs]
  (let [db-spec (str dburl "?user=" dbuser "&password=" dbpass)]
    (if (empty? migs)
      (do-migration db-spec)
      (doall (map (partial do-migration db-spec) migs)))))

(defn seed
  [dburl dbuser dbpass & seeds]
  (let [db-spec (str dburl "?user=" dbuser "&password=" dbpass)]
    (if (empty? seeds)
      (do-seed db-spec)
      (doall (map (partial do-seed db-spec) seeds)))))

(defn rollback
  [dburl dbuser dbpass kind & specific]
  (let [db-spec (str dburl "?user=" dbuser "&password=" dbpass)
        is-seed (= "seeds" kind)
        specified (empty? specific)]
    (if specified
      (do-rollback db-spec is-seed)
      (doall (map (partial do-rollback db-spec is-seed) specific)))))
