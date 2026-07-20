(ns epiphany.infra.adapters.in-memory
  "In-memory adapters for :local (direct) mode.

  Every adapter is a plain map of keyword -> function, matching the port
  shapes defined in epiphany.law.ports. State is held in atoms; nothing
  survives process restart. These adapters are for unit testing and the
  direct-mode REPL workflow — never for :services mode.

  The constructor `make` returns a fresh, isolated adapter set. Each
  call produces an independent world with its own atoms.

  The observations adapter is contract-enforcing (ENG-017C): every
  write validates its input against the schema registry and enforces
  idempotency semantics. Rejected writes leave all observable state
  byte-identical to the pre-write state."

  (:require [epiphany.law.operations :as operations]
            [epiphany.law.registry :as registry]))

;; ---------------------------------------------------------------------------
;; Git adapter

(defn- make-git-adapter
  "In-memory Git port. The common-git-directory function is injected at
   construction time — it delegates to the real infra/git implementation
   or a test double."
  [common-git-dir-fn]
  {:common-git-directory common-git-dir-fn})

;; ---------------------------------------------------------------------------
;; Repository metadata adapter

(defn- make-repository-metadata-adapter
  "In-memory repository metadata port. Stores repository.edn data in an
   atom keyed by common-git-dir path."
  []
  (let [store (atom {})]
    {:read  (fn [common-git-dir]
              (get @store common-git-dir))
     :write (fn [common-git-dir resource-id]
              (swap! store assoc common-git-dir {:resource-id resource-id})
              nil)}))

;; ---------------------------------------------------------------------------
;; Contract enforcement helpers (ENG-017C)

(defn- validate-write!
  "Validate a record against the schema registered for `op`.
  Returns nil when valid; throws :schema-validation-failed when invalid."
  [op record]
  (when-let [entry (get operations/operations op)]
    (when-let [schema-name (:input-schema entry)]
      (when-let [explain (registry/explain schema-name record)]
        (throw (ex-info (str "Schema validation failed for " op)
                        {:code :schema-validation-failed
                         :operation op
                         :schema/name schema-name
                         :explanation (mapv #(select-keys % [:path :schema :message])
                                           (:errors explain))}))))))

(defn- idempotent-record-repository-location!
  "Enforce idempotency for repository-location writes.

  Same request-ID replay: returns the existing recorded fact (no mutation).
  Same request-ID with materially different content: returns
  {:code :idempotency-conflict}.
  New request-ID: validates, stores, returns nil."
  [by-request-id observation]
  (let [rid (:observation/request-id observation)]
    (if-not rid
      ;; No request-id — validate and store unconditionally
      (do (validate-write! :record-repository-location! observation)
          (swap! by-request-id assoc (random-uuid) observation)
          nil)
      ;; Has request-id — check for existing
      (if-let [existing (get @by-request-id rid)]
        ;; Existing found — check for content conflict
        (if (= existing observation)
          nil  ;; Idempotent replay — no mutation, no error
          {:code :idempotency-conflict
           :request-id rid
           :message "Same request-id with materially different content"})
        ;; New request-id — validate and store
        (do (validate-write! :record-repository-location! observation)
            (swap! by-request-id assoc rid observation)
            nil)))))

(defn- validated-record-fn
  "Create a validating write function for non-idempotent record ops.
  Validates against the schema registry before delegating to the store fn."
  [op store-fn]
  (fn [observation]
    (validate-write! op observation)
    (store-fn observation)))

;; ---------------------------------------------------------------------------
;; Observations adapter

