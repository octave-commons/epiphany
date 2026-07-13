(ns epiphany.law-suite.observations-test
  "Run the observation-port law suite against:
    1. The ENG-017C in-memory reference adapter (must pass all laws)
    2. A deliberately permissive fixture adapter (must fail rejection laws)"
  (:require [clojure.test :refer [deftest is testing]]
            [epiphany.infra.adapters.in-memory :as in-memory]
            [epiphany.law-suite.observations-laws :as laws]))

;; ---------------------------------------------------------------------------
;; Reference adapter (ENG-017C in-memory)

(deftest reference-adapter-passes-all-laws
  (testing "ENG-017C in-memory adapter passes the full law suite"
    (let [adapters (in-memory/make {:common-git-dir-fn (fn [p] (str p "/.git"))})
          port (:observations adapters)]
      (laws/observations-laws
       {:port port
        :capabilities #{:schema-validation :idempotency :export-import}}))))

;; ---------------------------------------------------------------------------
;; Permissive fixture adapter (bare swap!, like pre-ENG-017C)

(defn- make-permissive-adapter
  "An adapter that accepts any map without validation — the false-green
  oracle that ENG-017C was designed to retire."
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
     :list-ingestion-runs (fn [_] [])
     :list-checkpoints (fn [_] [])
     :list-revision-at-path-by-resource (fn [_] [])
     :list-section-extractions-by-revision (fn [_] [])
     :export-all (fn [] {"repository-location" (vals @by-request-id)})
     :import-all (fn [_] nil)}))

(deftest permissive-adapter-fails-schema-laws
  (testing "A permissive adapter must fail the schema-validation laws"
    (let [port (make-permissive-adapter)]
      ;; The permissive adapter accepts invalid records — so the
      ;; "invalid write rejected" law must NOT pass.
      ;; We test this by checking that the invalid record is accepted
      ;; (which means the law would fail if run against this adapter).
      (let [invalid {:observation/request-id #uuid "ffffffff-ffff-ffff-ffff-ffffffffffff"
                     :resource-id #uuid "aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeeee"}]
        (is (nil? ((:record-repository-location! port) invalid))
            "Permissive adapter must accept invalid records (proving it fails the law)")
        (is (some? ((:find-by-request-id port)
                    #uuid "ffffffff-ffff-ffff-ffff-ffffffffffff"))
            "Invalid record must be stored (proving permissive adapter has no validation)")))))
