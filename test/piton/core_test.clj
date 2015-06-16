(ns piton.core-test
  (:require [clojure.java.io :as io]
            [clojure.test :refer :all]
            [piton.core :refer :all]
            [clojure.java.jdbc :as db]))

(db/get-connection "jdbc:h2:mem:test;DB_CLOSE_DELAY=-1")

(def db-spec "jdbc:h2:mem:test")

(deftest piton-testing
  (testing "Initial state"
    (is (empty? (applied-pitons db-spec true)))
    (is (empty? (applied-pitons db-spec false)))

    (is (= (sort (set (:migrations piton-list))) (to-be-applied db-spec false)))
    (is (= (sort (set (:seeds piton-list))) (to-be-applied db-spec true))))

  (testing "Migrating"
    (do (do-migration db-spec "1-mig-test")
        (is (not (empty? (applied-pitons db-spec false)))))

    (do (do-migration db-spec)
        (is (= 3 (count (applied-pitons db-spec false))))))

  (testing "Seeding"
    (do (do-seed db-spec "1-seed-test")
        (is (not (empty? (applied-pitons db-spec true)))))

    (do (do-seed db-spec)
        (is (= 3 (count (applied-pitons db-spec true))))))

  (testing "Selective rolback"
    (do (do-rollback db-spec true "3-seed-test")
        (is (= 2 (count (applied-pitons db-spec true)))))

    (do (do-rollback db-spec false "3-mig-test")
        (is (= 2 (count (applied-pitons db-spec false))))))

  (testing "General rollback"
    (do (do-migration db-spec)
        (do-seed db-spec)
        (do-rollback db-spec true)
        (is (empty? (applied-pitons db-spec true))))

    (do (do-rollback db-spec false)
        (is (empty? (applied-pitons db-spec false))))))