(defn- make-observations-adapter
  "In-memory observations port. Append-only store for repository location
   observations, ingestion runs, projection checkpoints, and section
   extractions, indexed by request-id for idempotent lookup.

   Every write is validated against the schema registry (ENG-017C).
   Repository-location writes enforce idempotency: same request-ID
   replay returns the recorded fact; same request-ID with different
   content returns {:code :idempotency-conflict}."
  []
  (let [by-request-id (atom {})
        ingestion-runs (atom [])
        checkpoints (atom [])
        section-extractions (atom [])
        revision-at-paths (atom [])
        review-decisions (atom [])
        review-decision-index (atom {})]
    {:find-by-request-id (fn [request-id]
                           (get @by-request-id request-id))
     :record-repository-location! (fn [observation]
                                    (idempotent-record-repository-location!
                                     by-request-id observation))
     :record-revision-at-path! (validated-record-fn
                                 :record-revision-at-path!
                                 (fn [observation]
                                   (swap! revision-at-paths conj observation)
                                   nil))
     :record-ingestion-run! (validated-record-fn
                              :record-ingestion-run!
                              (fn [observation]
                                (swap! ingestion-runs conj observation)
                                nil))
     :record-checkpoint! (validated-record-fn
                           :record-checkpoint!
                           (fn [observation]
                             (swap! checkpoints conj observation)
                             nil))
     :record-section-extraction! (validated-record-fn
                                   :record-section-extraction!
                                   (fn [observation]
                                     (swap! section-extractions conj observation)
                                     nil))
     ;; Append-only, idempotent by request-id: a retry carrying a
     ;; request-id already recorded returns nil without appending a
     ;; second decision (ENG-005A AC: "retries do not duplicate").
     :record-review-decision! (fn [observation]
                                (let [rid (:observation/request-id observation)]
                                  (if (contains? @review-decision-index rid)
                                    nil
                                    (do (validate-write! :record-review-decision! observation)
                                        (swap! review-decision-index assoc rid observation)
                                        (swap! review-decisions conj observation)
                                        nil))))
     :list-ingestion-runs (fn [resource-id]
                            (filterv #(= resource-id (:resource-id %))
                                     @ingestion-runs))
     :list-checkpoints (fn [ingestion-run-id]
                         (filterv #(= ingestion-run-id
                                      (:checkpoint/ingestion-run-id %))
                                  @checkpoints))
     :list-revision-at-path-by-resource (fn [resource-id]
                                          (filterv #(and (= resource-id (:resource-id %))
                                                         (= :revision/at-path-observed (:observation/type %)))
                                                   @revision-at-paths))
     :list-section-extractions-by-revision (fn [revision-at-path-id]
                                              (filterv #(and (= revision-at-path-id
                                                               (:extraction/revision-at-path-id %))
                                                            (= :section/extraction-completed (:observation/type %)))
                                                       @section-extractions))
     :list-review-decisions (fn [resource-id]
                              (filterv #(= resource-id (:resource-id %))
                                       @review-decisions))
     :list-review-decisions-by-candidate (fn [candidate-id]
                                           (filterv #(= candidate-id
                                                        (:review-decision/candidate-id %))
                                                    @review-decisions))
      :export-all (fn []
                    {"repository-location" (vals @by-request-id)
                     "ingestion-run"       @ingestion-runs
                     "projection-checkpoint" @checkpoints
                     "section-extraction"  @section-extractions
                     "revision-at-path"    @revision-at-paths
                     "review-decision"     @review-decisions})
      :import-all (fn [data]
                    (doseq [[coll-name docs] data]
                      (case coll-name
                        "repository-location"
                        (doseq [doc docs]
                          (let [rid (:observation/request-id doc)]
                            (when (and rid (not (get @by-request-id rid)))
                              (validate-write! :record-repository-location! doc)
                              (swap! by-request-id assoc rid doc))))
                        "ingestion-run"
                        (doseq [doc docs]
                          (validate-write! :record-ingestion-run! doc)
                          (swap! ingestion-runs conj doc))
                        "projection-checkpoint"
                        (doseq [doc docs]
                          (validate-write! :record-checkpoint! doc)
                          (swap! checkpoints conj doc))
                        "section-extraction"
                        (doseq [doc docs]
                          (validate-write! :record-section-extraction! doc)
                          (swap! section-extractions conj doc))
                        "revision-at-path"
                        (doseq [doc docs]
                          (validate-write! :record-revision-at-path! doc)
                          (swap! revision-at-paths conj doc))
                        "review-decision"
                        (doseq [doc docs]
                          (let [rid (:observation/request-id doc)]
                            (when (and rid (not (contains? @review-decision-index rid)))
                              (validate-write! :record-review-decision! doc)
                              (swap! review-decision-index assoc rid doc)
                              (swap! review-decisions conj doc))))
                        nil)))}))

;; ---------------------------------------------------------------------------
;; Index adapter

