(ns epiphany.domain.observation-admission
  "Pure admission of new observations against the locked accepted snapshot.

  Historical operation replay remains unchanged. Only new invocations remove
  already accepted identities before the reference adapter stages a write.")

(def record-collections
  "Prospective direct writes and the accepted collection whose identity they reuse."
  {:record-repository-location! "repository-location"
   :record-ingestion-run! "ingestion-run"
   :record-checkpoint! "projection-checkpoint"
   :record-section-extraction! "section-extraction"
   :record-revision-at-path! "revision-at-path"
   :record-review-decision! "review-decision"
   :record-lineage-candidate! "lineage-candidate"})

(defn- identity-key [collection record]
  (case collection
    "revision-at-path" (select-keys record [:resource-id :revision/commit-oid :revision/path-raw])
    ("repository-location" "review-decision" "lineage-candidate")
    (if-let [request-id (:observation/request-id record)]
      [:request request-id]
      [:observation (:observation/id record)])
    (:observation/id record)))

(defn- comparable-record [collection record]
  (case collection
    "revision-at-path" (dissoc record :observation/id :revision-at-path/id :observation/observed-at)
    ("ingestion-run" "projection-checkpoint" "section-extraction")
    (dissoc record :observation/observed-at)
    "repository-location" (dissoc record :observation/id :observation/observed-at)
    "review-decision" (dissoc record :observation/id :observation/observed-at
                              :review-decision/id :review-decision/decided-at)
    "lineage-candidate" (dissoc record :observation/id :observation/observed-at
                                :lineage-candidate/id :lineage-candidate/generated-at)
    record))

(defn- retain-record [{:keys [index] :as state} collection record]
  (let [id (identity-key collection record)]
    (if-let [accepted (get index id)]
      (if (= (comparable-record collection accepted)
             (comparable-record collection record))
        state
        (throw (ex-info "Observation identity has different accepted content"
                        {:code :idempotency-conflict :collection collection :identity id})))
      (-> state (assoc-in [:index id] record) (update :records conj record)))))

(defn missing-records
  "Return new records in order; reject changed-content reuse before mutation."
  [collection accepted incoming]
  (let [known (reduce #(retain-record %1 collection %2) {:index {} :records []} accepted)]
    (:records (reduce #(retain-record %1 collection %2) (assoc known :records []) incoming))))

(defn invocation-arguments
  "Filter new invocation data against accepted facts; nil means an exact no-op."
  [snapshot operation arguments]
  (if-let [collection (get record-collections operation)]
    (when-let [record (first (missing-records collection (get snapshot collection) arguments))]
      [record])
    (case operation
      :import-all
      [(into {} (map (fn [[collection records]]
                       [collection (missing-records collection (get snapshot collection) records)]))
             (first arguments))]
      arguments)))
