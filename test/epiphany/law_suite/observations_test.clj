(ns epiphany.law-suite.observations-test
  "Drive the observation-port law suite against:
    1. The ENG-017C in-memory reference adapter (must pass all laws,
       for every registered write operation — ENG-017N item 3)
    2. A deliberately permissive fixture adapter (must FAIL the schema
       rejection laws) — proving the harness itself has teeth.

  Both tests run the *same* shared harness (`laws/observations-laws`)
  and assert on the normalized per-law outcome data it returns. The
  negative test therefore proves that a silently-weakened harness that
  passed permissive adapters would be caught here."
  (:require [clojure.test :refer [deftest is testing]]
            [epiphany.infra.adapters.in-memory :as in-memory]
            [epiphany.law-suite.observations-laws :as laws]))

(defn- make-reference-port
  "Factory for the ENG-017C in-memory reference observations port."
  []
  (:observations (in-memory/make {:common-git-dir-fn (fn [p] (str p "/.git"))})))

;; ---------------------------------------------------------------------------
;; Reference adapter (ENG-017C in-memory) — must pass every declared law,
;; for every registered write operation.

(deftest reference-adapter-passes-all-laws
  (testing "ENG-017C in-memory adapter passes the full law suite for every write op"
    (let [outcomes (laws/observations-laws
                    {:make-port make-reference-port
                     :capabilities #{:schema-validation :idempotency :export-import}})]
      (is (empty? (laws/failed-laws outcomes))
          (str "no law may fail for the reference adapter; failures: "
               (pr-str (select-keys outcomes (laws/failed-laws outcomes)))))
      (is (empty? (laws/skipped-laws outcomes))
          "with every capability declared, no law may be skipped")
      (testing "universal laws hold for every registered write op"
        (doseq [op (keys laws/op-fixtures)
                law [:valid-write-accepted
                     :invalid-write-rejected
                     :rejection-leaves-state-unchanged]]
          (is (= :pass (:outcome (get outcomes [op law])))
              (str "law " [op law] " must pass"))))
      (testing "idempotency laws hold for request-id-bearing record kinds"
        (doseq [op [:record-repository-location!
                    :record-review-decision!
                    :record-lineage-candidate!]
                law [:idempotent-replay-stable :changed-content-replay]]
          (is (= :pass (:outcome (get outcomes [op law])))
              (str "law " [op law] " must pass, got " (pr-str (get outcomes [op law]))))))
      (testing "the export/import round-trip holds"
        (is (= :pass (:outcome (get outcomes [:record-repository-location! :export-import-round-trip]))))))))

;; ---------------------------------------------------------------------------
;; Permissive fixture adapter (bare swap!, like pre-ENG-017C).

(defn- make-permissive-adapter
  "An adapter that accepts any map without validation — the false-green
   oracle that ENG-017C was designed to retire. It CLAIMS the schema
   capability but does not actually enforce it."
  []
  (let [by-request-id (atom {})]
    {:find-by-request-id (fn [rid] (get @by-request-id rid))
     :record-repository-location! (fn [obs]
                                    (when-let [rid (:observation/request-id obs)]
                                      (swap! by-request-id assoc rid obs))
                                    nil)
     :record-revision-at-path! (fn [_] nil)
     :record-ingestion-run! (fn [_] nil)
     :record-checkpoint! (fn [_] nil)
     :record-section-extraction! (fn [_] nil)
     :record-review-decision! (fn [_] nil)
     :record-lineage-candidate! (fn [_] nil)
     :list-ingestion-runs (fn [_] [])
     :list-checkpoints (fn [_] [])
     :list-revision-at-path-by-resource (fn [_] [])
     :list-section-extractions-by-revision (fn [_] [])
     :export-all (fn [] {"repository-location" (vals @by-request-id)})
     :import-all (fn [_] nil)}))

(deftest permissive-adapter-fails-schema-laws
  (testing "the shared harness genuinely FAILS a permissive adapter"
    ;; Declare :schema-validation so the rejection laws actually RUN
    ;; against the permissive adapter. Because it does no validation,
    ;; those laws must report :fail — that is the proof the harness has
    ;; teeth. We assert on the returned outcome data, so a real law
    ;; failure here does NOT fail this test suite.
    (let [outcomes (laws/observations-laws
                    {:make-port make-permissive-adapter
                     :capabilities #{:schema-validation}})
          failed (laws/failed-laws outcomes)]
      (is (contains? failed [:record-repository-location! :invalid-write-rejected])
          "permissive adapter accepts invalid records, so the rejection law must FAIL")
      (is (contains? failed [:record-repository-location! :rejection-leaves-state-unchanged])
          "permissive adapter stores rejected writes, so the state-unchanged law must FAIL")
      (testing "the failing laws carry an explanatory :detail"
        (is (string? (:detail (get outcomes [:record-repository-location! :invalid-write-rejected]))))
        (is (string? (:detail (get outcomes [:record-repository-location! :rejection-leaves-state-unchanged])))))))
  (testing "undeclared capabilities yield a distinguishable :skip, not a silent pass"
    ;; No :export-import / :idempotency declared → those laws must be
    ;; reported :skip (distinguishable from :pass) so ENG-017E can rely
    ;; on the skip-vs-pass distinction.
    (let [outcomes (laws/observations-laws
                    {:make-port make-permissive-adapter
                     :capabilities #{:schema-validation}})
          skipped (laws/skipped-laws outcomes)]
      (is (contains? skipped [:record-repository-location! :export-import-round-trip])
          "export/import round-trip is GATED on :export-import; undeclared => :skip")
      (is (contains? skipped [:record-repository-location! :idempotent-replay-stable]))
      (is (contains? skipped [:record-repository-location! :changed-content-replay]))
      (is (= :skip (:outcome (get outcomes [:record-repository-location! :export-import-round-trip])))
          "skip must be a :skip outcome, never a passing (is true)")
      (is (not= :pass (:outcome (get outcomes [:record-repository-location! :export-import-round-trip])))
          "a skipped law must NOT be reported as :pass"))))