(defn- make-index-adapter
  "In-memory index port. Stores indexed documents in an atom vector.
   For unit testing — no full-text search, just stores and returns them."
  []
  (let [docs (atom [])
        embeddings (atom [])
        version (atom 1)]
    {:index-sections! (fn [extraction-record]
                        (swap! docs conj extraction-record)
                        nil)
     :search (fn [query]
               ;; Simple substring match for unit testing
               (let [q (clojure.string/lower-case query)
                     matches (filterv
                              (fn [rec]
                                (some (fn [s]
                                        (clojure.string/includes?
                                         (clojure.string/lower-case
                                          (apply str (:section/heading-path s)))
                                         q))
                                      (:extraction/sections rec)))
                              @docs)]
                 (mapv (fn [rec]
                         {:result/path-raw (:extraction/path-raw rec)
                          :result/commit-oid (:extraction/commit-oid rec)
                          :result/sections (:extraction/sections rec)})
                       matches)))
     :index-embeddings! (fn [embedding-records]
                          (swap! embeddings into embedding-records)
                          nil)
     :knn-search (fn [{:keys [vector k embedding-version]}]
                   ;; Simple cosine similarity for unit testing
                   (let [query-vec (vec vector)
                         norm (fn [v] (let [mag (Math/sqrt (reduce + (map #(* % %) v)))]
                                        (if (zero? mag) v (map #(/ % mag) v))))
                         q-norm (norm query-vec)
                         scored (->> @embeddings
                                     (filter #(or (nil? embedding-version)
                                                  (= embedding-version (:embedding-version %))))
                                     (map (fn [emb]
                                            (let [e-vec (:embedding/vector emb)
                                                  e-norm (norm e-vec)
                                                  score (reduce + (map * q-norm e-norm))]
                                              (assoc emb :result/score score))))
                                     (sort-by :result/score >)
                                     (take (or k 10)))]
                     (mapv (fn [emb]
                             {:result/path-raw (:embedding/path-raw emb)
                              :result/commit-oid (:embedding/commit-oid emb)
                              :result/heading-path (:embedding/heading-path emb)
                              :result/score (:result/score emb)
                              :result/model (:embedding/model emb)
                              :result/embedding-version (str (:embedding-version emb))})
                           scored)))
     :index-version (fn [] @version)
     :rebuild-index! (fn [records]
                       (reset! docs (vec records))
                       nil)
     :clear-index! (fn []
                     (reset! docs [])
                     (reset! embeddings [])
                     nil)}))

;; ---------------------------------------------------------------------------
;; Embeddings adapter

(defn- make-embeddings-adapter
  "In-memory embeddings port. Stores embedding records in an atom vector.
   For unit testing — deterministic vectors derived from text hash."
  []
  (let [embeddings (atom [])
        version (atom 1)]
    {:embed-sections! (fn [extraction-records]
                        (let [results
                              (mapcat (fn [rec]
                                        (map (fn [s]
                                               {:embedding/path-raw (:extraction/path-raw rec)
                                                :embedding/commit-oid (:extraction/commit-oid rec)
                                                :embedding/heading-path (:section/heading-path s)
                                                :embedding/level (:section/level s)
                                                :embedding/ordinal (:section/ordinal s)
                                                :embedding/vector (vec (repeatedly 8 #(double (- (rand 2) 1))))
                                                :embedding/model "test-embed"
                                                :embedding/dimensions 8})
                                             (:extraction/sections rec)))
                                      extraction-records)]
                          (swap! embeddings into results)
                          (vec results)))
     :embed-query (fn [text]
                    (vec (repeatedly 8 #(double (- (rand 2) 1)))))
     :embedding-version (fn [] @version)
     :clear-embeddings! (fn []
                          (reset! embeddings [])
                          nil)}))

;; ---------------------------------------------------------------------------
;; Composite constructor

(defn make
  "Construct a complete, isolated set of in-memory adapters.

   Options:
     :common-git-dir-fn  (fn [path] -> string)
       Required. Function that resolves a repository path to its common
       Git directory. In tests, inject a test double. In direct mode,
       use epiphany.infra.git/common-git-directory.

   Returns a port map satisfying epiphany.law.ports/application-ports-schema."
  [{:keys [common-git-dir-fn]}]
  (when-not common-git-dir-fn
    (throw (ex-info "In-memory adapters require :common-git-dir-fn" {})))
  {:git                 (make-git-adapter common-git-dir-fn)
   :repository-metadata (make-repository-metadata-adapter)
   :observations        (make-observations-adapter)
   :index               (make-index-adapter)
   :embeddings          (make-embeddings-adapter)})
