(ns epiphany.infra.git
  "Git object access via JGit. Wraps Java interop at the infra edge;
   never leaks Java types into domain."
  (:require [clojure.java.io :as io]
            [clojure.java.shell :as shell]
            [clojure.string :as string])
  (:import [org.eclipse.jgit.lib FileMode ObjectId PersonIdent]
           [org.eclipse.jgit.revwalk RevWalk RevCommit]
           [org.eclipse.jgit.treewalk TreeWalk]
           [org.eclipse.jgit.storage.file FileRepositoryBuilder]))

(defn common-git-directory [repository-path]
  (let [{:keys [exit out err]} (shell/sh "git" "-C" repository-path "rev-parse" "--path-format=absolute" "--git-common-dir")]
    (if (zero? exit)
      (string/trim out)
      (throw (ex-info "Could not resolve Git common directory"
                      {:repository-path repository-path
                       :git-error (string/trim err)})))))

(defn- person-ident->map
  "Convert a JGit PersonIdent to a plain map."
  [^PersonIdent ident]
  (when ident
    {:person/name       (.getName ident)
     :person/email      (.getEmailAddress ident)
     :person/timestamp  (.getWhen ident)}))

(defn- rev-commit->commit-map
  "Extract commit data from a RevCommit as a plain EDN map."
  [^RevCommit commit]
  {:commit/oid           (.getName (.getId commit))
   :commit/parent-oids   (mapv #(.getName (.getId ^RevCommit %))
                               (.getParents commit))
   :commit/tree-oid      (.getName (.getId (.getTree commit)))
   :commit/author        (person-ident->map (.getAuthorIdent commit))
   :commit/committer     (person-ident->map (.getCommitterIdent commit))
   :commit/message-bytes (.getBytes (.getFullMessage commit) "UTF-8")
   :commit/message-text  (string/trimr (.getFullMessage commit))})

(defn- open-repository
  "Open a JGit Repository from a filesystem path."
  [^String repository-path]
  (-> (FileRepositoryBuilder.)
      (.setGitDir (io/file repository-path ".git"))
      (.readEnvironment)
      (.findGitDir)
      .build))

(defn- resolve-ref-oid
  "Resolve a ref name to its target OID string. Returns nil on failure."
  [^org.eclipse.jgit.lib.Repository repo ^String ref-name]
  (try
    (let [ref (.exactRef repo ref-name)]
      (when ref
        (.getName (.getObjectId ref))))
    (catch Exception _ nil)))

(defn- walk-commits-from
  "Walk commits reachable from a single ref. Returns a map with
   :commits (vector of commit observation maps in walk order) and
   :failures (vector of failure maps for unreadable objects).
   Malformed/unreadable objects are recorded per-item and do not
   conceal completed observations."
  [^org.eclipse.jgit.lib.Repository repo ref-name oid-str]
  (let [walk (RevWalk. repo)
        failures (atom [])]
    (try
      (.setRetainBody walk true)
      (let [oid  (ObjectId/fromString oid-str)
            head (.parseCommit walk oid)]
        (.markStart walk head)
        (let [commits (mapv (fn [^RevCommit commit]
                              (try
                                (rev-commit->commit-map commit)
                                (catch Exception e
                                  (swap! failures conj {:failure/oid     (.getName (.getId commit))
                                                        :failure/ref     ref-name
                                                        :failure/reason  "object-unreadable"
                                                        :failure/message (.getMessage e)})
                                  nil)))
                            (iterator-seq (.iterator walk)))]
          {:commits  (vec (remove nil? commits))
           :failures @failures}))
      (finally
        (.close walk)))))

