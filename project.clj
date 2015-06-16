(defproject piton "0.1.0-SNAPSHOT"
  :description "SQL Migrations and seeding for Clojure projects"
  :url "https://github.com/thomasmeier"
  :dependencies [[org.clojure/clojure "1.6.0"]
                 [org.clojure/java.jdbc "0.3.7"]]
  :target-path "target/%s"
  :main piton.live
  :profiles {:dev {:dependencies [[com.h2database/h2 "1.4.187"]]
                   :resource-paths ["test/test_resources"]}
             :test {:dependencies [[com.h2database/h2 "1.4.187"]]
                    :resource-paths ["test/test_resources"]}
             :uberjar {:aot :all}})
