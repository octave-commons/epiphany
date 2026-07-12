(ns epiphany.infra.main
  "Single executable entry point for `epiphany` (short alias: `ep`).
  Dispatches subcommands and wires profile/adapter resolution."
  (:require [clojure.java.shell :as shell]
            [clojure.string :as string]
            [clojure.tools.cli :as cli]
            [epiphany.infra.services :as services]
            [epiphany.infra.profile :as profile]
            [epiphany.infra.adapters.in-memory :as in-memory]
            [epiphany.infra.adapters.mongo :as mongo]
            [epiphany.application.registration :as registration])
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
    "  status      Show ingestion run status for a resource"
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
          "status"   (run-status cmd-args)
          {:exit 1
           :out (str "Unknown command: " command "\n\n" (usage summary))})))))

(defn -main [& args]
  (let [{:keys [exit out]} (run args)]
    (println out)
    (System/exit exit)))
