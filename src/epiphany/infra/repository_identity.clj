(ns epiphany.infra.repository-identity
  "Resolve Git-local repository identity across repo shapes.

   Validates a path as a normal worktree, bare repository, or linked worktree.
   Resolves the common Git directory. Creates or reuses the Git-local
   repository.edn metadata file containing a resource-id UUID."
  (:require [epiphany.infra.git :as git]
            [epiphany.infra.repository-metadata-file :as metadata-file]))

(defn resolve-repository
  "Given a repository path, validate it as a Git repository, resolve the
   common Git directory, and create or reuse the repository identity metadata.

   Returns a map:
     :repository-path  the original path, preserved exactly
     :common-git-dir   resolved common Git directory
     :resource-id      UUID identifying this repository
     :created?         true if a new resource-id was generated and written

   Throws ex-info with structured error data for:
     - Non-Git paths (not-a-repository)
     - Unreadable Git metadata (unreadable-git-metadata)"
  [repository-path]
  (let [common-git-dir (try
                         (git/common-git-directory repository-path)
                         (catch clojure.lang.ExceptionInfo e
                           (throw (ex-info "Repository path is not a Git repository"
                                           {:code :not-a-repository
                                            :repository-path repository-path
                                            :git-error (:git-error (ex-data e))} e))))]
    (try
      (let [existing (metadata-file/read! common-git-dir)]
        {:repository-path repository-path
         :common-git-dir common-git-dir
         :resource-id (:resource-id existing)
         :created? false})
      (catch java.io.FileNotFoundException _
        (let [resource-id (random-uuid)]
          (metadata-file/write! common-git-dir resource-id)
          {:repository-path repository-path
           :common-git-dir common-git-dir
           :resource-id resource-id
           :created? true}))
      (catch clojure.lang.ExceptionInfo e
        (throw (ex-info "Unreadable repository metadata"
                        {:code :unreadable-git-metadata
                         :repository-path repository-path
                         :common-git-dir common-git-dir
                         :error (.getMessage e)}
                        e)))
      (catch RuntimeException e
        (throw (ex-info "Unreadable repository metadata"
                        {:code :unreadable-git-metadata
                         :repository-path repository-path
                         :common-git-dir common-git-dir
                         :error (.getMessage e)}
                        e))))))
