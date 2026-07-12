(ns epiphany.law.ports
  "Malli contracts for storage port interfaces.

  A port is a map of named functions that the application layer calls to
  interact with infrastructure. Each port group is a map of keyword ->
  function. Adapters (infra/adapters/*) implement these port shapes;
  the profile system (infra/profile) selects and constructs them.

  Port functions are plain Clojure functions — no protocols, no records.
  This keeps the dependency graph flat and the adapter boundary obvious.

  The schemas here validate the *data* that flows through ports (command
  maps, observation records, configuration). Function arity and return
  contracts are documented in docstrings and enforced by tests, not by
  Malli at runtime.")

;; ---------------------------------------------------------------------------
;; Git port
;;
;; Reads Git object data without side effects. The single function resolves
;; a repository path to its common Git directory (the real .git location,
;; whether worktree, bare, or linked).

(def git-port-schema
  "Schema for the :git port group.
   Functions are documented here; validated by tests at runtime."
  [:map {:closed true}
   [:common-git-directory :any]])

;; ---------------------------------------------------------------------------
;; Repository metadata port
;;
;; Reads and writes the Git-local repository.edn file that carries the
;; resource-id. This port is filesystem-local: reads and writes target the
;; common Git directory resolved by the :git port.

(def repository-metadata-port-schema
  "Schema for the :repository-metadata port group."
  [:map {:closed true}
   [:read  :any]
   [:write :any]])

;; ---------------------------------------------------------------------------
;; Observations port
;;
;; Append-only durable observation store. Records are never updated or
;; deleted; corrections append new observations.
;;
;; The observations port distinguishes:
;;   - :find-by-request-id — idempotent lookup (returns nil when absent)
;;   - :record-repository-location! — append a location observation
;;   - :record-ingestion-run! — append an ingestion run record
;;   - :record-checkpoint! — append a projection checkpoint record
;;   - :record-section-extraction! — append a section extraction record
;;   - :list-ingestion-runs — list all ingestion runs for a resource
;;   - :list-checkpoints — list all checkpoints for a run

(def observations-port-schema
  "Schema for the :observations port group."
  [:map {:closed true}
   [:find-by-request-id          :any]
   [:record-repository-location! :any]
   [:record-ingestion-run!       :any]
   [:record-checkpoint!          :any]
   [:record-section-extraction!  :any]
   [:list-ingestion-runs         :any]
   [:list-checkpoints            :any]])

;; ---------------------------------------------------------------------------
;; Index port
;;
;; Lexical search index + KNN vector search. Rebuildable from stored
;; records; versioned so schema changes are detectable.
;;
;; Port functions:
;;   - :index-sections! — add section extraction records to the index
;;   - :search — lexical query, return ranked results
;;   - :index-embeddings! — add embedding records with KNN vector fields
;;   - :knn-search — vector nearest-neighbor query, return ranked results
;;   - :index-version — current schema version of the index
;;   - :rebuild-index! — drop and rebuild from stored records
;;   - :clear-index! — drop all index data

(def index-port-schema
  "Schema for the :index port group."
  [:map {:closed true}
   [:index-sections!  :any]
   [:search           :any]
   [:index-embeddings! :any]
   [:knn-search       :any]
   [:index-version    :any]
   [:rebuild-index!   :any]
   [:clear-index!     :any]])

;; ---------------------------------------------------------------------------
;; Embeddings port
;;
;; Dense vector embeddings via local LLM (Ollama). Batch-embed sections,
;; single-embed queries, track model version, and clear/rebuild.
;;
;; Port functions:
;;   - :embed-sections! — batch-embed section extraction records, return embedding records
;;   - :embed-query — single text -> vector for search
;;   - :embedding-version — current model+config version
;;   - :clear-embeddings! — drop all embedding data

(def embeddings-port-schema
  "Schema for the :embeddings port group."
  [:map {:closed true}
   [:embed-sections!   :any]
   [:embed-query       :any]
   [:embedding-version :any]
   [:clear-embeddings! :any]])

;; ---------------------------------------------------------------------------
;; Composite application ports
;;
;; The full port map that the application layer receives. Every function
;; in this map is injected by the profile/adapter layer; the application
;; never constructs adapters directly.

(def application-ports-schema
  "Schema for the complete port map injected into application services."
  [:map {:closed true}
   [:git                 git-port-schema]
   [:repository-metadata repository-metadata-port-schema]
   [:observations        observations-port-schema]
   [:index               index-port-schema]
   [:embeddings          embeddings-port-schema]])
