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
            [epiphany.infra.adapters.lucene :as lucene]
            [epiphany.infra.adapters.ollama :as ollama]
            [epiphany.infra.repository-identity :as repository-identity]
            [epiphany.infra.repository-metadata-file :as repository-metadata-file]
            [epiphany.application.registration :as registration]
            [epiphany.domain.hybrid-search :as hs]
            [epiphany.domain.ingestion :as ingestion]
            [epiphany.domain.extraction-projection :as extraction]
            [epiphany.domain.revision-at-path :as revision-at-path]
            [epiphany.domain.markdown-selection :as markdown-selection]
            [epiphany.domain.evidence :as evidence]
            [epiphany.domain.diff :as diff]
            [epiphany.domain.lineage-trace :as lineage-trace]
            [epiphany.domain.candidates :as candidates]
            [epiphany.domain.inbox :as inbox]
            [epiphany.domain.review :as review]
            [epiphany.domain.export :as export])
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
                                                        :write (fn [_ _id] nil)
                                                        :list-repositories (fn [] [])}
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
;; Shared Git evidence helpers (show / diff / trace / ingest / search)

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

;; ---------------------------------------------------------------------------
;; Search subcommand

(def default-index-dir
  "Default on-disk Lucene index directory for the local corpus.
   The index is a rebuildable projection (ADR-000); it never lives inside
   a repository's Git dir (ADR-001)."
  (str (System/getProperty "user.home") "/.epiphany/index"))

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
    :validate [#(and (pos? %) (<= % http/max-search-limit))
               (str "Must be a positive integer <= " http/max-search-limit)]]
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
    [nil "--index-dir DIR" "Lucene index directory (durable, rebuildable)"
     :id :index-dir
     :default default-index-dir]
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

(defn- ollama-reachable?
  "TCP probe for the local Ollama service (explicit, never silent)."
  []
  (try
    (with-open [sock (java.net.Socket.)]
      (.connect sock (java.net.InetSocketAddress. "127.0.0.1" 11434) 2000)
      true)
    (catch Exception _ false)))

(defn- make-durable-index-ports
  "Construct the :index (on-disk Lucene) and :embeddings (Ollama HTTP)
   ports shared by search, ingest, and serve. The Lucene index is
   local-first and durable; Ollama is only contacted by semantic/hybrid
   queries and explicit --embed ingestion."
  [index-dir]
  {:index      (lucene/make-index-adapter
                {:index-dir (java.nio.file.Paths/get index-dir (into-array String []))})
   :embeddings (ollama/make-embeddings-adapter {})})

