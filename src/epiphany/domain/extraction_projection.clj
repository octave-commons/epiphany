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
  (:require [clojure.string :as str]
            [epiphany.domain.resource-extraction :as resource]
            [epiphany.shape.markdown :as md]
            [epiphany.domain.section-extraction :as se]
            [epiphany.domain.resource-selection :as selection]
            [epiphany.domain.observation-admission :as admission]
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
        completed (filter #(and (= projection-name
                                   (:checkpoint/projection-name %))
                                (= :completed (:checkpoint/status %)))
                          checkpoints)
        latest (last (sort-by :checkpoint/processed-count completed))]
    (when latest
      {:last-processed-count (:checkpoint/processed-count latest)
       :last-processed-oid   (:checkpoint/last-processed-oid latest)})))

;; ---------------------------------------------------------------------------
;; Single-revision extraction

(defn- failure [reason error revision-id]
  {:failure/reason reason
   :failure/message (ex-message error)
   :failure/revision-id revision-id})

(defn extract-revision
  "Extract sections from a single revision-at-path record.

   Returns a map:
     {:extraction/record     — newly accepted record; nil on duplicate or pre-admission failure
      :extraction/stored?     — acknowledged new write; legacy nil retains its old acknowledgement
      :extraction/error      — error map if extraction failed
      :extraction/revision-id — the revision-at-path ID (for logging)}"
  ([ports revision]
   (extract-revision ports nil revision))
  ([ports request-id
    {:keys [resource-id revision-at-path/id revision/commit-oid
            revision/path-raw revision/blob-oid] :as revision}]
   (let [git-fn   (get-in ports [:git :read-blob])
         obs-fn   (:record-section-extraction! (:observations ports))
         idx-fn   (:index-sections! (:index ports))
         write-id (ingestion/derived-request-id
                   request-id
                   (str "section-extraction:" id ":" (if (str/ends-with? (str/lower-case path-raw) ".md") extractor-version "typed-resource-v1")))]
     (try
       (let [blob-result (git-fn nil blob-oid)
             _          (when (:blob/failure blob-result)
                          (throw (ex-info "Blob not readable"
                                          {:code :blob-unreadable
                                           :blob-oid blob-oid
                                           :failure (:blob/failure blob-result)})))
             blob       (:blob/content blob-result)
             markdown? (str/ends-with? (str/lower-case path-raw) ".md")
             representation (when-not markdown?
                              (if-let [parse-resource (:parse-resource ports)]
                                (parse-resource path-raw blob-result)
                                (throw (ex-info "No structured resource parser configured" {:code :unsupported}))))
             record (if markdown?
                      (se/make-extraction-record
                       (se/extract-sections (md/parse blob))
                       id commit-oid path-raw blob-oid blob extractor-version)
                      (resource/make-record representation revision "typed-resource-v1"))
             observation (cond-> (assoc record
                                        :observation/type :section/extraction-completed
                                        :observation/id (or write-id
                                                            (java.util.UUID/randomUUID))
                                        :observation/observed-at (java.util.Date.)
                                        :observation/adapter-version "0.1.0"
                                        :observation/schema-version 1
                                        :resource-id resource-id)
                           write-id
                           (assoc :observation/request-id write-id))
             stored? (admission/newly-stored? (obs-fn observation))
             index-error (if (and representation (not= :parsed (:resource/status representation)))
                           {:failure/reason (str "resource-" (name (:resource/status representation)))
                            :failure/revision-id id}
                           (when (and stored? idx-fn)
                             (try
                               (idx-fn (assoc observation :extraction/content blob))
                               nil
                               (catch Exception error
                                 (failure "index-failed" error id)))))]
         {:extraction/record     (when stored? observation)
          :extraction/stored?    stored?
          :extraction/error      index-error
          :extraction/revision-id id})
       (catch Exception e
         {:extraction/record     nil
          :extraction/stored?    false
          :extraction/error      (failure "extraction-failed" e id)
          :extraction/revision-id id})))))

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
  [ports {:keys [resource-id ingestion-run-id request-id selection selected-commit-oids]}]
  (let [observations (:observations ports)
        ;; List all revisions for this resource
        all-revisions (selection/filter-revisions
                       ((:list-revision-at-path-by-resource observations) resource-id) selection selected-commit-oids)
        ;; List all existing extractions
        all-extractions (mapcat #((:list-section-extractions-by-revision observations) %)
                                (map :revision-at-path/id all-revisions))
        prior-failures (into [] (keep (fn [record]
                                        (when-let [representation (:extraction/resource record)]
                                          (when (not= :parsed (:resource/status representation))
                                            {:failure/reason (str "resource-" (name (:resource/status representation)))
                                             :failure/revision-id (:extraction/revision-at-path-id record)}))))
                             all-extractions)
        extracted-ids (set (map :extraction/revision-at-path-id all-extractions))
        ;; Filter to un-extracted revisions
        un-extracted (filterv #(and (not= :delete (:revision/evidence %))
                                    (not (extracted-ids (:revision-at-path/id %))))
                              all-revisions)
        ;; Find resume point
        resume-point (find-resume-point observations ingestion-run-id)
        ;; Admission identities, not a count offset, decide what is safe to resume.
        to-process un-extracted]
    (loop [remaining to-process
           processed 0
           extracted 0
           checkpoints 0
           failures prior-failures]
      (if (empty? remaining)
        (let [;; Record final checkpoint
              _ (when (pos? processed)
                  (let [processed-count (+ (or (:last-processed-count resume-point) 0)
                                           processed)
                        checkpoint (ingestion/make-checkpoint-record
                                    {:resource-id         resource-id
                                     :projection-name     projection-name
                                     :projection-version  projection-version
                                     :ingestion-run-id    ingestion-run-id
                                     :status              :completed
                                     :processed-count     processed-count
                                     :last-processed-oid  (:revision/commit-oid (last to-process))
                                     :request-id          (ingestion/derived-request-id
                                                           request-id
                                                           (str "checkpoint:section-extraction:completed:"
                                                                processed-count))})]
                    ((:record-checkpoint! observations) checkpoint)
                    (inc checkpoints)))]
          {:projection/revisions-scanned   (count all-revisions)
           :projection/sections-extracted   extracted
           :projection/checkpoints-recorded checkpoints
           :projection/failures            failures
           :projection/completed           true})
        (let [revision (first remaining)
              result (extract-revision ports request-id revision)
              error (:extraction/error result)
              new-failures (if error (conj failures error) failures)
              new-extracted (if (:extraction/stored? result) (inc extracted) extracted)
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
                               :last-processed-oid  (:revision/commit-oid revision)
                               :request-id          (ingestion/derived-request-id
                                                     request-id
                                                     (str "checkpoint:section-extraction:running:"
                                                          (+ (or (:last-processed-count resume-point) 0)
                                                             new-processed)))})]
              ((:record-checkpoint! observations) checkpoint)))
          (recur (rest remaining)
                 new-processed
                 new-extracted
                 (if should-checkpoint? (inc checkpoints) checkpoints)
                 new-failures))))))
