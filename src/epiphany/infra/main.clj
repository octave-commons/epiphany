(ns epiphany.infra.main
  "Single executable entry point for `epiphany` (short alias: `ep`).
  Dispatches subcommands and wires profile/adapter resolution."
  (:require [clojure.java.shell :as shell]
            [clojure.string :as string]
            [clojure.tools.cli :as cli]
            [clojure.data.json :as json]
            [epiphany.infra.services :as services]
            [epiphany.infra.profile :as profile]
            [epiphany.infra.http :as http]
            [epiphany.infra.git :as git]
            [epiphany.infra.adapters.in-memory :as in-memory]
            [epiphany.infra.adapters.mongo :as mongo]
            [epiphany.application.registration :as registration]
            [epiphany.domain.hybrid-search :as hs]
            [epiphany.domain.evidence :as evidence]
            [epiphany.domain.diff :as diff]
            [epiphany.domain.lineage-trace :as lineage-trace])
  (:gen-class))

(def version "0.1.0")

;; ---------------------------------------------------------------------------
;; Global options (before subcommand)

(def global-options
  [["-h" "--help" "Show this help and exit."]
   ["-v" "--version" "Show the version and exit."]
   ["-c" "--check-services" "Check service readiness and exit."]])

;; ---------------------------------------------------------------------------
;; Register subcommand

