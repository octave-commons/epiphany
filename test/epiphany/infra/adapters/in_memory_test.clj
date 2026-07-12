(ns epiphany.infra.adapters.in-memory-test
  (:require [clojure.test :refer [deftest is]]
            [epiphany.infra.adapters.in-memory :as in-memory]
            [epiphany.law.registry :as registry]))

(defn- fake-common-git-dir [path]
  (str path "/.git"))

(deftest in-memory-adapters-satisfy-application-ports-schema
  (let [adapters (in-memory/make {:common-git-dir-fn fake-common-git-dir})]
    (is (registry/valid? "application/ports" adapters)
        "In-memory adapters must satisfy the application ports schema")))

(deftest in-memory-git-resolves-common-directory
  (let [adapters (in-memory/make {:common-git-dir-fn fake-common-git-dir})]
    (is (= "/repos/notes/.git"
           ((:common-git-directory (:git adapters)) "/repos/notes")))))

(deftest in-memory-repository-metadata-round-trips
  (let [adapters (in-memory/make {:common-git-dir-fn fake-common-git-dir})
        repo-md  (:repository-metadata adapters)
        rid      #uuid "aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeeee"]
    (is (nil? ((:read repo-md) "/repo/.git")))
    ((:write repo-md) "/repo/.git" rid)
    (is (= {:resource-id rid}
           ((:read repo-md) "/repo/.git")))))

(deftest in-memory-observations-are-idempotent-by-request-id
  (let [adapters (in-memory/make {:common-git-dir-fn fake-common-git-dir})
        obs      (:observations adapters)
        rid      #uuid "11111111-2222-3333-4444-555555555555"
        record   {:observation/request-id rid
                  :resource-id #uuid "aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeeee"
                  :repository-path "/repo"
                  :common-git-dir "/repo/.git"}]
    (is (nil? ((:find-by-request-id obs) rid)))
    ((:record-repository-location! obs) record)
    (is (= record ((:find-by-request-id obs) rid)))))

(deftest in-memory-require-common-git-dir-fn
  (is (thrown? clojure.lang.ExceptionInfo
              (in-memory/make {}))))
