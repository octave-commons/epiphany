(ns epiphany.infra.git-commit-test
  (:require [clojure.java.io :as io]
            [clojure.java.shell :as shell]
            [clojure.test :refer [deftest is testing use-fixtures]]
            [epiphany.infra.git :as git]
            [epiphany.law.registry :as registry]))

(defn- sh! [& command]
  (let [{:keys [exit out err]} (apply shell/sh command)]
    (when-not (zero? exit)
      (throw (ex-info "Fixture command failed" {:command command :err err})))
    out))

(defn- temporary-directory []
  (.toFile (java.nio.file.Files/createTempDirectory "epiphany-commit-test" (make-array java.nio.file.attribute.FileAttribute 0))))

(defn- create-fixture-repository
  "Create a Git repository with a deterministic commit history.
   Returns the repository path as a string.

   History:
     C1 (initial) -> C2 (second) -> C3 (third, on main)
                                      //
                                     C4 (feature-branch, branched from C2)"
  [parent-dir]
  (let [repo-dir (io/file parent-dir "fixture-repo")]
    ;; init
    (sh! "git" "init" (.getPath repo-dir))
    (sh! "git" "-C" (.getPath repo-dir) "config" "user.email" "test@example.invalid")
    (sh! "git" "-C" (.getPath repo-dir) "config" "user.name" "Epiphany Test")

    ;; C1: initial commit
    (spit (io/file repo-dir "README.md") "fixture\n")
    (sh! "git" "-C" (.getPath repo-dir) "add" "README.md")
    (sh! "git" "-C" (.getPath repo-dir) "commit" "-m" "initial commit"
         "--author" "Alice <alice@example.invalid>" "--date" "2025-01-01T00:00:00Z")

    ;; C2: second commit
    (spit (io/file repo-dir "doc.md") "second\n")
    (sh! "git" "-C" (.getPath repo-dir) "add" "doc.md")
    (sh! "git" "-C" (.getPath repo-dir) "commit" "-m" "second commit"
         "--author" "Bob <bob@example.invalid>" "--date" "2025-01-02T00:00:00Z")

    ;; create feature-branch from C2
    (sh! "git" "-C" (.getPath repo-dir) "checkout" "-b" "feature-branch")

    ;; C4: feature commit
    (spit (io/file repo-dir "feature.md") "feature\n")
    (sh! "git" "-C" (.getPath repo-dir) "add" "feature.md")
    (sh! "git" "-C" (.getPath repo-dir) "commit" "-m" "feature commit"
         "--author" "Charlie <charlie@example.invalid>" "--date" "2025-01-04T00:00:00Z")

    ;; back to main
    (sh! "git" "-C" (.getPath repo-dir) "checkout" "main")

    ;; C3: third commit on main
    (spit (io/file repo-dir "main.md") "main content\n")
    (sh! "git" "-C" (.getPath repo-dir) "add" "main.md")
    (sh! "git" "-C" (.getPath repo-dir) "commit" "-m" "third commit"
         "--author" "Diana <diana@example.invalid>" "--date" "2025-01-03T00:00:00Z")

    (.getPath repo-dir)))

(defn- delete-recursive [^java.io.File file]
  (when (.exists file)
    (when (.isDirectory file)
      (doseq [child (.listFiles file)]
        (delete-recursive child)))
    (.delete file)))

(def ^:private fixture-repo-dir (atom nil))

(use-fixtures :once
  (fn [f]
    (let [parent (temporary-directory)]
      (reset! fixture-repo-dir (create-fixture-repository parent))
      (try
        (f)
        (finally
          (delete-recursive parent))))))

;; ---- Determinism tests ----

