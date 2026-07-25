(ns epiphany.law-suite.sabotage-test
  "ENG-017J: the two critical mutants, hand-rolled.

   The mutant that must die is 'the validation call was removed (or
   mistargeted) and invalid data persisted'. Each test INJECTS the
   mutant into a fresh adapter, runs the ENG-017D law suite, and
   asserts the suite DETECTS it — proof the harness notices when
   enforcement disappears or is mis-aimed. Surviving-mutant
   classification: none — both mutants die.

   Tool pilot outcome (recorded per the card's hard gate): heretic
   (io.github.parenstech/heretic @ 45ed7c7) is deps.edn-compatible and
   runs under clojure -M (alias :heretic retained); coverage collection
   succeeded, but a scoped 2-namespace mutant run exceeded 139 CPU-min
   without completing — classified TOOL LIMITATION for CI gating at
   this instrumentation scope; the alias remains for local runs."
  (:require [clojure.test :refer [deftest is testing]]
            [epiphany.infra.adapters.in-memory :as in-memory]
            [epiphany.law-suite.observations-laws :as laws]
            [epiphany.law.operations :as operations]))

(deftest removed-validation-mutant-dies
  (testing "mutant: validate-write! is a no-op — the law suite MUST notice"
    (with-redefs-fn {#'in-memory/validate-write! (fn [_op _record] nil)}
        (fn []
          (let [outcomes (laws/observations-laws
                          {:make-port (fn [] (:observations (in-memory/make {:common-git-dir-fn (fn [p] (str p "/.git"))})))
                           :capabilities #{:schema-validation :idempotency :export-import}})
                failed (laws/failed-laws outcomes)]
            (is (contains? failed [:record-repository-location! :invalid-write-rejected])
                "with validation removed, the invalid-write law must FAIL (mutant dies)")
            (is (contains? failed [:record-repository-location! :rejection-leaves-state-unchanged])
                "with validation removed, the state-unchanged law must FAIL (mutant dies)"))))
    (testing "sanity: the unmutated adapter passes (the mutant, not the harness, caused the failure)"
      (let [outcomes (laws/observations-laws
                      {:make-port (fn [] (:observations (in-memory/make {:common-git-dir-fn (fn [p] (str p "/.git"))})))
                       :capabilities #{:schema-validation :idempotency :export-import}})]
        (is (empty? (laws/failed-laws outcomes))
            (str "unmutated reference adapter must pass; failures: "
                 (pr-str (laws/failed-laws outcomes))))))))

(deftest wrong-schema-selection-mutant-dies
  (testing "mutant: every write op's registry entry points at the repository-location schema — the law suite MUST notice"
    (with-redefs [operations/operations
                  (into {} (map (fn [[op entry]]
                                  [op (assoc entry :input-schema "observation/repository-location-v1")]))
                        operations/operations)]
      (let [outcomes (laws/observations-laws
                      {:make-port (fn [] (:observations (in-memory/make {:common-git-dir-fn (fn [p] (str p "/.git"))})))
                       :capabilities #{:schema-validation :idempotency :export-import}})
            failed (laws/failed-laws outcomes)]
        (is (seq failed)
            "a wrong-schema mutant must produce law failures (mutant dies)")
        (is (some (fn [[op law]]
                    (and (= :valid-write-accepted law)
                         (not= :record-repository-location! op)))
                  failed)
            "records mis-validated against the wrong schema are falsely rejected (mutant dies)")))))
