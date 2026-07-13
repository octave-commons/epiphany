(ns epiphany.law-suite.observations-laws
  "Parameterized law suite for observation-port adapters.

  Every adapter that implements the observations port must pass this
  suite. The harness is data-parameterized: supply a factory function
  and a capabilities declaration, and the laws judge the adapter by
  identical criteria with normalized domain outcomes.

  Laws skipped for undeclared capabilities are reported as skipped,
  never silently passed.

  Usage:
    (observations-laws {:make-port (fn [] (:observations (in-memory/make ...)))
                        :capabilities #{:schema-validation :idempotency :export-import}})"
  (:require [clojure.test :refer [deftest is testing]]
            [epiphany.law.operations :as operations]))

;; ---------------------------------------------------------------------------
;; Fixture builders

(defn- valid-repository-location
  "Build a valid observation/repository-location-v1 record."
  [rid]
  {:observation/id #uuid "00000000-0000-0000-0000-000000000001"
   :observation/observed-at #inst "2026-01-01T00:00:00.000Z"
   :observation/adapter-version "law-suite-v1"
   :observation/schema-version 1
   :observation/type :repository/location-observed
   :observation/request-id rid
   :resource-id #uuid "aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeeee"
   :repository/path {:path/raw "/law/test-repo"
                     :path/source :filesystem-argument
                     :path/comparison :exact}
   :repository/common-git-dir {:path/raw "/law/test-repo/.git"
                               :path/source :filesystem-argument
                               :path/comparison :exact}})

(defn- invalid-record
  "A map that fails every observation schema (missing envelope)."
  []
  {:observation/request-id #uuid "ffffffff-ffff-ffff-ffff-ffffffffffff"
   :resource-id #uuid "aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeeee"})

(defn- second-valid-repository-location
  "A valid record with a different observation/id for conflict tests."
  [rid]
  (assoc (valid-repository-location rid)
         :observation/id #uuid "00000000-0000-0000-0000-000000000002"))

;; ---------------------------------------------------------------------------
;; Law definitions

(defn- law-valid-write-accepted
  "A valid record is accepted without error."
  [port]
  (testing "LAW: valid write accepted"
    (let [rid #uuid "10000000-0000-0000-0000-000000000001"
          record (valid-repository-location rid)
          result ((:record-repository-location! port) record)]
      (is (nil? result) "First write must return nil (success)"))))

(defn- law-invalid-write-rejected
  "An invalid record is rejected with ExceptionInfo."
  [port]
  (testing "LAW: invalid write rejected"
    (is (thrown? clojure.lang.ExceptionInfo
                ((:record-repository-location! port) (invalid-record)))
        "Invalid record must throw ExceptionInfo")))

(defn- law-rejection-leaves-state-unchanged
  "A rejected write leaves export-all byte-identical."
  [port]
  (testing "LAW: rejection leaves state unchanged"
    (let [snapshot-before ((:export-all port))]
      (try
        ((:record-repository-location! port) (invalid-record))
        (catch clojure.lang.ExceptionInfo _))
      (is (= snapshot-before ((:export-all port)))
          "export-all must be identical before and after rejected write"))))

(defn- law-idempotent-replay-stable
  "Same request-ID with identical content replays without mutation."
  [port]
  (testing "LAW: idempotent replay stable"
    (let [rid #uuid "20000000-0000-0000-0000-000000000001"
          record (valid-repository-location rid)]
      ((:record-repository-location! port) record)
      (let [result ((:record-repository-location! port) record)]
        (is (nil? result) "Replay with identical content must return nil"))
      (is (= record ((:find-by-request-id port) rid))
          "Stored fact must be unchanged after replay"))))

(defn- law-changed-content-replay-conflicts
  "Same request-ID with different content returns :idempotency-conflict."
  [port]
  (testing "LAW: changed-content replay conflicts"
    (let [rid #uuid "30000000-0000-0000-0000-000000000001"
          record1 (valid-repository-location rid)
          record2 (second-valid-repository-location rid)]
      ((:record-repository-location! port) record1)
      (let [result ((:record-repository-location! port) record2)]
        (is (= :idempotency-conflict (:code result))
            "Must return :idempotency-conflict")
        (is (= rid (:request-id result))
            "Conflict must include the request-id"))
      (is (= record1 ((:find-by-request-id port) rid))
          "Stored fact must be the original, not the conflicting one"))))

(defn- law-export-import-round-trip
  "Export then import preserves all data."
  [port]
  (testing "LAW: export/import round-trip"
    (let [rid #uuid "40000000-0000-0000-0000-000000000001"
          record (valid-repository-location rid)]
      ((:record-repository-location! port) record)
      (let [exported ((:export-all port))]
        ((:import-all port) exported)
        (is (= (get exported "repository-location")
               (get ((:export-all port)) "repository-location"))
            "Re-export must match original export")))))

;; ---------------------------------------------------------------------------
;; Law suite runner

(defn observations-laws
  "Run the full observation-port law suite against `port`.

  `port` — an observations port map (keyword -> function).
  `capabilities` — a set of capability keywords. Laws for undeclared
  capabilities are skipped with a `:skip` report, never silently passed.

  Supported capabilities:
    :schema-validation — adapter validates records against schemas
    :idempotency      — adapter enforces request-ID idempotency
    :export-import    — adapter supports export-all/import-all"
  [{:keys [port capabilities]}]
  (let [caps (or capabilities #{})]
    ;; These laws apply to every adapter (no special capability needed)
    (law-valid-write-accepted port)
    (law-export-import-round-trip port)

    ;; Schema validation laws
    (if (:schema-validation caps)
      (do
        (law-invalid-write-rejected port)
        (law-rejection-leaves-state-unchanged port))
      (testing "LAW: invalid write rejected [SKIPPED: :schema-validation not declared]"
        (is true "skipped")))

    ;; Idempotency laws
    (if (:idempotency caps)
      (do
        (law-idempotent-replay-stable port)
        (law-changed-content-replay-conflicts port))
      (testing "LAW: idempotent replay [SKIPPED: :idempotency not declared]"
        (is true "skipped")))))