(defn reachable-commits
  "Walk the commit graph reachable from `selected-refs` in `repository-path`.

   Returns a map:
     :commits        vector of commit observation maps (deduplicated by OID)
     :failures       vector of failure maps for unreachable/unreadable objects
     :commit-count   total unique commits discovered
     :failure-count  total failures recorded

   Determinism: refs are sorted alphabetically; each commit is attributed
   to the alphabetically-first ref that reaches it.

   Each commit map is ready to be wrapped in an observation envelope."
  [^String repository-path selected-refs]
  (let [repo        (open-repository repository-path)
        sorted-refs (sort selected-refs)

        ;; Resolve all ref names to OID strings
        ref-resolutions (mapv (fn [ref-name]
                                {:ref-name  ref-name
                                 :oid-str   (resolve-ref-oid repo ref-name)})
                              sorted-refs)

        failures (atom [])]

    ;; Record failures for unresolvable refs
    (doseq [{:keys [ref-name oid-str]} ref-resolutions]
      (when-not oid-str
        (swap! failures conj {:failure/ref     ref-name
                              :failure/reason  "ref-not-found"
                              :failure/message (str "Could not resolve ref: " ref-name)})))

    ;; Walk each ref independently, merging by OID
    (let [seen          (atom #{})
          commits-vec   (atom [])
          ref-context   (atom {})]
      (doseq [{:keys [ref-name oid-str]} ref-resolutions]
        (when oid-str
          (let [{walk-commits :commits walk-failures :failures} (walk-commits-from repo ref-name oid-str)]
            ;; Record any failures from this ref walk
            (swap! failures into walk-failures)
            ;; Deduplicate commits by OID
            (doseq [c walk-commits]
              (let [oid (:commit/oid c)]
                (when-not (contains? @seen oid)
                  (swap! seen conj oid)
                  (swap! commits-vec conj c)
                  (swap! ref-context assoc oid ref-name)))))))

      {:commits       (mapv (fn [c]
                              (assoc c :git/ref-context
                                     (get @ref-context (:commit/oid c) "unknown")))
                            @commits-vec)
       :failures      @failures
       :commit-count  (count @commits-vec)
       :failure-count (count @failures)})))

(defn- mode->type
  "Convert a JGit FileMode to a keyword indicating the entry type."
  [^FileMode mode]
  (cond
    (.equals mode FileMode/REGULAR_FILE) :file
    (.equals mode FileMode/EXECUTABLE_FILE) :file
    (.equals mode FileMode/TREE) :tree
    (.equals mode FileMode/SYMLINK) :symlink
    (.equals mode FileMode/GITLINK) :gitlink
    :else :missing))

(defn- walk-tree
  "Walk all entries in a commit's tree. Returns a vector of raw
   tree-entry maps with exact Git path strings, blob OIDs, modes,
   and entry types. No files are checked out."
  [^org.eclipse.jgit.lib.Repository repo ^RevCommit commit]
  (let [tw (TreeWalk. repo)]
    (try
      (.addTree tw (.getTree commit))
      (.setRecursive tw true)
      (loop [entries []]
        (if (.next tw)
          (let [^FileMode mode (.getFileMode tw)
                path (.getPathString tw)]
            (recur (conj entries
                         {:git/path     path
                          :git/blob-oid (.getName (.getObjectId tw 0))
                          :git/mode     (.getBits mode)
                          :git/type     (mode->type mode)})))
          entries))
      (finally
        (.close tw)))))

(defn commit-tree-entries
  "Walk the tree of a single commit, identified by OID string.
   Returns a map:
     :commit-oid  — the commit OID
     :entries     — vector of raw tree-entry maps
     :failure     — non-nil if the commit could not be read

   No files are checked out. Paths are preserved exactly as observed."
  [^String repository-path commit-oid-str]
  (let [repo  (open-repository repository-path)
        walk  (RevWalk. repo)]
    (try
      (.setRetainBody walk false)
      (let [oid    (ObjectId/fromString commit-oid-str)
            commit (.parseCommit walk oid)]
        {:commit-oid commit-oid-str
         :entries    (walk-tree repo commit)
         :failure    nil})
      (catch Exception e
        {:commit-oid commit-oid-str
         :entries    []
         :failure    {:failure/oid    commit-oid-str
                      :failure/reason "commit-unreadable"
                      :failure/message (.getMessage e)}})
      (finally
        (.close walk)))))
