(ns epiphany.domain.status
  "Cross-stage projection status query.

  Reports per-stage counts, failures, retries, checkpoint state, and
  projection lag. A failure is inspectable with resource ID, commit/blob/path
  context, operation version, and error detail. Unavailable sources report
  UNAVAILABLE without inventing state."
  (:require [clojure.string :as str]))

;; ---------------------------------------------------------------------------
;; Stage definitions

(def stages
  "Ordered pipeline stages."
  [:registration :discovery :extraction :indexing :embedding])

;; ---------------------------------------------------------------------------
;; Status record construction

(defn make-stage-status
  "Create a stage status map."
  [stage-name & {:keys [status counts failures retries checkpoint lag]
                 :or {status :unknown counts {} failures [] retries 0 checkpoint nil lag nil}}]
  {:stage/name stage-name
   :stage/status status            ;; :ok :in-progress :error :unavailable :unknown
   :stage/counts counts            ;; {:processed n, :total n, ...}
   :stage/failures failures        ;; [{:error ... :context ... :version ...}]
   :stage/retries retries
   :stage/checkpoint checkpoint    ;; {:projection-name ... :version ... :last-processed ...}
   :stage/lag lag})                ;; projection lag (seconds or nil)

(defn make-failure-record
  "Create a failure record."
  [error & {:keys [resource-id commit-id blob-id path-raw version context]
            :or {context ""}}]
  {:failure/error error
   :failure/resource-id resource-id
   :failure/commit-id commit-id
   :failure/blob-id blob-id
   :failure/path-raw path-raw
   :failure/version version
   :failure/context context})

;; ---------------------------------------------------------------------------
;; Status query protocol

(defn query-registration-status
  "Query repository registration status.

   Parameters:
     repo-metadata-adapter — adapter implementing :list-repositories

   Returns a stage status map."
  [repo-metadata-adapter]
  (try
    (let [repos ((:list-repositories repo-metadata-adapter))]
      (make-stage-status :registration
                         :status :ok
                         :counts {:registered (count repos)}))
    (catch clojure.lang.ExceptionInfo e
      (let [data (ex-data e)]
        (if (= :unavailable (:code data))
          (make-stage-status :registration :status :unavailable)
          (make-stage-status :registration
                             :status :error
                             :failures [(make-failure-record (.getMessage e))]))))
    (catch Exception e
      (make-stage-status :registration
                         :status :error
                         :failures [(make-failure-record (.getMessage e))]))))

(defn query-discovery-status
  "Query Git discovery status for a repository.

   Parameters:
     git-adapter — adapter implementing :repository, :resolve-ref
     resource-id — repository resource UUID

   Returns a stage status map."
  [git-adapter resource-id]
  (try
    (let [repo ((:repository git-adapter) resource-id)
          refs ((:resolve-ref git-adapter) repo "HEAD")]
      (make-stage-status :discovery
                         :status :ok
                         :counts {:refs (count refs)}))
    (catch clojure.lang.ExceptionInfo e
      (let [data (ex-data e)]
        (if (= :unavailable (:code data))
          (make-stage-status :discovery :status :unavailable)
          (make-stage-status :discovery
                             :status :error
                             :failures [(make-failure-record (.getMessage e)
                                                            :resource-id resource-id)]))))
    (catch Exception e
      (make-stage-status :discovery
                         :status :error
                         :failures [(make-failure-record (.getMessage e)
                                                        :resource-id resource-id)]))))

(defn query-extraction-status
  "Query extraction projection status.

   Parameters:
     obs-adapter — adapter implementing :list-checkpoints
     resource-id — repository resource UUID

   Returns a stage status map."
  [obs-adapter resource-id]
  (try
    (let [checkpoints ((:list-checkpoints obs-adapter) resource-id "extraction")]
      (if (seq checkpoints)
        (let [ckpt (first checkpoints)]
          (make-stage-status :extraction
                             :status (if (= :completed (:checkpoint/status ckpt)) :ok :in-progress)
                             :counts {:processed (:checkpoint/processed-count ckpt)}
                             :checkpoint {:projection-name (:checkpoint/projection-name ckpt)
                                         :version (:checkpoint/projection-version ckpt)
                                         :last-processed (:checkpoint/last-processed-oid ckpt)}
                             :lag (when-let [ts (:checkpoint/last-updated-at ckpt)]
                                    (long (/ (- (System/currentTimeMillis) (.getTime ts)) 1000)))))
        (make-stage-status :extraction :status :unknown)))
    (catch clojure.lang.ExceptionInfo e
      (let [data (ex-data e)]
        (if (= :unavailable (:code data))
          (make-stage-status :extraction :status :unavailable)
          (make-stage-status :extraction
                             :status :error
                             :failures [(make-failure-record (.getMessage e)
                                                            :resource-id resource-id)]))))
    (catch Exception e
      (make-stage-status :extraction
                         :status :error
                         :failures [(make-failure-record (.getMessage e)
                                                        :resource-id resource-id)]))))

