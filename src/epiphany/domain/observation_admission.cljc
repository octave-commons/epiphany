(ns epiphany.domain.observation-admission
  "Pure admission of new observations against the locked accepted snapshot.

  Historical operation replay remains unchanged. Only new invocations remove
  already accepted identities before the reference adapter stages a write.")

(defn- identity-key [collection record]
  (case collection
    "revision-at-path" (select-keys record [:resource-id :revision/commit-oid :revision/path-raw])
    ("repository-location" "review-decision" "lineage-candidate") (:observation/request-id record)
    (:observation/id record)))

(defn- comparable-record [collection record]
  (case collection
    "revision-at-path" (dissoc record :observation/id :revision-at-path/id :observation/observed-at)
    ("ingestion-run" "projection-checkpoint" "section-extraction")
    (dissoc record :observation/observed-at)
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
  (case operation
    :record-revision-at-path!
    (when-let [record (first (missing-records "revision-at-path"
                                              (get snapshot "revision-at-path") arguments))]
      [record])
    :import-all
    [(into {} (map (fn [[collection records]]
                     [collection (missing-records collection (get snapshot collection) records)]))
           (first arguments))]
    arguments))