(defn- require-ollama!
  "Throw UNAVAILABLE when a semantic/hybrid operation needs Ollama and the
   local service is unreachable. Never falls back to another mode."
  [operation]
  (when-not (ollama-reachable?)
    (throw (ex-info (str operation " requires the local Ollama service.")
                    {:code :unavailable
                     :hint "Start Ollama on localhost:11434, or use --mode lexical."}))))

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
          (when (#{:semantic :hybrid} (:mode options))
            (require-ollama! "Semantic/hybrid search"))
          (let [ports (make-durable-index-ports (:index-dir options))
                results (hs/search ports request)
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
;; Ingest subcommand

(def ingest-options
  [["-p" "--profile PROFILE" "Profile for durable observations: :local (in-memory, one-shot) or :services (MongoDB, incremental)"
    :id :profile
    :default :services
    :parse-fn keyword]
   [nil "--refs REFS" "Comma-separated Git refs to ingest"
    :id :refs
    :default "HEAD"]
   [nil "--index-dir DIR" "Lucene index directory (durable, rebuildable)"
    :id :index-dir
    :default default-index-dir]
   [nil "--embed" "Also embed extracted sections via Ollama and index KNN vectors"
    :id :embed
    :default false]
   ["-h" "--help" "Show ingest help and exit."
    :id :help]])

(defn- make-ingest-adapters
  "Build the registration/observation adapter map for `profile`, plus the
   durable index ports. For :services the repository-metadata port is the
   real Git-local repository.edn file (ADR-001), so resource-ids are stable
   across runs. Hands the map to `f`, cleaning up any Mongo connection."
  [profile index-dir f]
  (let [index-ports (make-durable-index-ports index-dir)]
    (case profile
      :local
      (f (merge (in-memory/make {:common-git-dir-fn resolve-common-git-dir})
                index-ports))

      :services
      (let [conn (try
                   (mongo/connect!)
                   (catch Exception e
                     (throw (ex-info
                             (str "Cannot connect to MongoDB: " (.getMessage e))
                             {:code :unavailable
                              :hint "Start MongoDB with: docker compose up -d mongodb"}))))]
        (try
          (f (merge {:git {:common-git-directory resolve-common-git-dir}
                     :repository-metadata {:read repository-metadata-file/read!
                                           :write repository-metadata-file/write!
                                           :list-repositories (fn [] [])}
                     :observations (mongo/make-observations-adapter conn)}
                    index-ports))
          (finally
            (mongo/disconnect! conn)))))))

(defn- project-revision-at-path!
  "Project revision-at-path observations for every commit reachable from
   `refs`, skipping identities already recorded for `resource-id`.
   Returns the count of newly recorded observations."
  [observations repository-path resource-id refs]
  (let [existing (into #{}
                       (map revision-at-path/observation-id-key)
                       ((:list-revision-at-path-by-resource observations) resource-id))
        {:keys [commits]} (git/reachable-commits repository-path refs)
        new-observations
        (into []
              (comp
               (mapcat
                (fn [commit]
                  (let [commit-oid (:commit/oid commit)
                        entries (markdown-selection/select-markdown
                                 commit-oid
                                 (:entries (git/commit-tree-entries repository-path commit-oid)))
                        parent-oid (first (:commit/parent-oids commit))
                        parent-entries (when parent-oid
                                         (:entries (git/commit-tree-entries repository-path parent-oid)))]
                    (revision-at-path/revisions-for-commit
                     entries
                     {:resource-id resource-id
                      :tree-oid (:commit/tree-oid commit)
                      :parent-commit-oid parent-oid
                      :parent-entries parent-entries
                      :observed-at (java.util.Date.)}))))
               (remove #(contains? existing (revision-at-path/observation-id-key %))))
              commits)]
    (doseq [observation new-observations]
      ((:record-revision-at-path! observations) observation))
    (count new-observations)))

(defn- embed-extractions!
  "Embed every recorded section extraction for `resource-id` via the
   :embeddings port and index the resulting vectors. Returns the count of
   embedding records indexed."
  [adapters resource-id]
  (let [observations (:observations adapters)
        revisions ((:list-revision-at-path-by-resource observations) resource-id)
        extractions (into []
                          (mapcat #((:list-section-extractions-by-revision observations)
                                    (:revision-at-path/id %)))
                          revisions)
        embeddings ((:embed-sections! (:embeddings adapters)) extractions)]
    ((:index-embeddings! (:index adapters)) embeddings)
    (count embeddings)))

(defn- run-ingest
  "Execute the ingest subcommand: register, walk commits, project
   revision-at-path observations, extract sections into the durable Lucene
   index, and optionally embed them. Returns {:exit int, :out string}."
  [args]
  (let [{:keys [options errors summary arguments]}
        (cli/parse-opts args ingest-options)
        profile (:profile options)]
    (cond
      errors
      {:exit 1
       :out (string/join \newline (concat errors ["" (str "Usage: ep ingest [options] <path>\n\n" summary)]))}

      (:help options)
      {:exit 0 :out (str "Usage: ep ingest [options] <path>\n\n" summary)}

      (empty? arguments)
      {:exit 1 :out "Error: repository path required.\nUsage: ep ingest [options] <path>"}

      (not (profile/valid-profile? profile))
      {:exit 1 :out (str "Error: invalid profile " (pr-str profile)
                         ". Valid: " (pr-str profile/valid-profiles))}

      :else
      (try
        (let [repository-path (first arguments)
              _ (resolve-common-git-dir repository-path)
              _ (when (:embed options)
                  (require-ollama! "Embedding ingestion"))]
          (make-ingest-adapters
           profile (:index-dir options)
           (fn [adapters]
             (let [{:keys [resource-id]}
                   (registration/register! adapters {:repository-path repository-path})
                   observations (:observations adapters)
                   refs (string/split (:refs options) #",")
                   run-record (ingestion/run-ingestion
                               {:git {:reachable-commits
                                      (fn [path refs'] (git/reachable-commits path refs'))}
                                :observations observations}
                               {:resource-id resource-id
                                :repository-path repository-path
                                :selected-refs refs})
                   revision-count (project-revision-at-path!
                                   observations repository-path resource-id refs)
                   projection (extraction/run-extraction-projection
                               {:git {:read-blob (fn [_ oid] (git/read-blob repository-path oid))}
                                :observations observations
                                :index (:index adapters)}
                               {:resource-id resource-id
                                :ingestion-run-id (:observation/id run-record)
                                :repository-path repository-path})
                   embedded (when (:embed options)
                              (embed-extractions! adapters resource-id))]
               {:exit 0
                :out (string/join
                      \newline
                      (cond-> [(str "Ingested: " repository-path)
                               (str "  Resource ID:         " resource-id)
                               (str "  Commits traversed:   " (:ingestion/commit-count run-record))
                               (str "  Revisions observed:  " revision-count)
                               (str "  Revisions scanned:   " (:projection/revisions-scanned projection))
                               (str "  Sections extracted:  " (:projection/sections-extracted projection))
                               (str "  Extraction failures: " (count (:projection/failures projection)))
                               (str "  Index dir:           " (:index-dir options))]
                        embedded
                        (conj (str "  Embeddings indexed:  " embedded))))}))))
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
          {:exit 1 :out (str "Error: " (.getMessage e))})))))

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
                         (merge (in-memory/make {:common-git-dir-fn
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
                                (make-durable-index-ports default-index-dir))

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
                           (merge {:git {:common-git-directory git-resolve}
                                   :repository-metadata {:read (fn [_] nil)
                                                         :write (fn [_ _id] nil)
                                                         :list-repositories (fn [] [])}
                                   :observations obs-adapter}
                                  (make-durable-index-ports default-index-dir))))]
           (println (str "Epiphany workbench starting on http://localhost:" port))
          (println (str "Profile: " (name profile)))
          (http/start-server! adapters port)
          ;; Block until interrupted
          (.addShutdownHook (Runtime/getRuntime)
                            (Thread. (fn []
                                       (println "\nShutting down..."))))
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
   :reachable-commits (fn [_ refs] (git/reachable-commits repository-path refs))
   :read-commit (fn [_ commit-oid] (git/read-commit repository-path commit-oid))})

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
   [nil "--seed-candidate RELATION" "Seed a provisional lineage candidate from this comparison (relation type)"
    :parse-fn keyword
    :validate [candidates/relation-types
               (str "Must be one of: " (string/join ", " (map name candidates/relation-types)))]]
   ["-p" "--profile PROFILE" "Profile for candidate seeding: :local (in-memory) or :services (MongoDB)"
    :default :local
    :parse-fn keyword
    :validate [profile/valid-profile? (str "Valid: " (pr-str profile/valid-profiles))]]
   ["-h" "--help" "Show diff help and exit."]])

