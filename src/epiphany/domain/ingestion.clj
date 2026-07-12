(ns epiphany.domain.ingestion
  "Ingestion run orchestration.

   Coordinates commit graph traversal, revision-at-path observation
   construction, and checkpoint persistence. Pure functions where
   possible; I/O is delegated to ports.

   An ingestion run is a durable record of one traversal pass over a
   repository's commit graph. Each run records which refs were selected,
   how many commits were encountered, and which objects failed.
   Projection checkpoints record progress within a run, enabling
   deterministic resumption.")

;; ---------------------------------------------------------------------------
;; Ingestion run construction

(defn make-run-id
  "Generate a new ingestion run UUID."
  []
  (java.util.UUID/randomUUID))

(defn make-run-record
  "Construct an ingestion-run observation map.

   Options:
     :resource-id       — the repository's resource-id (UUID)
     :repo-path         — observed path map {:path/raw :path/source :path/comparison}
     :selected-refs     — vector of ref name keywords
     :commit-count      — total commits traversed
     :failure-count     — total failures
     :failures          — vector of failure maps
     :observed-at       — timestamp (default: now)
     :adapter-version   — adapter version string
     :schema-version    — schema version int (default: 1)
     :request-id        — optional request UUID"
  [{:keys [resource-id repo-path selected-refs commit-count failure-count
           failures observed-at adapter-version schema-version request-id]}]
  (cond-> {:observation/type           :ingestion/run-completed
           :observation/id             (make-run-id)
           :observation/observed-at    (or observed-at (java.util.Date.))
           :observation/adapter-version (or adapter-version "0.1.0")
           :observation/schema-version (or schema-version 1)
           :resource-id                resource-id
           :ingestion/repo-path        repo-path
           :ingestion/selected-refs    selected-refs
           :ingestion/commit-count     commit-count
           :ingestion/failure-count    failure-count
           :ingestion/failures         failures}
    request-id
    (assoc :observation/request-id request-id)))

;; ---------------------------------------------------------------------------
;; Checkpoint construction

(def checkpoint-version
  "Current version of the revision-at-path projection logic.
   Bump when projection logic changes to force reprocessing."
  1)

(defn make-checkpoint-record
  "Construct a projection-checkpoint observation map.

   Options:
     :resource-id            — the repository's resource-id (UUID)
     :projection-name        — name of the projection (e.g., \"revision-at-path\")
     :projection-version     — version of the projection logic
     :ingestion-run-id       — the run this checkpoint belongs to (UUID)
     :status                 — :running, :completed, or :failed
     :last-processed-oid     — optional last processed commit OID string
     :processed-count        — number of items processed so far
     :error-message          — optional error message on failure
     :observed-at            — timestamp (default: now)
     :adapter-version        — adapter version string
     :schema-version         — schema version int (default: 1)
     :request-id             — optional request UUID"
  [{:keys [resource-id projection-name projection-version ingestion-run-id
           status last-processed-oid processed-count error-message
           observed-at adapter-version schema-version request-id]}]
  (cond-> {:observation/type           :projection/checkpoint-recorded
           :observation/id             (java.util.UUID/randomUUID)
           :observation/observed-at    (or observed-at (java.util.Date.))
           :observation/adapter-version (or adapter-version "0.1.0")
           :observation/schema-version (or schema-version 1)
           :resource-id                resource-id
           :checkpoint/projection-name  projection-name
           :checkpoint/projection-version (or projection-version checkpoint-version)
           :checkpoint/ingestion-run-id ingestion-run-id
           :checkpoint/status           status
           :checkpoint/processed-count  processed-count}
    last-processed-oid
    (assoc :checkpoint/last-processed-oid last-processed-oid)
    error-message
    (assoc :checkpoint/error-message error-message)
    request-id
    (assoc :observation/request-id request-id)))

;; ---------------------------------------------------------------------------
;; Ingestion run execution

(defn run-ingestion
  "Execute an ingestion run over the selected refs of a repository.

   This is a pure orchestration function that:
   1. Walks the commit graph via the git port
   2. Records an ingestion-run observation
   3. Records a checkpoint at completion

   The function delegates all I/O to the provided ports:
     :git            — commit graph walking
     :observations   — recording run and checkpoint observations

   Returns the completed ingestion-run observation map.

   Note: this function does NOT yet construct revision-at-path
   observations — that is a separate projection step (ENG-001F).
   This run records the traversal metadata only."
  [{:keys [git observations]} {:keys [resource-id repository-path selected-refs]
                                :as _command}]
  (let [run-id (make-run-id)
        ;; Walk the commit graph
        walk-result ((:reachable-commits git) repository-path selected-refs)
        {:keys [commits failures commit-count failure-count]} walk-result

        ;; Construct the run record
        run-record (make-run-record
                    {:resource-id    resource-id
                     :repo-path      {:path/raw       repository-path
                                      :path/source    :filesystem-argument
                                      :path/comparison :exact}
                     :selected-refs  selected-refs
                     :commit-count   commit-count
                     :failure-count  failure-count
                     :failures       failures
                     :request-id     run-id})]

    ;; Record the ingestion run
    ((:record-ingestion-run! observations) run-record)

    ;; Record completion checkpoint
    (let [last-oid (when (seq commits)
                     (:commit/oid (last commits)))
          checkpoint (make-checkpoint-record
                      {:resource-id         resource-id
                       :projection-name     "ingestion-traversal"
                       :projection-version  checkpoint-version
                       :ingestion-run-id    run-id
                       :status              :completed
                       :last-processed-oid  last-oid
                       :processed-count     commit-count})]
      ((:record-checkpoint! observations) checkpoint))

    run-record))
