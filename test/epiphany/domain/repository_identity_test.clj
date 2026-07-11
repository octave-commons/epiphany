(ns epiphany.domain.repository-identity-test
  (:require [clojure.test :refer [deftest is testing]]
            [epiphany.domain.repository-identity :as repository-identity]))

(deftest new-resource-id-is-a-uuid
  (is (uuid? (repository-identity/new-resource-id))))

(deftest repository-metadata-has-only-a-resource-id
  (let [resource-id #uuid "7a6b0d26-1000-4000-8000-000000000001"]
    (is (= {:resource-id resource-id}
           (repository-identity/repository-metadata resource-id)))))

(deftest repository-metadata-rejects-non-uuids
  (testing "resource identity is a UUID, not a path or arbitrary string"
    (is (thrown? clojure.lang.ExceptionInfo
                 (repository-identity/repository-metadata "not-a-uuid")))))

(deftest resource-id-survives-edn-round-trip
  (let [resource-id #uuid "7a6b0d26-1000-4000-8000-000000000001"
        metadata (repository-identity/repository-metadata resource-id)]
    (is (= metadata
           (repository-identity/read-repository-metadata
            (repository-identity/write-repository-metadata metadata))))))

(deftest read-repository-metadata-rejects-extra-operational-state
  (is (thrown? clojure.lang.ExceptionInfo
               (repository-identity/read-repository-metadata
                "{:resource-id #uuid \"7a6b0d26-1000-4000-8000-000000000001\" :cursor \"abc\"}"))))

(deftest repository-metadata-path-is-under-common-git-directory
  (is (= "/repos/notes/.git/corpus-archaeology/repository.edn"
         (repository-identity/repository-metadata-path "/repos/notes/.git"))))

(deftest repository-metadata-path-does-not-treat-a-worktree-as-the-git-directory
  (is (= "/repos/notes/.git/worktrees/experiment/corpus-archaeology/repository.edn"
         (repository-identity/repository-metadata-path
          "/repos/notes/.git/worktrees/experiment"))))
