(ns epiphany.infra.git-test
  (:require [clojure.java.io :as io]
            [clojure.java.shell :as shell]
            [clojure.test :refer [deftest is]]
            [epiphany.infra.git :as git]))

(defn- sh! [& command]
  (let [{:keys [exit out err]} (apply shell/sh command)]
    (when-not (zero? exit)
      (throw (ex-info "Fixture command failed" {:command command :err err})))
    out))

(defn- temporary-directory []
  (.toFile (java.nio.file.Files/createTempDirectory "epiphany-git-test" (make-array java.nio.file.attribute.FileAttribute 0))))

(deftest resolves-common-git-directory-for-a-worktree
  (let [repository (temporary-directory)
        worktree (io/file repository "linked-worktree")]
    (sh! "git" "init" (.getPath repository))
    (sh! "git" "-C" (.getPath repository) "config" "user.email" "test@example.invalid")
    (sh! "git" "-C" (.getPath repository) "config" "user.name" "Epiphany Test")
    (spit (io/file repository "README.md") "fixture\n")
    (sh! "git" "-C" (.getPath repository) "add" "README.md")
    (sh! "git" "-C" (.getPath repository) "commit" "-m" "fixture")
    (sh! "git" "-C" (.getPath repository) "worktree" "add" "-b" "linked" (.getPath worktree))
    (is (= (.getCanonicalPath (io/file repository ".git"))
           (git/common-git-directory (.getPath worktree))))))

(deftest rejects-a-non-git-path-with-structured-context
  (let [not-a-repository (temporary-directory)]
    (try
      (git/common-git-directory (.getPath not-a-repository))
      (is false "Expected non-Git path to fail")
      (catch clojure.lang.ExceptionInfo error
        (is (= (.getPath not-a-repository)
               (:repository-path (ex-data error))))
        (is (string? (:git-error (ex-data error))))))))

(deftest resolves-common-git-directory-for-a-bare-repository
  (let [parent (temporary-directory)
        repository (io/file parent "notes.git")]
    (sh! "git" "init" "--bare" (.getPath repository))
    (is (= (.getCanonicalPath repository)
           (git/common-git-directory (.getPath repository))))))