(defn- with-observations-adapter
  "Construct an observations-port adapter for `profile` and hand it to `f`,
   cleaning up any Mongo connection afterward. Mirrors run-register's
   per-profile adapter construction."
  [profile f]
  (case profile
    :local
    (f (:observations (in-memory/make {:common-git-dir-fn resolve-common-git-dir})))

    :services
    (let [conn (try
                 (mongo/connect!)
                 (catch Exception e
                   (throw (ex-info
                           (str "Cannot connect to MongoDB: " (.getMessage e))
                           {:code :unavailable
                            :hint "Start MongoDB with: docker compose up -d mongodb"}))))]
      (try
        (f (mongo/make-observations-adapter conn))
        (finally (mongo/disconnect! conn))))))

(defn- seed-candidate!
  "Seed a provisional lineage candidate relating `left` to `right` (parsed
   section expressions) under `relation`, durably recorded through the
   observations port for `repo`/`profile`. Returns the candidate map (the
   candidate is always PROVISIONAL — it is never auto-accepted; a human
   reviews it via the ENG-005A decision events)."
  [repo profile relation left right]
  (let [{:keys [resource-id]} (repository-identity/resolve-repository repo)
        source-span (candidates/make-span {:path-raw (:path left)
                                            :heading-path (:heading left)
                                            :commit-oid (:commit-oid left)})
        target-span (candidates/make-span {:path-raw (:path right)
                                            :heading-path (:heading right)
                                            :commit-oid (:commit-oid right)})
        candidate (candidates/make-candidate relation source-span target-span
                                             :confidence 1.0
                                             :generator-version "ep-diff-v1")
        observation (candidates/candidate->observation
                     candidate {:resource-id resource-id :adapter-version "0.1.0"})]
    (with-observations-adapter profile
      (fn [obs-adapter] ((:record-lineage-candidate! obs-adapter) observation)))
    candidate))

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
              seeded (when (:seed-candidate options)
                       (seed-candidate! repo (:profile options) (:seed-candidate options) left right))
              output (str (case (:format options)
                            :edn (pr-str result)
                            (str (diff/format-diff-text (:diff/lines result)
                                                        (str "--- " left-expr)
                                                        (str "+++ " right-expr))
                                 "\n\n"
                                 ;; Continuity is display-only context — never folded into the diff above.
                                 "Continuity (not part of the diff): " (pr-str (:diff/continuity result))
                                 "\nSummary: " (pr-str (:diff/summary result))))
                          (when seeded
                            (str "\n\nSeeded candidate " (:lineage-candidate/id seeded)
                                 " [" (name (:lineage-candidate/relation seeded)) ", provisional]"
                                 " — review with the ENG-005A decision events before treating it as established.")))]
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

