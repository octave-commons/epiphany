(ns epiphany.law-suite.observations-laws
  "Parameterized law suite for observation-port adapters.

  Every adapter that implements the observations port must pass this
  suite. The harness is data-parameterized twice over:

    :make-port / :port — the adapter under judgment
    :capabilities      — what the adapter declares it enforces

  and every registered write operation is judged by the same universal
  laws (valid accepted, invalid rejected, rejection-without-mutation),
  plus the idempotency laws its record kind supports (ENG-017N item 3 —
  the harness is no longer repository-location-only).

  Idempotency kinds (per-op fixture declaration):
    :full           — identical replay nil; changed-content replay
                      returns {:code :idempotency-conflict} and the
                      stored fact stays the original (repository-location)
    :first-write-wins — any replay returns nil; the stored fact stays
                      the original (review-decision, lineage-candidate)
    :none           — no request-id idempotency laws apply

  The runner is a pure function of its argument map: it returns a
  normalized map of [op law] -> outcome and emits no clojure.test
  assertions itself. Callers inspect the returned data and make their
  own assertions. This lets a negative fixture prove the harness has
  teeth without failing the enclosing test suite.

  Each outcome is a map:
    {:outcome :pass}
    {:outcome :fail   :detail \"...\"}
    {:outcome :skip   :capability :kw}

  `:skip` and `:pass` are genuinely distinguishable — a law whose
  required capability is not declared is reported as `:skip`, never as
  a silent pass.")

;; ---------------------------------------------------------------------------
;; Fixtures — one valid-record builder per write op

(def ^:private a-uuid #uuid "aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeeee")
(def ^:private an-oid "0123456789abcdef0123456789abcdef01234567")
(def ^:private an-oid-2 "fedcba9876543210fedcba9876543210fedcba98")

(defn- base-envelope
  [rid]
  {:observation/id #uuid "00000000-0000-0000-0000-000000000001"
   :observation/observed-at #inst "2026-01-01T00:00:00.000Z"
   :observation/adapter-version "law-suite-v1"
   :observation/schema-version 1
   :observation/request-id rid
   :resource-id a-uuid})

(defn- valid-repository-location
  [rid]
  (assoc (base-envelope rid)
         :observation/type :repository/location-observed
         :repository/path {:path/raw "/law/test-repo"
                           :path/source :filesystem-argument
                           :path/comparison :exact}
         :repository/common-git-dir {:path/raw "/law/test-repo/.git"
                                     :path/source :filesystem-argument
                                     :path/comparison :exact}))

(defn- valid-revision-at-path
  [rid]
  (-> (base-envelope rid)
      (dissoc :observation/request-id)
      (assoc :observation/type :revision/at-path-observed
             :revision-at-path/id #uuid "bbbbbbbb-0000-0000-0000-000000000001"
             :revision/commit-oid an-oid
             :revision/tree-oid an-oid
             :revision/path-raw "docs/law.md"
             :revision/blob-oid an-oid-2
             :revision/mode 33188
             :revision/evidence :add)))

(defn- valid-ingestion-run
  [rid]
  (assoc (base-envelope rid)
         :observation/type :ingestion/run-completed
         :ingestion/repo-path {:path/raw "/law/test-repo"
                               :path/source :filesystem-argument
                               :path/comparison :exact}
         :ingestion/selected-refs ["HEAD"]
         :ingestion/commit-count 3
         :ingestion/failure-count 0
         :ingestion/failures []))

(defn- valid-checkpoint
  [rid]
  (-> (base-envelope rid)
      (dissoc :observation/request-id)
      (assoc :observation/type :projection/checkpoint-recorded
             :checkpoint/projection-name "law-projection"
             :checkpoint/projection-version 1
             :checkpoint/ingestion-run-id #uuid "cccccccc-0000-0000-0000-000000000001"
             :checkpoint/status :completed
             :checkpoint/processed-count 3)))

(defn- valid-section-extraction
  [rid]
  (-> (base-envelope rid)
      (dissoc :observation/request-id)
      (assoc :observation/type :section/extraction-completed
             :extraction/revision-at-path-id #uuid "dddddddd-0000-0000-0000-000000000001"
             :extraction/commit-oid an-oid
             :extraction/path-raw "docs/law.md"
             :extraction/blob-oid an-oid-2
             :extraction/extractor-version "law-extractor-v1"
             :extraction/section-count 1
             :extraction/content-sha256 "abc123"
             :extraction/sections [{:section/heading-path ["Law"]
                                    :section/level 1
                                    :section/ordinal 0
                                    :section/heading-span-start-byte 0
                                    :section/heading-span-end-byte 5
                                    :section/body-span-start-byte 6
                                    :section/body-span-end-byte 20
                                    :section/body-span-start-line 2
                                    :section/body-span-end-line 3}])))

(defn- valid-review-decision
  [rid]
  (assoc (base-envelope rid)
         :observation/type :review/decision-recorded
         :review-decision/id #uuid "eeeeeeee-0000-0000-0000-000000000001"
         :review-decision/candidate-id #uuid "ffffffff-0000-0000-0000-000000000001"
         :review-decision/decision :accepted
         :review-decision/decided-at #inst "2026-01-01T00:00:00.000Z"))

(defn- valid-lineage-candidate
  [rid]
  (assoc (base-envelope rid)
         :observation/type :lineage/candidate-generated
         :lineage-candidate/id #uuid "99999999-0000-0000-0000-000000000001"
         :lineage-candidate/relation :continues
         :lineage-candidate/generator-version "law-generator-v1"
         :lineage-candidate/confidence (double 0.5)
         :lineage-candidate/source {:span/path-raw "docs/a.md"
                                    :span/heading-path ["A"]
                                    :span/commit-oid an-oid}
         :lineage-candidate/target {:span/path-raw "docs/b.md"
                                    :span/heading-path ["B"]
                                    :span/commit-oid an-oid-2}
         :lineage-candidate/tier :provisional
         :lineage-candidate/generated-at #inst "2026-01-01T00:00:00.000Z"))

(defn- invalid-record
  "A map that fails every observation schema (missing envelope)."
  []
  {:observation/request-id #uuid "ffffffff-ffff-ffff-ffff-ffffffffffff"
   :resource-id a-uuid})

;; ---------------------------------------------------------------------------
;; Per-op fixture table
;;
;; :idempotency — which idempotency laws apply (see ns docstring).

(def op-fixtures
  "Record-kind fixtures per registered write operation."
  {:record-repository-location! {:make-valid valid-repository-location
                                 :make-conflict (fn [record]
                                                  (assoc record :observation/id
                                                         #uuid "00000000-0000-0000-0000-000000000002"))
                                 :idempotency :full}
   :record-revision-at-path! {:make-valid valid-revision-at-path
                              :idempotency :none}
   :record-ingestion-run! {:make-valid valid-ingestion-run
                           :idempotency :none}
   :record-checkpoint! {:make-valid valid-checkpoint
                        :idempotency :none}
   :record-section-extraction! {:make-valid valid-section-extraction
                                :idempotency :none}
   :record-review-decision! {:make-valid valid-review-decision
                             :make-conflict (fn [record]
                                              (assoc record :review-decision/decision :rejected))
                             :idempotency :first-write-wins}
   :record-lineage-candidate! {:make-valid valid-lineage-candidate
                               :make-conflict (fn [record]
                                                (assoc record :lineage-candidate/confidence
                                                       (double 0.99)))
                               :idempotency :first-write-wins}})

;; ---------------------------------------------------------------------------
;; Law definitions
;;
;; Each law is a pure predicate over a fresh port: it returns an outcome
;; map and NEVER emits a clojure.test assertion.

(defn- pass [] {:outcome :pass})
(defn- fail [detail] {:outcome :fail :detail detail})

(defn- law-valid-write-accepted
  [port op {:keys [make-valid]}]
  (let [record (make-valid #uuid "10000000-0000-0000-0000-000000000001")]
    (try
      (let [result ((get port op) record)]
        (if (nil? result)
          (pass)
          (fail (str "expected nil for a valid first write, got " (pr-str result)))))
      (catch Exception e
        (fail (str "valid write threw " (.getName (class e)) ": " (.getMessage e)))))))

(defn- law-invalid-write-rejected
  [port op _fixture]
  (try
    (let [result ((get port op) (invalid-record))]
      (fail (str "invalid record was accepted (no ExceptionInfo); returned "
                 (pr-str result))))
    (catch clojure.lang.ExceptionInfo _
      (pass))
    (catch Exception e
      (fail (str "invalid write threw " (.getName (class e))
                 ", expected clojure.lang.ExceptionInfo")))))

(defn- law-rejection-leaves-state-unchanged
  [port op _fixture]
  (let [snapshot-before ((:export-all port))]
    (try
      ((get port op) (invalid-record))
      (catch clojure.lang.ExceptionInfo _)
      (catch Exception _))
    (let [snapshot-after ((:export-all port))]
      (if (= snapshot-before snapshot-after)
        (pass)
        (fail "export-all differs before vs after a rejected write")))))

(defn- law-idempotent-replay-stable
  [port op {:keys [make-valid]}]
  (let [rid #uuid "20000000-0000-0000-0000-000000000001"
        record (make-valid rid)]
    (try
      ((get port op) record)
      (let [result ((get port op) record)]
        (if (some? result)
          (fail (str "replay with identical content must return nil, got " (pr-str result)))
          (pass)))
      (catch Exception e
        (fail (str "idempotent replay threw " (.getName (class e)) ": " (.getMessage e)))))))

(defn- law-changed-content-replay
  [port op {:keys [make-valid make-conflict idempotency]}]
  (let [rid #uuid "30000000-0000-0000-0000-000000000001"
        record1 (make-valid rid)
        record2 (make-conflict record1)]
    (try
      ((get port op) record1)
      (let [result ((get port op) record2)]
        (case idempotency
          :full
          (if (not= :idempotency-conflict (:code result))
            (fail (str "expected {:code :idempotency-conflict}, got " (pr-str result)))
            (pass))

          :first-write-wins
          (if (some? result)
            (fail (str "first-write-wins replay must return nil, got " (pr-str result)))
            (pass))

          (fail (str "no changed-content law for idempotency kind " idempotency))))
      (catch Exception e
        (fail (str "changed-content replay threw " (.getName (class e)) ": " (.getMessage e)))))))

(defn- law-export-import-round-trip
  [port _op {:keys [make-valid]}]
  (let [rid #uuid "40000000-0000-0000-0000-000000000001"
        record (make-valid rid)]
    (try
      ((:record-repository-location! port) record)
      (let [exported ((:export-all port))]
        ((:import-all port) exported)
        (if (= (get exported "repository-location")
               (get ((:export-all port)) "repository-location"))
          (pass)
          (fail "re-export after import does not match original export")))
      (catch Exception e
        (fail (str "export/import round-trip threw " (.getName (class e)) ": " (.getMessage e)))))))

;; ---------------------------------------------------------------------------
;; Law registry
;;
;; Universal laws run for every op; idempotency laws run only for ops
;; whose fixture declares an idempotency kind other than :none. The
;; export/import round-trip runs once, against repository-location.

(def ^:private universal-laws
  [{:law :valid-write-accepted            :capability nil                :run law-valid-write-accepted}
   {:law :invalid-write-rejected          :capability :schema-validation :run law-invalid-write-rejected}
   {:law :rejection-leaves-state-unchanged :capability :schema-validation :run law-rejection-leaves-state-unchanged}])

(def ^:private idempotency-laws
  [{:law :idempotent-replay-stable        :capability :idempotency       :run law-idempotent-replay-stable}
   {:law :changed-content-replay          :capability :idempotency       :run law-changed-content-replay}])

(def ^:private global-laws
  [{:law :export-import-round-trip        :capability :export-import
    :op :record-repository-location!      :run law-export-import-round-trip}])

(defn- resolve-port-provider
  [{:keys [make-port port]}]
  (cond
    make-port make-port
    port (constantly port)
    :else (throw (ex-info "law suite requires :make-port or :port"
                          {:code :invalid-law-suite-argument}))))

;; ---------------------------------------------------------------------------
;; Law suite runner

(defn observations-laws
  "Run the observation-port law suite and return normalized outcomes.

   Argument map:
     :make-port    — zero-arg factory returning a fresh observations port
                     (preferred: each law runs against an isolated port).
     :port         — a shared observations port map (used only when
                     :make-port is absent).
     :capabilities — a set of capability keywords declared by the adapter.
     :ops          — write operations to judge (default: every op with a
                     fixture in `op-fixtures`).

   Returns a map of [op law] -> outcome map. Every applicable law is
   present in the result:
     {:outcome :pass}
     {:outcome :fail :detail \"...\"}
     {:outcome :skip :capability :kw}   ; required capability not declared

   Supported capabilities:
     :schema-validation — adapter validates records against schemas
     :idempotency      — adapter enforces request-ID idempotency
     :export-import    — adapter supports export-all/import-all round-trip

   This function emits no clojure.test assertions; callers inspect the
   returned data. Skip and pass are distinguishable outcomes."
  [{:keys [capabilities ops] :as args}]
  (let [caps (or capabilities #{})
        provider (resolve-port-provider args)
        judged-ops (or ops (keys op-fixtures))]
    (into {}
          (concat
           (for [op judged-ops
                 :let [fixture (get op-fixtures op)]
                 {:keys [law capability run]} universal-laws]
             [[op law]
              (if (and capability (not (contains? caps capability)))
                {:outcome :skip :capability capability}
                (run (provider) op fixture))])
           (for [op judged-ops
                 :let [fixture (get op-fixtures op)]
                 :when (not= :none (:idempotency fixture))
                 {:keys [law capability run]} idempotency-laws]
             [[op law]
              (if (and capability (not (contains? caps capability)))
                {:outcome :skip :capability capability}
                (run (provider) op fixture))])
           (for [{:keys [law capability op run]} global-laws
                 :when (some #(= op %) judged-ops)]
             [[op law]
              (if (and capability (not (contains? caps capability)))
                {:outcome :skip :capability capability}
                (run (provider) op (get op-fixtures op)))])))))

(defn failed-laws
  "Return the set of [op law] keys whose outcome is :fail."
  [outcomes]
  (into #{} (keep (fn [[law-key {:keys [outcome]}]]
                    (when (= :fail outcome) law-key))
                  outcomes)))

(defn skipped-laws
  "Return the set of [op law] keys whose outcome is :skip."
  [outcomes]
  (into #{} (keep (fn [[law-key {:keys [outcome]}]]
                    (when (= :skip outcome) law-key))
                  outcomes)))
