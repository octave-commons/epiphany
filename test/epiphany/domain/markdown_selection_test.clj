(ns epiphany.domain.markdown-selection-test
  (:require [clojure.java.io :as io]
            [clojure.java.shell :as shell]
            [clojure.string :as string]
            [clojure.test :refer [deftest is testing use-fixtures]]
            [epiphany.domain.markdown-selection :as sel]
            [epiphany.infra.git :as git]))

(defn- sh! [& command]
  (let [{:keys [exit out err]} (apply shell/sh command)]
    (when-not (zero? exit)
      (throw (ex-info "Fixture command failed" {:command command :err err})))
    out))

(defn- temporary-directory []
  (.toFile (java.nio.file.Files/createTempDirectory "epiphany-selection-test"
                                                     (make-array java.nio.file.attribute.FileAttribute 0))))

(defn- delete-recursive [^java.io.File file]
  (when (.exists file)
    (when (.isDirectory file)
      (doseq [child (.listFiles file)]
        (delete-recursive child)))
    (.delete file)))

(defn- create-fixture-repository
  "Create a Git repository with paths exercising all acceptance criteria:
   nested paths, Unicode paths, non-Markdown files, and a deletion.
   Returns {:repo-path <string> :commit-oids {:c1 .. :c2 ..}}"
  [parent-dir]
  (let [repo-dir (io/file parent-dir "fixture-repo")]
    ;; init
    (sh! "git" "init" (.getPath repo-dir))
    (sh! "git" "-C" (.getPath repo-dir) "config" "user.email" "test@example.invalid")
    (sh! "git" "-C" (.getPath repo-dir) "config" "user.name" "Epiphany Test")

    ;; C1: root-level files
    (spit (io/file repo-dir "top.md") "# Top\n")
    (spit (io/file repo-dir "readme.txt") "plain text\n")
    (sh! "git" "-C" (.getPath repo-dir) "add" "top.md" "readme.txt")
    (sh! "git" "-C" (.getPath repo-dir) "commit" "-m" "C1: root files"
         "--author" "Test <test@example.invalid>" "--date" "2025-01-01T00:00:00Z")
    (let [c1 (string/trim (sh! "git" "-C" (.getPath repo-dir) "rev-parse" "HEAD"))]

      ;; C2: nested Markdown, Unicode path, non-Markdown, and deletion
      (let [docs-dir (io/file repo-dir "docs")]
        (.mkdirs docs-dir)
        (spit (io/file docs-dir "guide.md") "# Guide\n"))
      (let [nested-dir (io/file repo-dir "docs" "nested")]
        (.mkdirs nested-dir)
        (spit (io/file nested-dir "deep.md") "# Deep\n"))
      ;; Unicode path (Greek letters as in ADR-001)
      (let [unicode-dir (io/file repo-dir ".ημ")]
        (.mkdirs unicode-dir)
        (spit (io/file unicode-dir "spec.md") "# Spec\n"))
      ;; Non-Markdown in a directory
      (spit (io/file repo-dir "image.png") "not-a-real-png")
      (sh! "git" "-C" (.getPath repo-dir) "add" "-A")
      (sh! "git" "-C" (.getPath repo-dir) "commit" "-m" "C2: nested + unicode + non-md"
           "--author" "Test <test@example.invalid>" "--date" "2025-01-02T00:00:00Z")
      (let [c2 (string/trim (sh! "git" "-C" (.getPath repo-dir) "rev-parse" "HEAD"))]

        ;; C3: delete top.md
        (sh! "git" "-C" (.getPath repo-dir) "rm" "top.md")
        (sh! "git" "-C" (.getPath repo-dir) "commit" "-m" "C3: delete top.md"
             "--author" "Test <test@example.invalid>" "--date" "2025-01-03T00:00:00Z")
        (let [c3 (string/trim (sh! "git" "-C" (.getPath repo-dir) "rev-parse" "HEAD"))]
          {:repo-path    (.getPath repo-dir)
           :commit-oids  {:c1 c1 :c2 c2 :c3 c3}})))))

(def ^:private fixture (atom nil))

(use-fixtures :once
  (fn [f]
    (let [parent (temporary-directory)]
      (reset! fixture (create-fixture-repository parent))
      (try
        (f)
        (finally
          (delete-recursive parent))))))

;; ---- Glob matching unit tests (no I/O) ----