(defn- section->show-expr
  "Build the `ep show` section expression for a trace node's section,
   so its exact historical evidence is one command away."
  [section]
  (let [heading (:heading-path section)]
    (str (:path-raw section)
         (when (seq heading) (str "#" (string/join ">" heading)))
         "@" (:commit-oid section))))

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
                                            (when (:timestamp s) (str "  (" (:timestamp s) ")"))
                                            "\n     ep show " (section->show-expr s))))
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
;; Inbox subcommand

(defn- split-csv [s] (when (seq s) (remove empty? (string/split s #","))))

(defn- parse-relation-list [s]
  (some->> (split-csv s) (mapv keyword)))

(defn- parse-uuid-list [s]
  (some->> (split-csv s) (mapv #(java.util.UUID/fromString %))))

(defn- parse-confidence-band [s]
  (when (seq s)
    (let [[low high] (string/split s #",")]
      [(when (seq low) (Double/parseDouble low))
       (when (seq high) (Double/parseDouble high))])))

(defn- parse-instant [s]
  (when (seq s) (java.util.Date/from (java.time.Instant/parse s))))

(def inbox-options
  [repo-option
   [nil "--also-repo PATHS" "Additional repository paths (comma-separated) whose candidates join this repo's family view"]
   [nil "--relation RELATIONS" "Comma-separated relation types to include"]
   [nil "--confidence-band LOW,HIGH" "Confidence range, e.g. 0.7,1.0"]
   [nil "--generator VERSIONS" "Comma-separated generator versions to include"]
   [nil "--after INSTANT" "ISO-8601 instant; only candidates generated at/after this time"]
   [nil "--before INSTANT" "ISO-8601 instant; only candidates generated before this time"]
   [nil "--repository-family UUIDS" "Comma-separated resource-id UUIDs narrowing a merged multi-repo view"]
   [nil "--limit N" "Max items" :default 50 :parse-fn #(Integer/parseInt %)]
   [nil "--sort SORT" "confidence (default) or evidence"
    :default :confidence
    :parse-fn keyword
    :validate [#{:confidence :evidence} "Must be confidence or evidence"]]
   [nil "--include-suppressed" "Also show rejected/do-not-suggest candidates"]
   ["-p" "--profile PROFILE" "Profile: :local (in-memory) or :services (MongoDB)"
    :default :local
    :parse-fn keyword
    :validate [profile/valid-profile? (str "Valid: " (pr-str profile/valid-profiles))]]
   ["-f" "--format FORMAT" "Output format: text or edn"
    :default :text
    :parse-fn keyword
    :validate [#{:text :edn} "Must be text or edn"]]
   ["-h" "--help" "Show inbox help and exit."]])

(def inbox-decide-options
  [repo-option
   [nil "--reason TEXT" "Human-readable reason for the decision"]
   [nil "--relabel-to RELATION" "New relation type (for relabel decisions)"
    :parse-fn keyword]
   [nil "--annotation TEXT" "Free-text annotation (for annotated decisions)"]
   ["-p" "--profile PROFILE" "Profile: :local (in-memory) or :services (MongoDB)"
    :default :local
    :parse-fn keyword
    :validate [profile/valid-profile? (str "Valid: " (pr-str profile/valid-profiles))]]
   ["-h" "--help" "Show inbox decide help and exit."]])

(defn- resource-ids-for-repos [repo-paths]
  (mapv #(:resource-id (repository-identity/resolve-repository %)) repo-paths))

(defn- fetch-candidates-and-decisions [resource-ids profile]
  (with-observations-adapter profile
    (fn [obs-adapter]
      (let [list-candidates (:list-lineage-candidates obs-adapter)
            list-decisions (:list-review-decisions obs-adapter)]
        {:candidates (vec (mapcat list-candidates resource-ids))
         :decisions (vec (mapcat list-decisions resource-ids))}))))

(defn- format-inbox-item-text [idx item]
  (let [c (:inbox/candidate item)]
    (str "  " idx ". " (:lineage-candidate/id c)
         " [" (name (:lineage-candidate/relation c)) ", "
         (name (:inbox/decision-status item)) ", confidence="
         (:lineage-candidate/confidence c) "]"
         "\n     " (:inbox/evidence-summary item)
         "\n     source: " (get-in c [:lineage-candidate/source :span/path-raw]
                                    (get-in c [:lineage-candidate/source :section/path-raw]))
         " -> target: " (get-in c [:lineage-candidate/target :span/path-raw]
                                 (get-in c [:lineage-candidate/target :section/path-raw])))))

(defn- format-inbox-text [items]
  (if (empty? items)
    "No unreviewed candidates."
    (str (count items) " item(s):\n\n"
         (string/join "\n\n" (map-indexed format-inbox-item-text items)))))

(defn- run-inbox-list
  [args]
  (let [{:keys [options errors summary]} (cli/parse-opts args inbox-options)]
    (cond
      errors
      {:exit 1
       :out (string/join \newline (concat errors ["" (str "Usage: ep inbox [options]\n\n" summary)]))}

      (:help options)
      {:exit 0 :out (str "Usage: ep inbox [options]\n\n" summary)}

      :else
      (try
        (let [repo-paths (cons (:repo options) (split-csv (:also-repo options)))
              resource-ids (resource-ids-for-repos repo-paths)
              {:keys [candidates decisions]} (fetch-candidates-and-decisions resource-ids (:profile options))
              filters (cond-> {}
                        (:relation options) (assoc :relation-types (parse-relation-list (:relation options)))
                        (:confidence-band options) (assoc :confidence-band (parse-confidence-band (:confidence-band options)))
                        (:generator options) (assoc :generators (split-csv (:generator options)))
                        (or (:after options) (:before options))
                        (assoc :date-range [(parse-instant (:after options)) (parse-instant (:before options))])
                        (:repository-family options) (assoc :repository-families (parse-uuid-list (:repository-family options))))
              items (inbox/build-inbox candidates decisions filters
                                       {:limit (:limit options)
                                        :sort (:sort options)
                                        :include-suppressed? (boolean (:include-suppressed options))})
              output (case (:format options)
                       :edn (pr-str items)
                       (format-inbox-text items))]
          {:exit 0 :out output})
        (catch clojure.lang.ExceptionInfo e (git-boundary-error e))
        (catch Exception e {:exit 1 :out (str "Error: " (.getMessage e))})))))

(defn- run-inbox-decide
  [args]
  (let [{:keys [options errors summary arguments]} (cli/parse-opts args inbox-decide-options)]
    (cond
      errors
      {:exit 1
       :out (string/join \newline (concat errors ["" (str "Usage: ep inbox decide <candidate-id> <decision> [options]\n\n" summary)]))}

      (:help options)
      {:exit 0 :out (str "Usage: ep inbox decide <candidate-id> <decision> [options]\n\n" summary)}

      (not= 2 (count arguments))
      {:exit 1 :out "Error: candidate id and decision required.\nUsage: ep inbox decide <candidate-id> <decision> [options]"}

      (not (contains? review/review-decision-types (keyword (second arguments))))
      {:exit 1 :out (str "Error: decision must be one of "
                         (string/join ", " (map name review/review-decision-types)))}

      :else
      (let [[candidate-id-str decision-str] arguments]
        (try
          (let [candidate-id (java.util.UUID/fromString candidate-id-str)
                decision-type (keyword decision-str)
                {:keys [resource-id]} (repository-identity/resolve-repository (:repo options))
                decision (review/make-decision candidate-id decision-type
                                               :reason (:reason options)
                                               :relabel-to (:relabel-to options)
                                               :annotation (:annotation options))
                observation (review/decision->observation
                             decision {:resource-id resource-id :adapter-version "0.1.0"})]
            (with-observations-adapter (:profile options)
              (fn [obs-adapter] ((:record-review-decision! obs-adapter) observation)))
            {:exit 0 :out (str "Recorded " decision-str " for candidate " candidate-id-str ".")})
          (catch IllegalArgumentException _e
            {:exit 1 :out (str "Error: invalid candidate id " (pr-str candidate-id-str))})
          (catch clojure.lang.ExceptionInfo e (git-boundary-error e))
          (catch Exception e {:exit 1 :out (str "Error: " (.getMessage e))}))))))

(defn- run-inbox
  "Execute the inbox subcommand. `ep inbox [options]` lists the review
   queue; `ep inbox decide <candidate-id> <decision> [options]` records a
   decision in one action, per the AC's keyboard-efficient-triage bullet."
  [args]
  (if (= "decide" (first args))
    (run-inbox-decide (rest args))
    (run-inbox-list args)))

;; ---------------------------------------------------------------------------
;; Export subcommand

(def export-options
  [repo-option
   [nil "--also-repo PATHS" "Additional repository paths (comma-separated) to include candidates/decisions from"]
   [nil "--label TEXT" "Human-readable packet label" :default "Evidence Packet"]
   ["-p" "--profile PROFILE" "Profile: :local (in-memory) or :services (MongoDB)"
    :default :local
    :parse-fn keyword
    :validate [profile/valid-profile? (str "Valid: " (pr-str profile/valid-profiles))]]
   ["-f" "--format FORMAT" "Output format: markdown, edn, or json"
    :default :markdown
    :parse-fn keyword
    :validate [#{:markdown :edn :json} "Must be markdown, edn, or json"]]
   [nil "--out FILE" "Write the packet to FILE instead of stdout"]
   ["-h" "--help" "Show export help and exit."]])

(defn- run-export
  "Execute the export subcommand. Returns {:exit int, :out string}."
  [args]
  (let [{:keys [options errors summary]} (cli/parse-opts args export-options)]
    (cond
      errors
      {:exit 1
       :out (string/join \newline (concat errors ["" (str "Usage: ep export [options]\n\n" summary)]))}

      (:help options)
      {:exit 0 :out (str "Usage: ep export [options]\n\n" summary)}

      :else
      (try
        (let [repo-paths (cons (:repo options) (split-csv (:also-repo options)))
              resource-ids (resource-ids-for-repos repo-paths)
              {:keys [candidates decisions]} (fetch-candidates-and-decisions resource-ids (:profile options))
              packet (-> (export/make-packet :resource-id (first resource-ids)
                                             :label (:label options)
                                             :generator-version "ep-export-v1")
                         (export/populate-from-lineage-candidates candidates)
                         (export/populate-from-review-decisions decisions)
                         (export/add-content-hash))
              output (case (:format options)
                       :edn (export/packet->edn packet)
                       :json (export/packet->json packet)
                       (export/packet->markdown packet))]
          (if (:out options)
            (do (spit (:out options) output)
                {:exit 0 :out (str "Wrote " (:out options) ".")})
            {:exit 0 :out output}))
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
    "  ingest      Ingest a repository: observe revisions, extract sections, index into Lucene"
    "  search      Search sections by query (lexical, semantic, or hybrid)"
    "  status      Show ingestion run status for a resource"
    "  show        Open exact historical evidence for a section expression"
    "  diff        Compare two historical section expressions"
    "  trace       Trace a section's Git-history lineage chronology"
    "  inbox       Review the lineage-candidate queue; 'inbox decide' records a decision"
    "  export      Export an evidence packet (Markdown/EDN/JSON) of candidates and decisions"
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
          "ingest"   (run-ingest cmd-args)
          "search"   (run-search cmd-args)
          "status"   (run-status cmd-args)
          "show"     (run-show cmd-args)
          "diff"     (run-diff cmd-args)
          "trace"    (run-trace cmd-args)
          "inbox"    (run-inbox cmd-args)
          "export"   (run-export cmd-args)
          "serve"    (run-serve cmd-args)
          {:exit 1
           :out (str "Unknown command: " command "\n\n" (usage summary))})))))

(defn -main [& args]
  (let [{:keys [exit out]} (run args)]
    (println out)
    (System/exit exit)))
