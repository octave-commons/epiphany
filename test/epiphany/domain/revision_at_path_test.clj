(ns epiphany.domain.revision-at-path-test
  (:require [clojure.java.io :as io]
            [clojure.java.shell :as shell]
            [clojure.string :as string]
            [clojure.test :refer [deftest is testing use-fixtures]]
            [epiphany.domain.revision-at-path :as ratp]
            [epiphany.domain.markdown-selection :as sel]
            [epiphany.infra.git :as git]
            [epiphany.law.registry :as registry]))

(defn- sh! [& command]
  (let [{:keys [exit out err]} (apply shell/sh command)]
    (when-not (zero? exit)
      (throw (ex-info "Fixture command failed" {:command command :err err})))
    out))

(defn- temporary-directory []
  (.toFile (java.nio.file.Files/createTempDirectory "epiphany-ratp-test"
                                                     (make-array java.nio.file.attribute.FileAttribute 0))))

(defn- delete-recursive [^java.io.File file]
  (when (.exists file)
    (when (.isDirectory file)
      (doseq [child (.listFiles file)]
        (delete-recursive child)))
    (.delete file)))

(defn- create-fixture-repository
  "Create a Git repository with a deterministic commit history exercising
  all acceptance criteria: add, modify, delete, continuity, Unicode paths,
  root commit, multi-parent comparisons.

  History:
    C1 (root): top.md, readme.txt                    — initial for all paths
    C2: add docs/guide.md, modify top.md, keep readme.txt
    C3: delete top.md, add .ημ/spec.md, keep docs/guide.md unchanged
    C4: modify docs/guide.md"
  [parent-dir]
  (let [repo-dir (io/file parent-dir "fixture-repo")]
    ;; init
    (sh! "git" "init" (.getPath repo-dir))
    (sh! "git" "-C" (.getPath repo-dir) "config" "user.email" "test@example.invalid")
    (sh! "git" "-C" (.getPath repo-dir) "config" "user.name" "Epiphany Test")

    ;; C1: root commit
    (spit (io/file repo-dir "top.md") "# Top v1\n")
    (spit (io/file repo-dir "readme.txt") "plain text\n")
    (sh! "git" "-C" (.getPath repo-dir) "add" "top.md" "readme.txt")
    (sh! "git" "-C" (.getPath repo-dir) "commit" "-m" "C1: root"
         "--author" "Test <test@example.invalid>" "--date" "2025-01-01T00:00:00Z")
    (let [c1 (string/trim (sh! "git" "-C" (.getPath repo-dir) "rev-parse" "HEAD"))
          ;; C2: add docs/guide.md, modify top.md, keep readme.txt
          _  (doto (io/file repo-dir "docs") .mkdirs)
          _  (spit (io/file repo-dir "docs" "guide.md") "# Guide\n")
          _  (spit (io/file repo-dir "top.md") "# Top v2\n")
          _  (sh! "git" "-C" (.getPath repo-dir) "add" "-A")
          _  (sh! "git" "-C" (.getPath repo-dir) "commit" "-m" "C2: add + modify"
                  "--author" "Test <test@example.invalid>" "--date" "2025-01-02T00:00:00Z")
          c2 (string/trim (sh! "git" "-C" (.getPath repo-dir) "rev-parse" "HEAD"))
          ;; C3: delete top.md, add .ημ/spec.md
          _  (sh! "git" "-C" (.getPath repo-dir) "rm" "top.md")
          _  (doto (io/file repo-dir ".ημ") .mkdirs)
          _  (spit (io/file repo-dir ".ημ" "spec.md") "# Spec\n")
          _  (sh! "git" "-C" (.getPath repo-dir) "add" "-A")
          _  (sh! "git" "-C" (.getPath repo-dir) "commit" "-m" "C3: delete + add unicode"
                  "--author" "Test <test@example.invalid>" "--date" "2025-01-03T00:00:00Z")
          c3 (string/trim (sh! "git" "-C" (.getPath repo-dir) "rev-parse" "HEAD"))
          ;; C4: modify docs/guide.md
          _  (spit (io/file repo-dir "docs" "guide.md") "# Guide v2\n")
          _  (sh! "git" "-C" (.getPath repo-dir) "add" "-A")
          _  (sh! "git" "-C" (.getPath repo-dir) "commit" "-m" "C4: modify guide"
                  "--author" "Test <test@example.invalid>" "--date" "2025-01-04T00:00:00Z")
          c4 (string/trim (sh! "git" "-C" (.getPath repo-dir) "rev-parse" "HEAD"))]
      {:repo-path   (.getPath repo-dir)
       :commit-oids {:c1 c1 :c2 c2 :c3 c3 :c4 c4}})))

