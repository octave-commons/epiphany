(ns epiphany.infra.repository-identity-test
  "Integration tests for resolve-repository-identity across repo shapes.
   Exercises real Git operations and filesystem metadata. No external services."
  (:require [clojure.java.io :as io]
            [clojure.java.shell :as shell]
            [clojure.test :refer [deftest is testing]]
            [epiphany.infra.repository-identity :as repo-ident]
            [epiphany.infra.repository-metadata-file :as metadata-file]))

(defn- sh! [& command]
  (let [{:keys [exit out err]} (apply shell/sh command)]
    (when-not (zero? exit)
      (throw (ex-info "Fixture command failed" {:command command :err err})))
    out))

(defn- temporary-directory []
  (.toFile (java.nio.file.Files/createTempDirectory
            "epiphany-repo-id-test"
            (make-array java.nio.file.attribute.FileAttribute 0))))

(defn- init-normal-repo [dir]
  (sh! "git" "init" (.getPath dir))
  (sh! "git" "-C" (.getPath dir) "config" "user.email" "test@example.invalid")
  (sh! "git" "-C" (.getPath dir) "config" "user.name" "Epiphany Test")
  (spit (io/file dir "README.md") "fixture\n")
  (sh! "git" "-C" (.getPath dir) "add" "README.md")
  (sh! "git" "-C" (.getPath dir) "commit" "-m" "fixture"))

(defn- init-bare-repo [dir]
  (sh! "git" "init" "--bare" (.getPath dir)))

(defn- init-linked-worktree [repo-dir worktree-dir]
  (init-normal-repo repo-dir)
  (sh! "git" "-C" (.getPath repo-dir) "worktree" "add"
       "-b" "linked" (.getPath worktree-dir)))

;; ── Normal worktree ──────────────────────────────────────────────

(deftest resolves-normal-worktree
  (let [repo (temporary-directory)
        _ (init-normal-repo repo)
        result (repo-ident/resolve-repository (.getPath repo))]
    (is (= (.getPath repo) (:repository-path result)))
    (is (.exists (io/file (:common-git-dir result))))
    (is (uuid? (:resource-id result)))
    (is (true? (:created? result)))))

;; ── Bare repository ──────────────────────────────────────────────

(deftest resolves-bare-repository
  (let [repo (io/file (temporary-directory) "notes.git")
        _ (init-bare-repo repo)
        result (repo-ident/resolve-repository (.getPath repo))]
    (is (= (.getPath repo) (:repository-path result)))
    (is (= (.getCanonicalPath repo)
           (.getCanonicalPath (io/file (:common-git-dir result)))))
    (is (uuid? (:resource-id result)))
    (is (true? (:created? result)))))

;; ── Linked worktree ──────────────────────────────────────────────

(deftest resolves-linked-worktree
  (let [parent (temporary-directory)
        repo (io/file parent "main")
        worktree (io/file parent "linked")
        _ (init-linked-worktree repo worktree)
        result (repo-ident/resolve-repository (.getPath worktree))]
    (is (= (.getPath worktree) (:repository-path result)))
    (is (= (.getCanonicalPath (io/file repo ".git"))
           (.getCanonicalPath (io/file (:common-git-dir result)))))
    (is (uuid? (:resource-id result)))
    (is (true? (:created? result)))))

;; ── Reuses existing repository.edn ───────────────────────────────

(deftest reuses-existing-repository-edn
  (let [repo (temporary-directory)
        _ (init-normal-repo repo)
        first-result  (repo-ident/resolve-repository (.getPath repo))
        second-result (repo-ident/resolve-repository (.getPath repo))]
    (is (false? (:created? second-result)))
    (is (= (:resource-id first-result)
           (:resource-id second-result)))))

(deftest preserves-existing-resource-id-over-competing-write
  (let [repo (temporary-directory)
        _ (init-normal-repo repo)
        known-id #uuid "7a6b0d26-1000-4000-8000-000000000001"
        _ (metadata-file/write! (.getCanonicalPath
                                  (io/file (.getPath repo) ".git"))
                                known-id)
        result (repo-ident/resolve-repository (.getPath repo))]
    (is (= known-id (:resource-id result)))
    (is (false? (:created? result)))))

;; ── Invalid / non-Git path ───────────────────────────────────────

(deftest rejects-non-git-path-with-structured-error
  (let [not-a-repo (temporary-directory)]
    (try
      (repo-ident/resolve-repository (.getPath not-a-repo))
      (is false "Expected non-Git path to fail")
      (catch clojure.lang.ExceptionInfo e
        (is (= :not-a-repository (:code (ex-data e))))
        (is (= (.getPath not-a-repo) (:repository-path (ex-data e))))
        (is (string? (:git-error (ex-data e))))))))

(deftest rejects-non-git-path-without-creating-partial-state
  (let [not-a-repo (temporary-directory)]
    (try
      (repo-ident/resolve-repository (.getPath not-a-repo))
      (catch clojure.lang.ExceptionInfo _))
    (is (not (.exists (io/file (.getPath not-a-repo)
                               ".git"
                               "corpus-archaeology"
                               "repository.edn")))
        "No metadata directory or file should be created for a non-Git path")))

;; ── Unreadable Git metadata ──────────────────────────────────────

(deftest rejects-corrupt-repository-with-distinct-error
  (let [repo (temporary-directory)
        _ (init-normal-repo repo)
        common-git-dir (.getCanonicalPath (io/file (.getPath repo) ".git"))
        metadata-file (io/file common-git-dir "corpus-archaeology" "repository.edn")]
    ;; Write valid metadata, then corrupt the file content
    (metadata-file/write! common-git-dir (random-uuid))
    (spit metadata-file "not-valid-edn{{{")
    (try
      (repo-ident/resolve-repository (.getPath repo))
      (is false "Expected unreadable metadata to fail")
      (catch clojure.lang.ExceptionInfo e
        (is (= :unreadable-git-metadata (:code (ex-data e)))
            (str "Expected :unreadable-git-metadata code, got: " (pr-str (ex-data e))))))))

;; ── Unicode path preservation ────────────────────────────────────

(deftest preserves-unicode-path-exactly
  (let [parent (temporary-directory)
        unicode-name (str (.getPath parent) "/\u03b7\u03bc-repo")
        repo (io/file unicode-name)
        _ (.mkdirs repo)
        _ (init-normal-repo repo)
        result (repo-ident/resolve-repository (.getPath repo))]
    (is (= (.getPath repo) (:repository-path result))
        "The exact Unicode path is preserved without normalization")
    (is (uuid? (:resource-id result)))))