(def register-options
  [["-r" "--request-id UUID" "Idempotent request ID (UUID format)"
    :parse-fn #(java.util.UUID/fromString %)]
   ["-p" "--profile PROFILE" "Profile: :local (in-memory) or :services (MongoDB)"
    :default :local
    :parse-fn keyword]
   ["-h" "--help" "Show register help and exit."]])

(defn- format-register-result
  "Format a registration result for CLI output."
  [{:keys [resource-id repository-path common-git-dir request-id profile]}]
  (string/join
   \newline
   [(str "Registered: " repository-path)
    (str "  Resource ID:     " resource-id)
    (str "  Common Git dir:  " common-git-dir)
    (str "  Profile:         " (name profile))
    (when request-id
      (str "  Request ID:      " request-id))]))

(defn- run-register
  "Execute the register subcommand. Returns {:exit int, :out string}."
  [args]
  (let [{:keys [options errors summary arguments]}
        (cli/parse-opts args register-options)
        profile (:profile options)]
    (cond
      errors
      {:exit 1
       :out (string/join \newline (concat errors ["" (str "Usage: ep register [options] <path>\n\n" summary)]))}

      (:help options)
      {:exit 0 :out (str "Usage: ep register [options] <path>\n\n" summary)}

      (empty? arguments)
      {:exit 1 :out "Error: repository path required.\nUsage: ep register [options] <path>"}

      (not (profile/valid-profile? profile))
      {:exit 1 :out (str "Error: invalid profile " (pr-str profile)
                         ". Valid: " (pr-str profile/valid-profiles))}

      :else
      (let [repository-path (first arguments)
            request-id (:request-id options)
            result
            (try
              (case profile
                :local
                (let [git-resolve (fn [path]
                                    (let [{:keys [exit out err]}
                                          (clojure.java.shell/sh
                                           "git" "-C" path "rev-parse"
                                           "--path-format=absolute"
                                          "--git-common-dir")]
                                      (if (zero? exit)
                                        (string/trim out)
                                        (throw (ex-info
                                                (str "Not a Git repository: " path)
                                                {:repository-path path
                                                 :git-error (string/trim err)})))))
                      adapters (in-memory/make {:common-git-dir-fn git-resolve})]
                  (registration/register! adapters
                                          (cond-> {:repository-path repository-path}
                                            request-id (assoc :request-id request-id))))

                :services
                (let [conn (try
                             (mongo/connect!)
                             (catch Exception e
                               (throw (ex-info
                                       (str "Cannot connect to MongoDB: " (.getMessage e))
                                       {:code :unavailable
                                        :hint "Start MongoDB with: docker compose up -d mongodb"}))))
                      git-resolve (fn [path]
                                    (let [{:keys [exit out err]}
                                          (clojure.java.shell/sh
                                           "git" "-C" path "rev-parse"
                                           "--path-format=absolute"
                                           "--git-common-dir")]
                                      (if (zero? exit)
                                        (string/trim out)
                                        (throw (ex-info
                                                (str "Not a Git repository: " path)
                                                {:repository-path path
                                                 :git-error (string/trim err)})))))
                      obs-adapter (mongo/make-observations-adapter conn)]
                  (try
                    (let [adapters {:git {:common-git-directory git-resolve}
                                   :repository-metadata {:read (fn [_] nil)
                                                        :write (fn [_ _id] nil)}
                                   :observations obs-adapter}]
                      (registration/register! adapters
                                              (cond-> {:repository-path repository-path}
                                                request-id (assoc :request-id request-id))))
                    (finally
                      (mongo/disconnect! conn)))))

              (catch clojure.lang.ExceptionInfo e
                (let [data (ex-data e)]
                  {:exit 1
                   :out (str "Error: " (.getMessage e)
                             (when (:git-error data)
                               (str "\n  Git error: " (:git-error data)))
                             (when (:code data)
                               (str "\n  Code: " (name (:code data))))
                             (when (:hint data)
                               (str "\n  Hint: " (:hint data))))}))
              (catch Exception e
                {:exit 1 :out (str "Error: " (.getMessage e))}))]
        (if (:exit result)
          result
          {:exit 0
           :out (format-register-result (assoc result :profile profile))})))))

;; ---------------------------------------------------------------------------
;; Status subcommand

(def status-options
  [["-r" "--resource-id UUID" "Resource ID to query"
    :parse-fn #(java.util.UUID/fromString %)]
   ["-p" "--profile PROFILE" "Profile: :local (in-memory) or :services (MongoDB)"
    :default :local
    :parse-fn keyword]
   ["-h" "--help" "Show status help and exit."]])

(defn- format-run
  "Format a single ingestion run for display."
  [run]
  (str "  Run " (:observation/id run)
       "\n    Refs:      " (string/join ", " (:ingestion/selected-refs run))
       "\n    Commits:   " (:ingestion/commit-count run)
       "\n    Failures:  " (:ingestion/failure-count run)
       (when (seq (:ingestion/failures run))
         (str "\n    Errors:    "
              (string/join ", "
                           (map (fn [f]
                                  (str (:failure/reason f)
                                       (when (:failure/message f)
                                         (str " (" (:failure/message f) ")"))))
                                (:ingestion/failures run)))))))

(defn format-checkpoint
  "Format a single checkpoint for display."
  [ckpt]
  (str "  Checkpoint " (:checkpoint/projection-name ckpt)
       " v" (:checkpoint/projection-version ckpt)
       "\n    Status:    " (name (:checkpoint/status ckpt))
       "\n    Processed: " (:checkpoint/processed-count ckpt)
       (when (:checkpoint/last-processed-oid ckpt)
         (str "\n    Last OID:  " (:checkpoint/last-processed-oid ckpt)))
       (when (:checkpoint/error-message ckpt)
         (str "\n    Error:     " (:checkpoint/error-message ckpt)))))

(defn- run-status
  "Execute the status subcommand. Returns {:exit int, :out string}."
  [args]
  (let [{:keys [options errors summary _arguments]}
        (cli/parse-opts args status-options)
        profile (:profile options)]
    (cond
      errors
      {:exit 1
       :out (string/join \newline (concat errors ["" (str "Usage: ep status [options]\n\n" summary)]))}

      (:help options)
      {:exit 0 :out (str "Usage: ep status [options]\n\n" summary)}

      (not (profile/valid-profile? profile))
      {:exit 1 :out (str "Error: invalid profile " (pr-str profile)
                         ". Valid: " (pr-str profile/valid-profiles))}

      :else
      (let [resource-id (:resource-id options)]
        (if-not resource-id
          {:exit 1 :out "Error: --resource-id required.\nUsage: ep status --resource-id <uuid>"}
          (try
            (case profile
              :local
              {:exit 1 :out "Error: :local profile does not persist data. Use --profile :services."}

              :services
              (let [conn (try
                           (mongo/connect!)
                           (catch Exception e
                             (throw (ex-info
                                     (str "Cannot connect to MongoDB: " (.getMessage e))
                                     {:code :unavailable
                                      :hint "Start MongoDB with: docker compose up -d mongodb"}))))
                    obs-adapter (mongo/make-observations-adapter conn)]
                (try
                  (let [runs ((:list-ingestion-runs obs-adapter) resource-id)
                        output (if (empty? runs)
                                 (str "No ingestion runs found for " resource-id)
                                 (str "Ingestion runs for " resource-id ":\n"
                                      (string/join "\n\n" (map format-run runs))))]
                    {:exit 0 :out output})
                  (finally
                    (mongo/disconnect! conn)))))

            (catch clojure.lang.ExceptionInfo e
              (let [data (ex-data e)]
                {:exit 1
                 :out (str "Error: " (.getMessage e)
                           (when (:code data)
                             (str "\n  Code: " (name (:code data))))
                           (when (:hint data)
                             (str "\n  Hint: " (:hint data))))}))
            (catch Exception e
              {:exit 1 :out (str "Error: " (.getMessage e))})))))))

;; ---------------------------------------------------------------------------
;; Search subcommand

(def search-options
  [["-m" "--mode MODE" "Search mode: lexical, semantic, hybrid"
    :id :mode
    :default :hybrid
    :parse-fn keyword
    :validate [#{:lexical :semantic :hybrid} "Must be lexical, semantic, or hybrid"]]
   ["-l" "--limit N" "Max results"
    :id :limit
    :default 20
    :parse-fn #(Integer/parseInt %)
    :validate [pos? "Must be positive"]]
   ["-f" "--format FORMAT" "Output format: text, edn, json"
    :id :format
    :default :text
    :parse-fn keyword
    :validate [#{:text :edn :json} "Must be text, edn, or json"]]
   [nil "--path-prefix PREFIX" "Filter results by path prefix"
    :id :path-prefix]
   [nil "--ref REF" "Filter results by Git ref"
    :id :ref]
   [nil "--embedding-version VER" "Embedding model version for semantic search"
    :id :embedding-version
    :parse-fn #(Integer/parseInt %)]
   ["-v" "--verbose" "Show diagnostics (profile, versions)"
    :id :verbose]
   ["-p" "--profile PROFILE" "Profile: :local (in-memory) or :services (MongoDB)"
    :id :profile
    :default :local
    :parse-fn keyword]
   ["-h" "--help" "Show search help and exit."
    :id :help]])

(defn- format-result-text
  "Format a single search result for text output."
  [result]
  (let [path (:result/path-raw result)
        score (:result/score result)
        mode (:result/mode result)
        heading (string/join " > " (:result/heading-path result))
        scores (:result/scores result)]
    (str path
         (when (seq heading) (str "\n  Heading:  " heading))
         (str "\n  Score:    " (format "%.4f" score)
              " (" (name mode) ")")
         (when (:lexical scores)
           (str "\n  Lexical:  " (format "%.4f" (:lexical scores))))
         (when (:semantic scores)
           (str "\n  Semantic: " (format "%.4f" (:semantic scores))))
         (str "\n  Commit:   " (:result/commit-oid result)))))

(defn- format-results-text
  "Format search results for text output."
  [results verbose? profile]
  (let [count (count results)
        header (str count " result" (when (not= 1 count) "s"))
        body (string/join "\n\n" (map format-result-text results))]
    (str header
         (when verbose?
           (str "\n\nProfile: " (name profile)))
     "\n\n" body)))

(defn- format-results-edn
  "Format search results as EDN."
  [results]
  (pr-str results))

(defn- format-results-json
  "Format search results as JSON."
  [results]
  (json/write-str results :key-fn (fn [k] (subs (str k) 1))))

(defn- build-search-request
  "Build a hybrid search request map from CLI options."
  [query opts]
  (cond-> {:query query
           :mode (:mode opts)
           :limit (:limit opts)}
    (:path-prefix opts)
    (assoc-in [:filters :path-prefix] (:path-prefix opts))

    (:ref opts)
    (assoc-in [:filters :ref] (:ref opts))

    (:embedding-version opts)
    (assoc :embedding-version (:embedding-version opts))))

(defn- make-search-adapters
  "Create in-memory adapters for search (no persistence needed)."
  [profile]
  (case profile
    :local
    (in-memory/make {:common-git-dir-fn (constantly "/tmp")})

    :services
    (throw (ex-info "Search --profile :services requires running services."
                    {:code :unavailable
                     :hint "Start MongoDB and Ollama with: docker compose up -d"}))))

(defn- run-search
  "Execute the search subcommand. Returns {:exit int, :out string}."
  [args]
  (let [{:keys [options errors summary arguments]}
        (cli/parse-opts args search-options)
        profile (:profile options)]
    (cond
      errors
      {:exit 1
       :out (string/join \newline (concat errors ["" (str "Usage: ep search [options] <query>\n\n" summary)]))}

      (:help options)
      {:exit 0 :out (str "Usage: ep search [options] <query>\n\n" summary)}

      (not (profile/valid-profile? profile))
      {:exit 1 :out (str "Error: invalid profile " (pr-str profile)
                         ". Valid: " (pr-str profile/valid-profiles))}

      (empty? arguments)
      {:exit 1 :out "Error: search query required.\nUsage: ep search [options] <query>"}

      :else
      (let [query (string/join " " arguments)
            request (build-search-request query options)]
        (try
          (let [adapters (make-search-adapters profile)
                results (hs/search adapters request)
                fmt (:format options)
                output (case fmt
                         :edn  (format-results-edn results)
                         :json (format-results-json results)
                         :text (format-results-text results (:verbose options) profile))]
            {:exit 0 :out output})

          (catch clojure.lang.ExceptionInfo e
            (let [data (ex-data e)]
              {:exit 1
               :out (str "Error: " (.getMessage e)
                         (when (:code data)
                           (str "\n  Code: " (name (:code data))))
                         (when (:hint data)
                           (str "\n  Hint: " (:hint data))))}))
          (catch Exception e
            {:exit 1 :out (str "Error: " (.getMessage e))}))))))

;; ---------------------------------------------------------------------------
;; Serve subcommand

(defn- parse-profile
  "Parse a profile keyword from CLI string, stripping leading colon."
  [s]
  (keyword (if (.startsWith ^String s ":") (subs s 1) s)))

(def serve-options
  [["-p" "--profile PROFILE" "Profile: :local (in-memory) or :services (MongoDB)"
    :default :services
    :parse-fn parse-profile]
   [nil "--port PORT" "Port to listen on"
    :default 5197
    :parse-fn #(Integer/parseInt %)]
   ["-h" "--help" "Show serve help and exit."]])

(defn- run-serve
  "Execute the serve subcommand. Starts the HTTP server and joins."
  [args]
  (let [{:keys [options errors summary]} (cli/parse-opts args serve-options)
        profile (:profile options)
        port    (:port options)]
    (cond
      errors
      {:exit 1
       :out (string/join \newline (concat errors ["" (str "Usage: ep serve [options]\n\n" summary)]))}

      (:help options)
      {:exit 0 :out (str "Usage: ep serve [options]\n\n" summary)}

      (not (profile/valid-profile? profile))
      {:exit 1 :out (str "Error: invalid profile " (pr-str profile)
                         ". Valid: " (pr-str profile/valid-profiles))}

      :else
      (try
        (let [adapters (case profile
                         :local
                         (in-memory/make {:common-git-dir-fn
                                          (fn [path]
                                            (let [{:keys [exit out err]}
                                                  (clojure.java.shell/sh
                                                   "git" "-C" path "rev-parse"
                                                   "--path-format=absolute"
                                                   "--git-common-dir")]
                                              (if (zero? exit)
                                                (string/trim out)
                                                (throw (ex-info
                                                        (str "Not a Git repository: " path)
                                                        {:repository-path path
                                                         :git-error (string/trim err)})))))})

                         :services
                         (let [conn (try
                                      (mongo/connect!)
                                      (catch Exception e
                                        (throw (ex-info
                                                (str "Cannot connect to MongoDB: " (.getMessage e))
                                                {:code :unavailable
                                                 :hint "Start MongoDB with: docker compose up -d mongodb"}))))
                               git-resolve (fn [path]
                                             (let [{:keys [exit out err]}
                                                   (clojure.java.shell/sh
                                                    "git" "-C" path "rev-parse"
                                                    "--path-format=absolute"
                                                    "--git-common-dir")]
                                               (if (zero? exit)
                                                 (string/trim out)
                                                 (throw (ex-info
                                                         (str "Not a Git repository: " path)
                                                         {:repository-path path
                                                          :git-error (string/trim err)})))))
                               obs-adapter (mongo/make-observations-adapter conn)]
                           {:git {:common-git-directory git-resolve}
                            :repository-metadata {:read (fn [_] nil)
                                                  :write (fn [_ _id] nil)}
                            :observations obs-adapter}))]
          (println (str "Epiphany workbench starting on http://localhost:" port))
          (println (str "Profile: " (name profile)))
          (http/start-server! adapters port)
          ;; Block until interrupted
          (.addShutdownHook (Runtime/getRuntime)
                            (Thread. (fn []
                                       (println "\nShutting down...")
                                       (when (= :services profile)
                                         ;; TODO: disconnect mongo
                                         ))))
          (.join (Thread/currentThread))
          {:exit 0 :out ""})

        (catch clojure.lang.ExceptionInfo e
          (let [data (ex-data e)]
            {:exit 1
             :out (str "Error: " (.getMessage e)
                       (when (:code data)
                         (str "\n  Code: " (name (:code data))))
                       (when (:hint data)
                         (str "\n  Hint: " (:hint data))))}))
        (catch Exception e
          {:exit 1 :out (str "Error: " (.getMessage e))})))))

;; ---------------------------------------------------------------------------
;; Shared Git evidence helpers (show / diff / trace)

(defn- resolve-common-git-dir
  "Resolve a repository path's common Git directory via `git rev-parse`."
  [repository-path]
  (let [{:keys [exit out err]}
        (shell/sh "git" "-C" repository-path "rev-parse"
                  "--path-format=absolute" "--git-common-dir")]
    (if (zero? exit)
      (string/trim out)
      (throw (ex-info (str "Not a Git repository: " repository-path)
                      {:repository-path repository-path
                       :git-error (string/trim err)})))))

(defn- resolve-commit-oid
  "Resolve a ref, short OID, or HEAD-relative expression to a full commit OID."
  [repository-path expr]
  (let [{:keys [exit out err]}
        (shell/sh "git" "-C" repository-path "rev-parse" "--verify"
                  (str expr "^{commit}"))]
    (if (zero? exit)
      (string/trim out)
      (throw (ex-info (str "Could not resolve commit: " expr)
                      {:repository-path repository-path
                       :git-error (string/trim err)})))))

(defn- make-evidence-git-port
  "Build a :git port backed by real Git object access for a repository's
  working-tree path (epiphany.infra.git's `repository-path` — it resolves
  the .git dir itself; this is NOT the common-git-dir returned by
  resolve-common-git-dir). Matches the (fn [_ arg] ...) shape evidence/diff/
  trace expect (first arg reserved, mirrors the port-fn calling convention
  used in their unit tests)."
  [repository-path]
  {:read-blob (fn [_ oid] (git/read-blob repository-path oid))
   :commit-tree-entries (fn [_ commit-oid] (git/commit-tree-entries repository-path commit-oid))
   :reachable-commits (fn [_ refs] (git/reachable-commits repository-path refs))})

(defn- resolve-evidence-request
  "Parse a section expression and resolve its commit-oid (ref/short-oid/HEAD)
  to a full OID via the repository at `repository-path`."
  [repository-path expr]
  (let [parsed (evidence/parse-section-expression expr)]
    (cond-> parsed
      (:commit-oid parsed)
      (assoc :commit-oid (resolve-commit-oid repository-path (:commit-oid parsed))))))

(def ^:private repo-option
  ["-r" "--repo PATH" "Path to the Git repository" :default "."])

(defn- git-boundary-error
  "Format a caught exception from the Git evidence helpers as a CLI error."
  [e]
  (let [data (ex-data e)]
    {:exit 1
     :out (str "Error: " (.getMessage ^Exception e)
               (when (:git-error data)
                 (str "\n  Git error: " (:git-error data))))}))

;; ---------------------------------------------------------------------------
;; Show subcommand

(def show-options
  [repo-option
   ["-f" "--format FORMAT" "Output format: text or edn"
    :default :text
    :parse-fn keyword
    :validate [#{:text :edn} "Must be text or edn"]]
   ["-h" "--help" "Show show help and exit."]])

(defn- run-show
  "Execute the show subcommand. Returns {:exit int, :out string}."
  [args]
  (let [{:keys [options errors summary arguments]} (cli/parse-opts args show-options)]
    (cond
      errors
      {:exit 1
       :out (string/join \newline (concat errors ["" (str "Usage: ep show [options] <path[#heading][@commit]>\n\n" summary)]))}

      (:help options)
      {:exit 0 :out (str "Usage: ep show [options] <path[#heading][@commit]>\n\n" summary)}

      (empty? arguments)
      {:exit 1 :out "Error: section expression required.\nUsage: ep show [options] <path[#heading][@commit]>"}

      :else
      (try
        (let [repo (:repo options)
              _ (resolve-common-git-dir repo)
              request (resolve-evidence-request repo (first arguments))
              result (evidence/retrieve-evidence {:git (make-evidence-git-port repo)} request)
              output (case (:format options)
                       :edn (evidence/format-evidence-edn result)
                       (evidence/format-evidence-text result))]
          {:exit (if (:evidence/unavailable result) 1 0) :out output})
        (catch clojure.lang.ExceptionInfo e (git-boundary-error e))
        (catch Exception e {:exit 1 :out (str "Error: " (.getMessage e))})))))

;; ---------------------------------------------------------------------------
;; Diff subcommand

(def diff-options
  [repo-option
   ["-f" "--format FORMAT" "Output format: text or edn"
    :default :text
    :parse-fn keyword
    :validate [#{:text :edn} "Must be text or edn"]]
   ["-h" "--help" "Show diff help and exit."]])

(defn- run-diff
  "Execute the diff subcommand. Returns {:exit int, :out string}."
  [args]
  (let [{:keys [options errors summary arguments]} (cli/parse-opts args diff-options)]
    (cond
      errors
      {:exit 1
       :out (string/join \newline (concat errors ["" (str "Usage: ep diff [options] <left-expr> <right-expr>\n\n" summary)]))}

      (:help options)
      {:exit 0 :out (str "Usage: ep diff [options] <left-expr> <right-expr>\n\n" summary)}

      (not= 2 (count arguments))
      {:exit 1 :out "Error: exactly two section expressions required.\nUsage: ep diff [options] <left-expr> <right-expr>"}

      :else
      (try
        (let [repo (:repo options)
              _ (resolve-common-git-dir repo)
              [left-expr right-expr] arguments
              left (resolve-evidence-request repo left-expr)
              right (resolve-evidence-request repo right-expr)
              result (diff/compare-evidence {:git (make-evidence-git-port repo)}
                                            {:left left :right right})
              output (case (:format options)
                       :edn (pr-str result)
                       (str (diff/format-diff-text (:diff/lines result)
                                                   (str "--- " left-expr)
                                                   (str "+++ " right-expr))
                            "\n\n"
                            ;; Continuity is display-only context — never folded into the diff above.
                            "Continuity (not part of the diff): " (pr-str (:diff/continuity result))
                            "\nSummary: " (pr-str (:diff/summary result))))]
          {:exit (if (:diff/failure result) 1 0) :out output})
        (catch clojure.lang.ExceptionInfo e (git-boundary-error e))
        (catch Exception e {:exit 1 :out (str "Error: " (.getMessage e))})))))

;; ---------------------------------------------------------------------------
;; Trace subcommand

(def trace-options
  [repo-option
   [nil "--refs REFS" "Comma-separated refs to walk (default HEAD)"
    :default "HEAD"]
   [nil "--observed-only" "Only include observed (Git-history) edges"]
   [nil "--provisional MODE" "Provisional/rejected edges: include or exclude"
    :default :include
    :parse-fn keyword
    :validate [#{:include :exclude} "Must be include or exclude"]]
   ["-f" "--format FORMAT" "Output format: text or edn"
    :default :text
    :parse-fn keyword
    :validate [#{:text :edn} "Must be text or edn"]]
   ["-h" "--help" "Show trace help and exit."]])

(defn- path-revisions
  "Walk commit history reachable from `refs` in `common-git-dir`, returning
  every revision of `path` as a section map, chronologically sorted.

  Only Git-history revisions are represented — this does not include
  cross-file lineage candidates (no candidate store exists yet; see
  ENG-005A/005B for that prerequisite)."
  [repository-path refs path]
  (let [{:keys [commits]} (git/reachable-commits repository-path refs)
        revisions (keep (fn [c]
                          (let [{:keys [entries]} (git/commit-tree-entries repository-path (:commit/oid c))
                                entry (first (filter #(= path (:git/path %)) entries))]
                            (when entry
                              {:path-raw path
                               :heading-path []
                               :commit-oid (:commit/oid c)
                               :blob-oid (:git/blob-oid entry)
                               :timestamp (get-in c [:commit/author :person/timestamp])})))
                        commits)]
    (sort-by (fn [s] (.getTime ^java.util.Date (or (:timestamp s) (java.util.Date. 0))))
             revisions)))

(defn- format-trace-text
  [trace]
  (let [nodes (:trace/nodes trace)
        edges (:trace/edges trace)]
    (str (count nodes) " node(s), " (count edges) " edge(s) [" (name (:trace/filter trace)) "]"
         "\n\nNodes:\n"
         (string/join "\n"
                      (map-indexed (fn [i n]
                                     (let [s (:node/section n)]
                                       (str "  " i ". " (:path-raw s) " @ " (:commit-oid s)
                                            (when (:timestamp s) (str "  (" (:timestamp s) ")")))))
                                   nodes))
         "\n\nEdges:\n"
         (if (seq edges)
           (string/join "\n"
                        (map (fn [e]
                               (str "  " (:edge/from e) " -> " (:edge/to e)
                                    " [" (name (:edge/relation e)) ", " (name (:edge/status e))
                                    ", confidence=" (:edge/confidence e) "]"))
                             edges))
           "  (none)"))))

(defn- run-trace
  "Execute the trace subcommand. Returns {:exit int, :out string}."
  [args]
  (let [{:keys [options errors summary arguments]} (cli/parse-opts args trace-options)]
    (cond
      errors
      {:exit 1
       :out (string/join \newline (concat errors ["" (str "Usage: ep trace [options] <path>\n\n" summary)]))}

      (:help options)
      {:exit 0 :out (str "Usage: ep trace [options] <path>\n\n" summary)}

      (empty? arguments)
      {:exit 1 :out "Error: path required.\nUsage: ep trace [options] <path>"}

      :else
      (try
        (let [repo (:repo options)
              _ (resolve-common-git-dir repo)
              refs (string/split (:refs options) #",")
              path (first arguments)
              revisions (path-revisions repo refs path)]
          (if (empty? revisions)
            {:exit 1 :out (str "Error: no revisions found for path " (pr-str path)
                               " (refs: " (string/join ", " refs) ")")}
            (let [source (last revisions)
                  history (butlast revisions)
                  trace (lineage-trace/trace-lineage source history []
                                                     {:observed-only? (boolean (:observed-only options))
                                                      :provisional-rejected (:provisional options)})
                  output (case (:format options)
                           :edn (pr-str trace)
                           (format-trace-text trace))]
              {:exit 0 :out output})))
        (catch clojure.lang.ExceptionInfo e (git-boundary-error e))
        (catch Exception e {:exit 1 :out (str "Error: " (.getMessage e))})))))

;; ---------------------------------------------------------------------------
;; Top-level dispatch

(defn- usage [options-summary]
  (string/join
   \newline
   ["epiphany — local-first, Git-backed knowledge archaeology."
    ""
    "Usage: epiphany [global-options] <command> [command-options]"
    "       ep [global-options] <command> [command-options]"
    ""
    "Commands:"
    "  register    Register a local Git repository"
    "  search      Search sections by query (lexical, semantic, or hybrid)"
    "  status      Show ingestion run status for a resource"
    "  show        Open exact historical evidence for a section expression"
    "  diff        Compare two historical section expressions"
    "  trace       Trace a section's Git-history lineage chronology"
    "  serve       Start the workbench HTTP server"
    ""
    "Global Options:"
    options-summary
    ""
    "Run 'ep <command> --help' for command-specific help."]))

(defn run
  "Interpret command-line arguments without side effects.
  Returns {:exit int, :out string}."
  [args]
  (let [{:keys [options errors summary arguments]}
        (cli/parse-opts args global-options :in-order true)]
    (cond
      errors
      {:exit 1
       :out (string/join \newline (concat errors ["" (usage summary)]))}

      (:version options)
      {:exit 0 :out (str "epiphany " version)}

      (:check-services options)
      (let [avail? (services/all-available?)
            report (services/report)]
        {:exit (if avail? 0 1)
         :out report})

      (:help options)
      {:exit 0 :out (usage summary)}

      (empty? arguments)
      {:exit 0 :out (usage summary)}

      :else
      (let [command (first arguments)
            cmd-args (rest arguments)]
        (case command
          "register" (run-register cmd-args)
          "search"   (run-search cmd-args)
          "status"   (run-status cmd-args)
          "show"     (run-show cmd-args)
          "diff"     (run-diff cmd-args)
          "trace"    (run-trace cmd-args)
          "serve"    (run-serve cmd-args)
          {:exit 1
           :out (str "Unknown command: " command "\n\n" (usage summary))})))))

(defn -main [& args]
  (let [{:keys [exit out]} (run args)]
    (println out)
    (System/exit exit)))