(def ^:private fixture (atom nil))

(use-fixtures :once
  (fn [f]
    (let [parent (temporary-directory)]
      (reset! fixture (create-fixture-repository parent))
      (try
        (f)
        (finally
          (delete-recursive parent))))))

(def ^:private test-uuid #uuid "7a6b0d26-1000-4000-8000-000000000001")

(defn- entries-at [commit-key]
  (let [{:keys [repo-path commit-oids]} @fixture
        oid (get commit-oids commit-key)]
    (:entries (git/commit-tree-entries repo-path oid))))

(defn- select-at [commit-key]
  (let [{:keys [commit-oids]} @fixture
        oid (get commit-oids commit-key)
        entries (entries-at commit-key)]
    (sel/select-markdown oid entries)))

(defn- tree-oid-for [commit-key]
  (let [{:keys [repo-path commit-oids]} @fixture
        oid (get commit-oids commit-key)]
    (:commit/tree-oid
     (first (filter #(= oid (:commit/oid %))
                     (:commits (git/reachable-commits repo-path
                                                       (case commit-key
                                                         :c1 #{"refs/heads/main"}
                                                         :c2 #{"refs/heads/main"}
                                                         :c3 #{"refs/heads/main"}
                                                         :c4 #{"refs/heads/main"}))))))))

(defn- parent-entries-at [commit-key]
  (let [parent-key (case commit-key
                     :c2 :c1
                     :c3 :c2
                     :c4 :c3
                     nil)]
    (when parent-key
      (entries-at parent-key))))

(defn- parent-commit-oid-for [commit-key]
  (let [parent-key (case commit-key
                     :c2 :c1
                     :c3 :c2
                     :c4 :c3
                     nil)]
    (when parent-key
      (get (:commit-oids @fixture) parent-key))))

;; ---- Evidence type unit tests (pure data, no I/O) ----

(deftest evidence-initial-for-root-commit
  (testing "a root commit entry produces :initial evidence"
    (let [entry {:git/path "top.md" :git/blob-oid "aaa" :git/mode 33188 :git/type :file}]
      (is (= :initial (ratp/evidence-type entry nil))))))

(deftest evidence-add-when-path-not-in-parent
  (testing "a path in child but not in parent is :add"
    (let [child  {:git/path "new.md" :git/blob-oid "aaa" :git/mode 33188 :git/type :file}
          parent [{:git/path "other.md" :git/blob-oid "bbb" :git/mode 33188 :git/type :file}]]
      (is (= :add (ratp/evidence-type child parent))))))

(deftest evidence-modify-when-blob-changes
  (testing "same path, different blob OID is :modify"
    (let [child  {:git/path "doc.md" :git/blob-oid "bbb" :git/mode 33188 :git/type :file}
          parent [{:git/path "doc.md" :git/blob-oid "aaa" :git/mode 33188 :git/type :file}]]
      (is (= :modify (ratp/evidence-type child parent))))))

(deftest evidence-continuity-when-blob-same
  (testing "same path, same blob OID is :continuity"
    (let [child  {:git/path "doc.md" :git/blob-oid "aaa" :git/mode 33188 :git/type :file}
          parent [{:git/path "doc.md" :git/blob-oid "aaa" :git/mode 33188 :git/type :file}]]
      (is (= :continuity (ratp/evidence-type child parent))))))

(deftest evidence-delete-when-path-in-parent-only
  (testing "path in parent but not in child returns :delete"
    (is (= :delete (ratp/evidence-for-deleted
                     {:git/path "old.md" :git/blob-oid "aaa"}
                     [])))))

;; ---- Observation construction tests ----

(deftest revision-at-path-contains-all-required-fields
  (let [entry {:entry/commit-oid     "abc123"
               :entry/path-raw       "docs/guide.md"
               :entry/blob-oid       "def456"
               :entry/mode           33188
               :entry/policy-version "markdown-tree-v1"}
        obs   (ratp/revision-at-path entry
                 {:resource-id      test-uuid
                  :tree-oid         "tree789"
                  :parent-commit-oid "parent123"
                  :parent-blob-oid   "blob456"
                  :observed-at       #inst "2025-01-01T00:00:00Z"
                  :parent-entries    [{:git/path "docs/guide.md" :git/blob-oid "old" :git/mode 33188}]})]
    (is (uuid? (:revision-at-path/id obs)))
    (is (= :revision/at-path-observed (:observation/type obs)))
    (is (= test-uuid (:resource-id obs)))
    (is (= "abc123" (:revision/commit-oid obs)))
    (is (= "tree789" (:revision/tree-oid obs)))
    (is (= "docs/guide.md" (:revision/path-raw obs)))
    (is (= "def456" (:revision/blob-oid obs)))
    (is (= 33188 (:revision/mode obs)))
    (is (= :modify (:revision/evidence obs)))
    (is (= "parent123" (:revision/parent-commit-oid obs)))
    (is (= "blob456" (:revision/parent-blob-oid obs)))
    (is (= 1 (:observation/schema-version obs)))
    (is (string? (:observation/adapter-version obs)))))

(deftest root-commit-observation-omits-parent-fields
  (let [entry {:entry/commit-oid     "abc123"
               :entry/path-raw       "top.md"
               :entry/blob-oid       "def456"
               :entry/mode           33188
               :entry/policy-version "markdown-tree-v1"}
        obs   (ratp/revision-at-path entry
                 {:resource-id       test-uuid
                  :tree-oid          "tree789"
                  :parent-commit-oid nil
                  :parent-blob-oid   nil
                  :observed-at       #inst "2025-01-01T00:00:00Z"
                  :parent-entries    nil})]
    (is (= :initial (:revision/evidence obs)))
    (is (not (contains? obs :revision/parent-commit-oid)))
    (is (not (contains? obs :revision/parent-blob-oid)))))

(deftest observation-satisfies-malli-schema
  (let [entry {:entry/commit-oid     "abc123def0123456789012345678901234567890"
               :entry/path-raw       "docs/guide.md"
               :entry/blob-oid       "456789abc0123456789012345678901234567890"
               :entry/mode           33188
               :entry/policy-version "markdown-tree-v1"}
        obs   (ratp/revision-at-path entry
                 {:resource-id       test-uuid
                  :tree-oid          "abcdef0123456789abcdef0123456789abcdef01"
                  :parent-commit-oid "1234567890abcdef1234567890abcdef12345678"
                  :parent-blob-oid   "abcdef0123456789abcdef0123456789abcdef01"
                  :observed-at       #inst "2025-01-01T00:00:00Z"
                  :parent-entries    [{:git/path "docs/guide.md"
                                       :git/blob-oid "old-blob-0000000000000000000"
                                       :git/mode 33188}]})]
    (is (registry/valid? "observation/revision-at-path-v1" obs)
        (pr-str (registry/explain "observation/revision-at-path-v1" obs)))))

(deftest root-commit-observation-satisfies-schema
  (let [entry {:entry/commit-oid     "abc123def0123456789012345678901234567890"
               :entry/path-raw       "top.md"
               :entry/blob-oid       "456789abc0123456789012345678901234567890"
               :entry/mode           33188
               :entry/policy-version "markdown-tree-v1"}
        obs   (ratp/revision-at-path entry
                 {:resource-id       test-uuid
                  :tree-oid          "abcdef0123456789abcdef0123456789abcdef01"
                  :parent-commit-oid nil
                  :parent-blob-oid   nil
                  :observed-at       #inst "2025-01-01T00:00:00Z"
                  :parent-entries    nil})]
    (is (registry/valid? "observation/revision-at-path-v1" obs)
        (pr-str (registry/explain "observation/revision-at-path-v1" obs)))))

;; ---- Deduplication tests ----

(deftest deduplicate-removes-identical-observations
  (let [entry {:entry/commit-oid     "abc"
               :entry/path-raw       "doc.md"
               :entry/blob-oid       "def"
               :entry/mode           33188
               :entry/policy-version "v1"}
        ctx   {:resource-id test-uuid :tree-oid "t" :observed-at #inst "2025-01-01"
               :parent-entries nil}
        obs1  (ratp/revision-at-path entry ctx)
        obs2  (ratp/revision-at-path entry ctx)]
    ;; Same identity key but different observation IDs
    (is (= (ratp/observation-id-key obs1) (ratp/observation-id-key obs2)))
    (is (not= (:revision-at-path/id obs1) (:revision-at-path/id obs2)))
    ;; Dedup keeps one
    (let [deduped (ratp/deduplicate [obs1 obs2])]
      (is (= 1 (count deduped))))))

(deftest deduplicate-keeps-distinct-observations
  (let [entry1 {:entry/commit-oid "abc" :entry/path-raw "a.md"
                :entry/blob-oid "d" :entry/mode 33188 :entry/policy-version "v1"}
        entry2 {:entry/commit-oid "abc" :entry/path-raw "b.md"
                :entry/blob-oid "e" :entry/mode 33188 :entry/policy-version "v1"}
        ctx    {:resource-id test-uuid :tree-oid "t" :observed-at #inst "2025-01-01"
                :parent-entries nil}
        obs1   (ratp/revision-at-path entry1 ctx)
        obs2   (ratp/revision-at-path entry2 ctx)]
    (is (= 2 (count (ratp/deduplicate [obs1 obs2]))))))

(deftest deduplicate-preserves-first-occurrence
  (let [entry {:entry/commit-oid "abc" :entry/path-raw "x.md"
               :entry/blob-oid "d" :entry/mode 33188 :entry/policy-version "v1"}
        ctx   {:resource-id test-uuid :tree-oid "t" :observed-at #inst "2025-01-01"
               :parent-entries nil}
        obs1  (ratp/revision-at-path entry ctx)
        obs2  (ratp/revision-at-path entry ctx)
        [kept] (ratp/deduplicate [obs1 obs2])]
    (is (= (:revision-at-path/id obs1) (:revision-at-path/id kept)))))

;; ---- Integration tests with real Git repository ----

(deftest c1-root-commit-all-paths-are-initial
  (testing "C1 (root) has :initial evidence for all paths"
    (let [entries (entries-at :c1)
          selected (sel/select-markdown (:c1 (:commit-oids @fixture)) entries)]
      (is (pos? (count selected)))
      (doseq [entry selected]
        (let [obs (ratp/revision-at-path entry
                    {:resource-id       test-uuid
                     :tree-oid          (tree-oid-for :c1)
                     :parent-commit-oid nil
                     :parent-blob-oid   nil
                     :observed-at       #inst "2025-01-01T00:00:00Z"
                     :parent-entries    nil})]
          (is (= :initial (:revision/evidence obs))
              (str "path " (:entry/path-raw entry) " should be :initial"))
          (is (not (contains? obs :revision/parent-commit-oid)))
          (is (not (contains? obs :revision/parent-blob-oid))))))))

(deftest c2-adds-and-modifies
  (testing "C2 shows :add for new paths and :modify for changed paths"
    (let [entries    (entries-at :c2)
          selected   (sel/select-markdown (:c2 (:commit-oids @fixture)) entries)
          parent-ents (parent-entries-at :c2)
          parent-oid  (parent-commit-oid-for :c2)
          observations (mapv (fn [entry]
                               (ratp/revision-at-path entry
                                 {:resource-id       test-uuid
                                  :tree-oid          (tree-oid-for :c2)
                                  :parent-commit-oid parent-oid
                                  :parent-blob-oid   nil
                                  :observed-at       #inst "2025-01-02T00:00:00Z"
                                  :parent-entries    parent-ents}))
                             selected)]
      ;; docs/guide.md is new in C2
      (let [guide-obs (first (filter #(= "docs/guide.md" (:revision/path-raw %)) observations))]
        (is (some? guide-obs))
        (is (= :add (:revision/evidence guide-obs))))
      ;; top.md was modified
      (let [top-obs (first (filter #(= "top.md" (:revision/path-raw %)) observations))]
        (is (some? top-obs))
        (is (= :modify (:revision/evidence top-obs)))))))

(deftest c3-deletes-and-adds-unicode
  (testing "C3 shows :delete for top.md and :add for .ημ/spec.md"
    (let [entries    (entries-at :c3)
          selected   (sel/select-markdown (:c3 (:commit-oids @fixture)) entries)
          parent-ents (parent-entries-at :c3)
          parent-oid  (parent-commit-oid-for :c3)
          observations (mapv (fn [entry]
                               (ratp/revision-at-path entry
                                 {:resource-id       test-uuid
                                  :tree-oid          (tree-oid-for :c3)
                                  :parent-commit-oid parent-oid
                                  :parent-blob-oid   nil
                                  :observed-at       #inst "2025-01-03T00:00:00Z"
                                  :parent-entries    parent-ents}))
                             selected)]
      ;; .ημ/spec.md is new
      (let [spec-obs (first (filter #(= ".ημ/spec.md" (:revision/path-raw %)) observations))]
        (is (some? spec-obs))
        (is (= :add (:revision/evidence spec-obs)))
        ;; Unicode path preserved exactly
        (is (= ".ημ/spec.md" (:revision/path-raw spec-obs))))
      ;; top.md is NOT in selected (it was deleted, so it's not in the tree)
      (is (not (some #(= "top.md" (:revision/path-raw %)) observations))))))

(deftest c3-deletion-evidence-from-parent
  (testing "top.md deleted in C3 produces :delete evidence via evidence-for-deleted"
    (let [parent-ents (parent-entries-at :c3)
          parent-entry (first (filter #(= "top.md" (:git/path %)) parent-ents))]
      (is (some? parent-entry))
      (is (= :delete (ratp/evidence-for-deleted parent-entry []))))))

(deftest c4-continuity-for-unchanged
  (testing "C4: docs/guide.md was modified, readme.txt is continuity"
    (let [entries    (entries-at :c4)
          selected   (sel/select-markdown (:c4 (:commit-oids @fixture)) entries)
          parent-ents (parent-entries-at :c4)
          parent-oid  (parent-commit-oid-for :c4)
          observations (mapv (fn [entry]
                               (ratp/revision-at-path entry
                                 {:resource-id       test-uuid
                                  :tree-oid          (tree-oid-for :c4)
                                  :parent-commit-oid parent-oid
                                  :parent-blob-oid   nil
                                  :observed-at       #inst "2025-01-04T00:00:00Z"
                                  :parent-entries    parent-ents}))
                             selected)]
      ;; docs/guide.md modified in C4
      (let [guide-obs (first (filter #(= "docs/guide.md" (:revision/path-raw %)) observations))]
        (is (some? guide-obs))
        (is (= :modify (:revision/evidence guide-obs)))))))

(deftest revisions-for-commit-handles-multiple-entries
  (testing "revisions-for-commit constructs observations for all selected entries"
    (let [entries  (select-at :c2)
          obs      (ratp/revisions-for-commit entries
                     {:resource-id       test-uuid
                      :tree-oid          (tree-oid-for :c2)
                      :parent-commit-oid (parent-commit-oid-for :c2)
                      :observed-at       #inst "2025-01-02T00:00:00Z"
                      :parent-entries    (parent-entries-at :c2)})]
      (is (= (count entries) (count obs)))
      (is (every? #(= :revision/at-path-observed (:observation/type %)) obs))
      (is (every? #(= test-uuid (:resource-id %)) obs)))))

;; ---- Blob retrieval through Git-object access ----

(deftest blob-retrievable-through-recorded-oid
  (testing "source bytes can be retrieved via the recorded blob OID without checkout"
    (let [{:keys [repo-path commit-oids]} @fixture
          entries (entries-at :c2)
          guide-entry (first (filter #(= "docs/guide.md" (:git/path %)) entries))
          blob-oid (:git/blob-oid guide-entry)
          repo-builder (org.eclipse.jgit.storage.file.FileRepositoryBuilder.)
          repo (-> repo-builder
                   (.setGitDir (java.io.File. repo-path ".git"))
                   (.readEnvironment)
                   (.findGitDir)
                   .build)
          object-reader (.newObjectReader repo)
          blob-id (org.eclipse.jgit.lib.ObjectId/fromString blob-oid)
          obj (.open object-reader blob-id)]
      (try
        (let [bs (.getBytes obj)
              content (String. bs "UTF-8")]
          (is (= "# Guide\n" content))
          (is (pos? (alength bs))))
        (finally
          (.close object-reader)
          (.close repo))))))

(deftest unicode-path-blob-retrievable
  (testing "blob for Unicode path .ημ/spec.md is retrievable by OID"
    (let [{:keys [repo-path commit-oids]} @fixture
          entries (entries-at :c3)
          spec-entry (first (filter #(= ".ημ/spec.md" (:git/path %)) entries))
          blob-oid (:git/blob-oid spec-entry)
          repo-builder (org.eclipse.jgit.storage.file.FileRepositoryBuilder.)
          repo (-> repo-builder
                   (.setGitDir (java.io.File. repo-path ".git"))
                   (.readEnvironment)
                   (.findGitDir)
                   .build)
          object-reader (.newObjectReader repo)
          blob-id (org.eclipse.jgit.lib.ObjectId/fromString blob-oid)
          obj (.open object-reader blob-id)]
      (try
        (let [bs (.getBytes obj)
              content (String. bs "UTF-8")]
          (is (= "# Spec\n" content)))
        (finally
          (.close object-reader)
          (.close repo))))))
