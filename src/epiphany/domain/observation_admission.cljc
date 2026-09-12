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

(defn- identity-keys [collection record]
  (if (= collection "section-extraction")
    ;; Logical derivation identity survives independently generated command
    ;; envelopes. Accepted envelope IDs must still never name other content.
    (cond-> [[:extraction (select-keys record [:resource-id :extraction/revision-at-path-id
                                               :extraction/blob-oid :extraction/extractor-version])]
             [:observation (:observation/id record)]]
      (:observation/request-id record) (conj [:request (:observation/request-id record)]))
    [(case collection
       "revision-at-path" (select-keys record [:resource-id :revision/commit-oid :revision/path-raw])
       ("repository-location" "review-decision" "lineage-candidate")
       (if-let [request-id (:observation/request-id record)]
         [:request request-id]
         [:observation (:observation/id record)])
       (:observation/id record))]))

(defn- comparable-record [collection record]
  (case collection
    "revision-at-path" (dissoc record :observation/id :revision-at-path/id :observation/observed-at)
    ("ingestion-run" "projection-checkpoint")
    (dissoc record :observation/observed-at)
    "section-extraction" (dissoc record :observation/id :observation/request-id :observation/observed-at)
    "repository-location" (dissoc record :observation/id :observation/observed-at)
    "review-decision" (dissoc record :observation/id :observation/observed-at
                              :review-decision/id :review-decision/decided-at)
    "lineage-candidate" (dissoc record :observation/id :observation/observed-at
                                :lineage-candidate/id :lineage-candidate/generated-at)
    record))

(defn- retain-record [{:keys [index] :as state} collection record]
  (let [ids (identity-keys collection record)
        accepted (keep index ids)]
    (doseq [previous accepted]
      (when-not (= (comparable-record collection previous) (comparable-record collection record))
        (throw (ex-info "Observation identity has different accepted content"
                        {:code :idempotency-conflict :collection collection :identities ids}))))
    ;; Historical duplicate facts still reserve each of their accepted envelope
    ;; identities. A bulk batch must also reject conflicting aliases internally.
    (cond-> (update state :index into (map (fn [id] [id record]) ids))
      (empty? accepted) (update :records conj record))))

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

(defn command-status
  "Classify an identified command before replaying its prospective side effects."
  [events operation arguments command-id]
  (when command-id
    (if-let [accepted (some #(when (= command-id (get-in % [:event/data :command-id]))
                               (:event/data %)) events)]
      (if (= {:operation operation :arguments arguments}
             (select-keys accepted [:operation :arguments]))
        :accepted
        (throw (ex-info "Command identity has different accepted content"
                        {:code :idempotency-conflict :command-id command-id})))
      :new)))
