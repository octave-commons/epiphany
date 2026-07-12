(ns epiphany.domain.extraction-projection
  "Checkpointed projection runner for section extraction.

  Drives section extraction over revision-at-path observations,
  with checkpoint support for resumability and per-item error handling.

  Pure orchestration where possible; I/O delegated to ports:
    :git            — read-blob for blob content
    :observations   — list revisions, list extractions, record extractions + checkpoints
    :index          — index-sections! for lexical index

  The projection name is \"section-extraction\" and the version is
  tracked in `projection-version` — bump when extraction logic changes
  to force reprocessing from the last checkpoint."
  (:require [epiphany.shape.markdown :as md]
            [epiphany.domain.section-extraction :as se]
            [epiphany.domain.ingestion :as ingestion]))

;; ---------------------------------------------------------------------------
;; Configuration

(def projection-name
  "The name of this projection, used in checkpoint records."
  "section-extraction")

(def projection-version
  "Version of the extraction projection logic.
   Bump when extraction logic changes to force reprocessing."
  1)

(def extractor-version
  "Version string recorded on every extraction record."
  "extraction-v1")

(def ^:dynamic checkpoint-interval
  "Number of items processed between checkpoints."
  10)

;; ---------------------------------------------------------------------------
;; Checkpoint resumption

(defn find-resume-point
  "Find the last processed revision-at-path ID from checkpoint history.

   Returns nil if no checkpoint exists (start from scratch).
   Otherwise returns the last-processed revision-at-path UUID."
  [observations ingestion-run-id]
  (let [checkpoints ((:list-checkpoints observations) ingestion-run-id)
        completed (filter #(= :completed (:checkpoint/status %)) checkpoints)
        latest (last (sort-by :checkpoint/processed-count completed))]
    (when latest
      {:last-processed-count (:checkpoint/processed-count latest)
       :last-processed-oid   (:checkpoint/last-processed-oid latest)})))

;; ---------------------------------------------------------------------------
;; Single-revision extraction

(defn extract-revision
  "Extract sections from a single revision-at-path record.

   Returns a map:
     {:extraction/record     — the extraction observation (or nil on error)
      :extraction/error      — error map if extraction failed
      :extraction/revision-id — the revision-at-path ID (for logging)}"
  [ports {:keys [resource-id revision-at-path/id revision/commit-oid
                 revision/path-raw revision/blob-oid] :as revision}]
  (let [git-fn   (get-in ports [:git :read-blob])
        obs-fn   (:record-section-extraction! (:observations ports))
        idx-fn   (:index-sections! (:index ports))]
    (try
      (let [blob-result (git-fn nil blob-oid)
            _          (when (:blob/failure blob-result)
                         (throw (ex-info "Blob not readable"
                                         {:code :blob-unreadable
                                          :blob-oid blob-oid
                                          :failure (:blob/failure blob-result)})))
            blob       (:blob/content blob-result)
            parsed     (md/parse blob)
            sections   (se/extract-sections parsed)
            record     (se/make-extraction-record
                        sections id commit-oid path-raw blob-oid blob extractor-version)
            observation (assoc record
                               :observation/type :section/extraction-completed
                               :observation/id (java.util.UUID/randomUUID)
                               :observation/observed-at (java.util.Date.)
                               :observation/adapter-version "0.1.0"
                               :observation/schema-version 1
                               :resource-id resource-id)]
        (obs-fn observation)
        (when idx-fn
          (idx-fn observation))
        {:extraction/record     observation
         :extraction/error      nil
         :extraction/revision-id id})
      (catch Exception e
        {:extraction/record     nil
         :extraction/error      {:failure/reason "extraction-failed"
                                 :failure/message (.getMessage e)
                                 :failure/revision-id id}
         :extraction/revision-id id}))))

;; ---------------------------------------------------------------------------
;; Projection runner

(defn run-extraction-projection
  "Run the section-extraction projection over all un-extracted revisions.

   Parameters:
     ports   — application port map
     command — {:resource-id       UUID
                :ingestion-run-id  UUID
                :repository-path   string}

   Behavior:
     1. Lists all revision-at-path observations for the resource
     2. Lists all existing section-extraction records
     3. Computes un-extracted revisions (idempotent)
     4. Extracts sections from each, recording results + checkpoints
     5. Reports final counts

   Returns a map:
     {:projection/revisions-scanned   int
      :projection/sections-extracted   int
      :projection/checkpoints-recorded int
      :projection/failures            [{:failure/reason string, :failure/message string}]
      :projection/completed           boolean}"
  [ports {:keys [resource-id ingestion-run-id repository-path]}]
  (let [observations (:observations ports)
        ;; List all revisions for this resource
        all-revisions ((:list-revision-at-path-by-resource observations) resource-id)
        ;; List all existing extractions
        all-extractions (mapcat #((:list-section-extractions-by-revision observations) %)
                                (map :revision-at-path/id all-revisions))
        extracted-ids (set (map :extraction/revision-at-path-id all-extractions))
        ;; Filter to un-extracted revisions
        un-extracted (filterv #(not (extracted-ids (:revision-at-path/id %)))
                              all-revisions)
        ;; Find resume point
        resume-point (find-resume-point observations ingestion-run-id)
        ;; Skip already-processed revisions if resuming
        to-process (if resume-point
                     (let [last-count (:last-processed-count resume-point)]
                       (subvec un-extracted (min last-count (count un-extracted))))
                     un-extracted)]
    (loop [remaining to-process
           processed 0
           extracted 0
           checkpoints 0
           failures []]
      (if (empty? remaining)
        (let [;; Record final checkpoint
              _ (when (pos? processed)
                   (let [checkpoint (ingestion/make-checkpoint-record
                                    {:resource-id         resource-id
                                     :projection-name     projection-name
                                     :projection-version  projection-version
                                     :ingestion-run-id    ingestion-run-id
                                     :status              :completed
                                     :processed-count     (+ (or (:last-processed-count resume-point) 0)
                                                             processed)
                                     :last-processed-oid  (:revision/commit-oid (last to-process))})]
                    ((:record-checkpoint! observations) checkpoint)
                    (inc checkpoints)))]
          {:projection/revisions-scanned   (count all-revisions)
           :projection/sections-extracted   extracted
           :projection/checkpoints-recorded checkpoints
           :projection/failures            failures
           :projection/completed           true})
        (let [revision (first remaining)
              result (extract-revision ports revision)
              error (:extraction/error result)
              new-failures (if error (conj failures error) failures)
              new-extracted (if (:extraction/record result) (inc extracted) extracted)
              new-processed (inc processed)
              ;; Checkpoint periodically
              should-checkpoint? (and (pos? new-processed)
                                      (zero? (mod new-processed checkpoint-interval)))]
          ;; Record checkpoint if needed
          (when should-checkpoint?
            (let [checkpoint (epiphany.domain.ingestion/make-checkpoint-record
                              {:resource-id         resource-id
                               :projection-name     projection-name
                               :projection-version  projection-version
                               :ingestion-run-id    ingestion-run-id
                               :status              :running
                               :processed-count     (+ (or (:last-processed-count resume-point) 0)
                                                       new-processed)
                               :last-processed-oid  (:revision/commit-oid revision)})]
              ((:record-checkpoint! observations) checkpoint)))
          (recur (rest remaining)
                 new-processed
                 new-extracted
                 (if should-checkpoint? (inc checkpoints) checkpoints)
                 new-failures))))))