(deftest reachable-commits-are-deterministic
  (testing "same refs produce same commit set in same order"
    (let [result-1 (git/reachable-commits @fixture-repo-dir #{"refs/heads/main" "refs/heads/feature-branch"})
          result-2 (git/reachable-commits @fixture-repo-dir #{"refs/heads/main" "refs/heads/feature-branch"})]
      (is (= (:commit-count result-1) (:commit-count result-2)))
      (is (= (mapv :commit/oid (:commits result-1))
             (mapv :commit/oid (:commits result-2))))
      (is (= (:failure-count result-1) (:failure-count result-2)))))

  (testing "ref sort order is deterministic"
    (let [result (git/reachable-commits @fixture-repo-dir #{"refs/heads/feature-branch" "refs/heads/main"})]
      (is (= 4 (:commit-count result)))
      (is (zero? (:failure-count result))))))

(deftest reachable-commits-discovers-all-reachable-objects
  (testing "all 4 commits are discovered"
    (let [result (git/reachable-commits @fixture-repo-dir #{"refs/heads/main" "refs/heads/feature-branch"})]
      (is (= 4 (:commit-count result)))
      (is (zero? (:failure-count result)))))

  (testing "feature-branch alone discovers 3 commits (C4, C2, C1)"
    (let [result (git/reachable-commits @fixture-repo-dir #{"refs/heads/feature-branch"})]
      (is (= 3 (:commit-count result)))))

  (testing "main alone discovers 3 commits (C1, C2, C3)"
    (let [result (git/reachable-commits @fixture-repo-dir #{"refs/heads/main"})]
      (is (= 3 (:commit-count result))))))

;; ---- Observation structure tests ----

(deftest commit-observation-contains-all-required-fields
  (let [result (git/reachable-commits @fixture-repo-dir #{"refs/heads/main"})
        commit (first (:commits result))]
    (is (string? (:commit/oid commit)))
    (is (vector? (:commit/parent-oids commit)))
    (is (string? (:commit/tree-oid commit)))
    (is (map? (:commit/author commit)))
    (is (map? (:commit/committer commit)))
    (is (bytes? (:commit/message-bytes commit)))
    (is (string? (:commit/message-text commit)))
    (is (string? (:git/ref-context commit)))))

(deftest commit-observation-identity-fields-are-valid
  (let [result (git/reachable-commits @fixture-repo-dir #{"refs/heads/main"})]
    (doseq [commit (:commits result)]
      (is (registry/valid? "git/oid" (:commit/oid commit))
          (str "commit OID must be valid: " (:commit/oid commit)))
      (is (registry/valid? "git/oid" (:commit/tree-oid commit))
          (str "tree OID must be valid: " (:commit/tree-oid commit)))
      (doseq [parent-oid (:commit/parent-oids commit)]
        (is (registry/valid? "git/oid" parent-oid)
            (str "parent OID must be valid: " parent-oid)))
      (is (registry/valid? "git/ref-name" (:git/ref-context commit))
          (str "ref context must be valid: " (:git/ref-context commit))))))

(deftest commit-observation-person-fields-are-populated
  (let [result (git/reachable-commits @fixture-repo-dir #{"refs/heads/main"})
        commit (first (:commits result))
        author (:commit/author commit)
        committer (:commit/committer commit)]
    (is (string? (:person/name author)))
    (is (pos? (count (:person/name author))))
    (is (string? (:person/email author)))
    (is (pos? (count (:person/email author))))
    (is (inst? (:person/timestamp author)))
    (is (string? (:person/name committer)))
    (is (string? (:person/email committer)))
    (is (inst? (:person/timestamp committer)))))

(deftest commit-message-is-preserved-exactly
  (let [result (git/reachable-commits @fixture-repo-dir #{"refs/heads/main"})
        messages (map :commit/message-text (:commits result))]
    (is (some #(= % "initial commit") messages))
    (is (some #(= % "second commit") messages))
    (is (some #(= % "third commit") messages))))

(deftest first-commit-has-no-parents
  (let [result (git/reachable-commits @fixture-repo-dir #{"refs/heads/main"})
        ;; The initial commit should have no parents
        initial (first (filter #(= "initial commit" (:commit/message-text %))
                               (:commits result)))]
    (is (some? initial))
    (is (empty? (:commit/parent-oids initial)))))

(deftest non-first-commit-has-parent
  (let [result (git/reachable-commits @fixture-repo-dir #{"refs/heads/main"})
        second (first (filter #(= "second commit" (:commit/message-text %))
                              (:commits result)))]
    (is (some? second))
    (is (= 1 (count (:commit/parent-oids second))))))

;; ---- Failure handling tests ----

(deftest unresolvable-ref-is-recorded-as-failure
  (let [result (git/reachable-commits @fixture-repo-dir #{"refs/heads/nonexistent"})]
    (is (zero? (:commit-count result)))
    (is (= 1 (:failure-count result)))
    (is (= "refs/heads/nonexistent" (:failure/ref (first (:failures result)))))
    (is (= "ref-not-found" (:failure/reason (first (:failures result)))))))

(deftest mixed-valid-and-invalid-refs-partial-success
  (let [result (git/reachable-commits @fixture-repo-dir #{"refs/heads/main" "refs/heads/nonexistent"})]
    (is (pos? (:commit-count result)))
    (is (= 1 (:failure-count result)))
    (is (= "refs/heads/nonexistent" (:failure/ref (first (:failures result)))))))

;; ---- Idempotency / immutable identity tests ----

(deftest same-commit-oid-emitted-once-across-refs
  (testing "commit reachable from multiple refs is emitted once"
    (let [result (git/reachable-commits @fixture-repo-dir #{"refs/heads/main" "refs/heads/feature-branch"})
          oids (mapv :commit/oid (:commits result))]
      (is (= (count oids) (count (set oids)))
          "no duplicate OIDs in the output"))))

(deftest feature-branch-commit-has-correct-ref-context
  (let [result (git/reachable-commits @fixture-repo-dir #{"refs/heads/main" "refs/heads/feature-branch"})
        feature-commit (first (filter #(= "feature commit" (:commit/message-text %))
                                      (:commits result)))]
    (is (some? feature-commit))
    (is (= "refs/heads/feature-branch" (:git/ref-context feature-commit)))))

;; ---- read-blob tests (regression: Repository has no .getObject method;
;;      must use .open, which throws MissingObjectException when absent) ----

(defn- blob-oid-for [path]
  (let [main-commit (first (filter #(= "third commit" (:commit/message-text %))
                                   (:commits (git/reachable-commits @fixture-repo-dir #{"refs/heads/main"}))))
        entries (:entries (git/commit-tree-entries @fixture-repo-dir (:commit/oid main-commit)))]
    (:git/blob-oid (first (filter #(= path (:git/path %)) entries)))))

(deftest read-blob-returns-real-content
  (let [oid (blob-oid-for "README.md")
        result (git/read-blob @fixture-repo-dir oid)]
    (is (some? oid) "fixture blob-oid resolves")
    (is (nil? (:blob/failure result)))
    (is (= "fixture\n" (:blob/content result)))
    (is (= (:blob/size result) (count (.getBytes "fixture\n" "UTF-8"))))))

(deftest read-blob-missing-oid-reports-failure-not-exception
  (let [fake-oid "0000000000000000000000000000000000000000"
        result (git/read-blob @fixture-repo-dir fake-oid)]
    (is (nil? (:blob/content result)))
    (is (= "blob-not-found" (:failure/reason (:blob/failure result))))))