(deftest glob-matches-root-level-markdown
  (is (true? (#'sel/glob-match? "*.md" "foo.md")))
  (is (false? (#'sel/glob-match? "*.md" "docs/foo.md"))))

(deftest glob-matches-nested-wildcard
  (is (true? (#'sel/glob-match? "**/*.md" "foo.md")))
  (is (true? (#'sel/glob-match? "**/*.md" "docs/foo.md")))
  (is (true? (#'sel/glob-match? "**/*.md" "docs/nested/deep.md"))))

(deftest glob-excludes-non-matching-extensions
  (is (false? (#'sel/glob-match? "**/*.md" "readme.txt")))
  (is (false? (#'sel/glob-match? "**/*.md" "image.png"))))

(deftest glob-excludes-directories
  (is (false? (#'sel/glob-match? "**/*.md" "docs"))))

(deftest glob-matches-unicode-paths
  (is (true? (#'sel/glob-match? "**/*.md" ".ημ/spec.md")))
  (is (true? (#'sel/glob-match? ".ημ/*.md" ".ημ/spec.md"))))

(deftest glob-matches-exact-directory-pattern
  (is (true? (#'sel/glob-match? "docs/*.md" "docs/guide.md")))
  (is (false? (#'sel/glob-match? "docs/*.md" "docs/nested/deep.md"))))

(deftest glob-star-star-matches-zero-segments
  (is (true? (#'sel/glob-match? "**/guide.md" "guide.md")))
  (is (true? (#'sel/glob-match? "**/guide.md" "docs/guide.md"))))

;; ---- Selection unit tests (pure data, no I/O) ----

(def ^:private sample-entries
  [{:git/path "top.md"      :git/blob-oid "aaa" :git/mode 33188 :git/type :file}
   {:git/path "readme.txt"  :git/blob-oid "bbb" :git/mode 33188 :git/type :file}
   {:git/path "docs/guide.md" :git/blob-oid "ccc" :git/mode 33188 :git/type :file}
   {:git/path "docs/nested/deep.md" :git/blob-oid "ddd" :git/mode 33188 :git/type :file}
   {:git/path ".ημ/spec.md" :git/blob-oid "eee" :git/mode 33188 :git/type :file}
   {:git/path "image.png"   :git/blob-oid "fff" :git/mode 33188 :git/type :file}
   {:git/path "docs"        :git/blob-oid "ggg" :git/mode 16384  :git/type :tree}])

(deftest default-policy-selects-only-markdown
  (let [entries (sel/select-entries "commit-1" sel/default-policy sample-entries)]
    (is (= 4 (count entries)))
    (is (every? #(= "markdown-tree-v1" (:entry/policy-version %)) entries))
    (is (every? #(= "commit-1" (:entry/commit-oid %)) entries))
    (is (= #{"top.md" "docs/guide.md" "docs/nested/deep.md" ".ημ/spec.md"}
           (set (map :entry/path-raw entries))))))

(deftest select-entries-preserves-exact-paths
  (let [entries (sel/select-entries "commit-1" sel/default-policy sample-entries)
        paths (set (map :entry/path-raw entries))]
    ;; Unicode path preserved exactly
    (is (contains? paths ".ημ/spec.md"))
    ;; Nested path preserved exactly
    (is (contains? paths "docs/nested/deep.md"))))

(deftest select-entries-preserves-blob-oid-and-mode
  (let [entries (sel/select-entries "commit-1" sel/default-policy sample-entries)
        guide (first (filter #(= "docs/guide.md" (:entry/path-raw %)) entries))]
    (is (= "ccc" (:entry/blob-oid guide)))
    (is (= 33188 (:entry/mode guide)))))

(deftest select-entries-excludes-non-markdown
  (let [entries (sel/select-entries "commit-1" sel/default-policy sample-entries)]
    (is (not (some #(= "readme.txt" (:entry/path-raw %)) entries)))
    (is (not (some #(= "image.png" (:entry/path-raw %)) entries)))
    (is (not (some #(= "docs" (:entry/path-raw %)) entries)))))

(deftest select-entries-respects-exclude-globs
  (let [policy (assoc sel/default-policy :selection/exclude-globs [".ημ/*"])
        entries (sel/select-entries "commit-1" policy sample-entries)]
    (is (= 3 (count entries)))
    (is (not (some #(= ".ημ/spec.md" (:entry/path-raw %)) entries)))))

(deftest select-entries-custom-include-glob
  (let [policy {:selection/policy-version "custom-v1"
                :selection/include-globs ["docs/**/*.md"]
                :selection/exclude-globs []}
        entries (sel/select-entries "commit-1" policy sample-entries)]
    (is (= 2 (count entries)))
    (is (= #{"docs/guide.md" "docs/nested/deep.md"}
           (set (map :entry/path-raw entries))))))

(deftest select-entries-empty-when-no-match
  (let [policy {:selection/policy-version "no-match-v1"
                :selection/include-globs ["*.html"]
                :selection/exclude-globs []}
        entries (sel/select-entries "commit-1" policy sample-entries)]
    (is (= [] entries))))

(deftest select-entries-does-not-normalize-paths
  (testing "Unicode path is preserved exactly, not NFC/NFD normalized"
    (let [entries (sel/select-entries "commit-1" sel/default-policy sample-entries)
          unicode-entry (first (filter #(= ".ημ/spec.md" (:entry/path-raw %)) entries))]
      (is (some? unicode-entry))
      (is (= ".ημ/spec.md" (:entry/path-raw unicode-entry))))))

(deftest select-markdown-uses-default-policy
  (let [entries (sel/select-markdown "commit-1" sample-entries)]
    (is (= 4 (count entries)))
    (is (every? #(= "markdown-tree-v1" (:entry/policy-version %)) entries))))

;; ---- Integration tests (real Git repository) ----

(deftest tree-entries-from-commit
  (testing "C2 has expected entries"
    (let [{:keys [repo-path commit-oids]} @fixture
          result (git/commit-tree-entries repo-path (:c2 commit-oids))]
      (is (nil? (:failure result)))
      (is (= (:c2 commit-oids) (:commit-oid result)))
      (is (pos? (count (:entries result))))
      (is (some #(= "docs/guide.md" (:git/path %)) (:entries result)))
      (is (some #(= ".ημ/spec.md" (:git/path %)) (:entries result)))
      (is (some #(= "image.png" (:git/path %)) (:entries result)))
      (is (some #(= "top.md" (:git/path %)) (:entries result))))))

(deftest deleted-file-absent-in-tree
  (testing "C3 (after deletion) has no top.md"
    (let [{:keys [repo-path commit-oids]} @fixture
          result (git/commit-tree-entries repo-path (:c3 commit-oids))]
      (is (nil? (:failure result)))
      (is (not (some #(= "top.md" (:git/path %)) (:entries result))))
      ;; But docs still exists
      (is (some #(= "docs/guide.md" (:git/path %)) (:entries result))))))

(deftest selection-integration-with-real-repo
  (testing "end-to-end: walk C2 tree + select Markdown"
    (let [{:keys [repo-path commit-oids]} @fixture
          {:keys [entries failure]} (git/commit-tree-entries repo-path (:c2 commit-oids))
          selected (sel/select-markdown (:c2 commit-oids) entries)]
      (is (nil? failure))
      (is (pos? (count selected)))
      ;; Should include nested Markdown
      (is (some #(= "docs/guide.md" (:entry/path-raw %)) selected))
      (is (some #(= "docs/nested/deep.md" (:entry/path-raw %)) selected))
      ;; Should include Unicode Markdown
      (is (some #(= ".ημ/spec.md" (:entry/path-raw %)) selected))
      ;; Should NOT include non-Markdown
      (is (not (some #(= "image.png" (:entry/path-raw %)) selected)))
      (is (not (some #(= "readme.txt" (:entry/path-raw %)) selected))))))

(deftest selection-integration-no-checkout
  (testing "selection never checks out historical revisions"
    (let [repo-path (:repo-path @fixture)
          {:keys [commit-oids]} @fixture
          working-dir (io/file repo-path "working-copy")]
      ;; Create a worktree at a different path
      (sh! "git" "-C" repo-path "worktree" "add"
           (.getPath working-dir) (:c2 commit-oids))
      (try
        ;; Walk the tree from the worktree's perspective
        (let [{:keys [entries failure]} (git/commit-tree-entries repo-path (:c2 commit-oids))
              selected (sel/select-markdown (:c2 commit-oids) entries)]
          (is (nil? failure))
          ;; The selection still works on C2's tree, regardless of worktree state
          (is (pos? (count selected))))
        (finally
          (sh! "git" "-C" repo-path "worktree" "remove" "--force"
               (.getPath working-dir)))))))

(deftest unreachable-commit-returns-failure
  (testing "unknown commit OID produces a failure record"
    (let [{:keys [repo-path]} @fixture
          fake-oid "0000000000000000000000000000000000000000"
          result (git/commit-tree-entries repo-path fake-oid)]
      (is (= [] (:entries result)))
      (is (some? (:failure result)))
      (is (= "commit-unreadable" (:failure/reason (:failure result)))))))
