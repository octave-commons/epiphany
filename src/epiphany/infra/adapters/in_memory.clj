(ns epiphany.infra.adapters.in-memory
  "In-memory adapters for :local (direct) mode.

  Every adapter is a plain map of keyword -> function, matching the port
  shapes defined in epiphany.law.ports. State is held in atoms; nothing
  survives process restart. These adapters are for unit testing and the
  direct-mode REPL workflow — never for :services mode.

  The constructor `make` returns a fresh, isolated adapter set. Each
  call produces an independent world with its own atoms."

  )

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
;; Observations adapter

(defn- make-observations-adapter
  "In-memory observations port. Append-only store for repository location
   observations, ingestion runs, projection checkpoints, and section
   extractions, indexed by request-id for idempotent lookup."
  []
  (let [by-request-id (atom {})
        ingestion-runs (atom [])
        checkpoints (atom [])
        section-extractions (atom [])
        revision-at-paths (atom [])]
    {:find-by-request-id (fn [request-id]
                           (get @by-request-id request-id))
     :record-repository-location! (fn [observation]
                                    (when-let [rid (:request-id observation)]
                                      (swap! by-request-id assoc rid observation))
                                    nil)
     :record-revision-at-path! (fn [observation]
                                 (swap! revision-at-paths conj observation)
                                 nil)
     :record-ingestion-run! (fn [observation]
                              (swap! ingestion-runs conj observation)
                              nil)
     :record-checkpoint! (fn [observation]
                           (swap! checkpoints conj observation)
                           nil)
     :record-section-extraction! (fn [observation]
                                   (swap! section-extractions conj observation)
                                   nil)
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
                                                      @section-extractions))}))

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