(defn query-indexing-status
  "Query Lucene indexing status.

   Parameters:
     index-adapter — adapter implementing :index-stats
     resource-id — repository resource UUID

   Returns a stage status map."
  [index-adapter resource-id]
  (try
    (let [stats ((:index-stats index-adapter) resource-id)]
      (make-stage-status :indexing
                         :status :ok
                         :counts {:documents (:document-count stats 0)
                                  :terms (:term-count stats 0)}))
    (catch clojure.lang.ExceptionInfo e
      (let [data (ex-data e)]
        (if (= :unavailable (:code data))
          (make-stage-status :indexing :status :unavailable)
          (make-stage-status :indexing
                             :status :error
                             :failures [(make-failure-record (.getMessage e)
                                                            :resource-id resource-id)]))))
    (catch Exception e
      (make-stage-status :indexing
                         :status :error
                         :failures [(make-failure-record (.getMessage e)
                                                        :resource-id resource-id)]))))

(defn query-embedding-status
  "Query embedding projection status.

   Parameters:
     obs-adapter — adapter implementing :list-checkpoints
     resource-id — repository resource UUID

   Returns a stage status map."
  [obs-adapter resource-id]
  (try
    (let [checkpoints ((:list-checkpoints obs-adapter) resource-id "embedding")]
      (if (seq checkpoints)
        (let [ckpt (first checkpoints)]
          (make-stage-status :embedding
                             :status (if (= :completed (:checkpoint/status ckpt)) :ok :in-progress)
                             :counts {:processed (:checkpoint/processed-count ckpt)}
                             :checkpoint {:projection-name (:checkpoint/projection-name ckpt)
                                         :version (:checkpoint/projection-version ckpt)
                                         :last-processed (:checkpoint/last-processed-oid ckpt)}
                             :lag (when-let [ts (:checkpoint/last-updated-at ckpt)]
                                    (long (/ (- (System/currentTimeMillis) (.getTime ts)) 1000)))))
        (make-stage-status :embedding :status :unknown)))
    (catch clojure.lang.ExceptionInfo e
      (let [data (ex-data e)]
        (if (= :unavailable (:code data))
          (make-stage-status :embedding :status :unavailable)
          (make-stage-status :embedding
                             :status :error
                             :failures [(make-failure-record (.getMessage e)
                                                            :resource-id resource-id)]))))
    (catch Exception e
      (make-stage-status :embedding
                         :status :error
                         :failures [(make-failure-record (.getMessage e)
                                                        :resource-id resource-id)]))))

;; ---------------------------------------------------------------------------
;; Aggregate status

(defn query-status
  "Query cross-stage status for a repository.

   Parameters:
     adapters — map of adapter keywords to adapter instances
       {:repo-metadata adapter
        :git adapter
        :observations adapter
        :index adapter}
     resource-id — repository resource UUID

   Returns a map:
     {:resource-id UUID
      :stages [stage-status-map]
      :summary {:ok int :error int :unavailable int :unknown int}}"
  [adapters resource-id]
  (let [stages (vec
                 [(query-registration-status (:repo-metadata adapters))
                  (query-discovery-status (:git adapters) resource-id)
                  (query-extraction-status (:observations adapters) resource-id)
                  (query-indexing-status (:index adapters) resource-id)
                  (query-embedding-status (:observations adapters) resource-id)])
        summary (frequencies (map :stage/status stages))]
    {:resource-id resource-id
     :stages stages
     :summary {:ok (:ok summary 0)
               :error (:error summary 0)
               :unavailable (:unavailable summary 0)
               :unknown (:unknown summary 0)}}))

;; ---------------------------------------------------------------------------
;; Formatting

(defn format-status
  "Format a status result for CLI display."
  [status]
  (let [{:keys [resource-id stages summary]} status
        lines [(str "Resource: " resource-id)
               (str "Summary: " (:ok summary 0) " ok, "
                    (:error summary 0) " error, "
                    (:unavailable summary 0) " unavailable, "
                    (:unknown summary 0) " unknown")
               ""]]
    (str (str/join \newline lines)
         (str/join \newline
                   (for [stage stages]
                     (str "  " (name (:stage/name stage))
                          " [" (name (:stage/status stage)) "]"
                          (when (seq (:stage/counts stage))
                            (str " " (pr-str (:stage/counts stage))))
                          (when (:stage/checkpoint stage)
                            (str " checkpoint=" (:projection-name (:stage/checkpoint stage))
                                 " v" (:version (:stage/checkpoint stage))))
                          (when (:stage/lag stage)
                            (str " lag=" (:stage/lag stage) "s"))
                          (when (seq (:stage/failures stage))
                            (str "\n    failures: "
                                 (str/join ", "
                                           (map :failure/error (:stage/failures stage)))))))))))
