(ns epiphany.infra.adapters.in-memory-test
  (:require [clojure.test :refer [deftest is testing]]
            [epiphany.infra.adapters.in-memory :as in-memory]
            [epiphany.domain.review :as review]
            [epiphany.law.registry :as registry]))

(defn- fake-common-git-dir [path]
  (str path "/.git"))

(defn- valid-repository-location
  "Build a valid observation/repository-location-v1 record."
  [rid]
  {:observation/id (random-uuid)
   :observation/observed-at (java.util.Date.)
   :observation/adapter-version "test"
   :observation/schema-version 1
   :observation/type :repository/location-observed
   :observation/request-id rid
   :resource-id #uuid "aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeeee"
   :repository/path {:path/raw "/repo"
                     :path/source :filesystem-argument
                     :path/comparison :exact}
   :repository/common-git-dir {:path/raw "/repo/.git"
                               :path/source :filesystem-argument
                               :path/comparison :exact}})

(defn- invalid-record
  "Build a map that fails schema validation (missing envelope)."
  []
  {:observation/request-id (random-uuid)
   :resource-id #uuid "aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeeee"})

(def ^:private resource-id #uuid "aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeeee")

(defn- review-decision-observation
  "Build a valid observation/review-decision-v1 record via the real
   domain builder, so the test exercises the same path production uses."
  [candidate-id decision-type & opts]
  (review/decision->observation
   (apply review/make-decision candidate-id decision-type opts)
   {:resource-id resource-id :adapter-version "test"}))

;; ---------------------------------------------------------------------------
;; Port satisfaction

(deftest in-memory-adapters-satisfy-application-ports-schema
  (let [adapters (in-memory/make {:common-git-dir-fn fake-common-git-dir})]
    (is (registry/valid? "application/ports" adapters)
        "In-memory adapters must satisfy the application ports schema")))

;; ---------------------------------------------------------------------------
;; Git adapter

(deftest in-memory-git-resolves-common-directory
  (let [adapters (in-memory/make {:common-git-dir-fn fake-common-git-dir})]
    (is (= "/repos/notes/.git"
           ((:common-git-directory (:git adapters)) "/repos/notes")))))

;; ---------------------------------------------------------------------------
;; Repository metadata adapter

(deftest in-memory-repository-metadata-round-trips
  (let [adapters (in-memory/make {:common-git-dir-fn fake-common-git-dir})
        repo-md  (:repository-metadata adapters)
        rid      #uuid "aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeeee"]
    (is (nil? ((:read repo-md) "/repo/.git")))
    ((:write repo-md) "/repo/.git" rid)
    (is (= {:resource-id rid}
           ((:read repo-md) "/repo/.git")))))

;; ---------------------------------------------------------------------------
;; Observations adapter — basic behavior

(deftest in-memory-observations-are-idempotent-by-request-id
  (let [adapters (in-memory/make {:common-git-dir-fn fake-common-git-dir})
        obs      (:observations adapters)
        rid      #uuid "11111111-2222-3333-4444-555555555555"
        record   (valid-repository-location rid)]
    (is (nil? ((:find-by-request-id obs) rid)))
    (is (nil? ((:record-repository-location! obs) record))
        "First write returns nil (success)")
    (is (= record ((:find-by-request-id obs) rid)))))

(deftest in-memory-require-common-git-dir-fn
  (is (thrown? clojure.lang.ExceptionInfo
              (in-memory/make {}))))

;; ---------------------------------------------------------------------------
;; ENG-017C: Contract enforcement

(deftest invalid-write-rejected-before-delegation
  (testing "adapter rejects schema-invalid records on direct use"
    (let [adapters (in-memory/make {:common-git-dir-fn fake-common-git-dir})
          obs      (:observations adapters)
          snapshot-before ((:export-all obs))]
      (is (thrown? clojure.lang.ExceptionInfo
                  ((:record-repository-location! obs) (invalid-record)))
          "Invalid record must throw")
      (testing "state is byte-identical after rejected write"
        (is (= snapshot-before ((:export-all obs)))
            "export-all must return same snapshot before and after")))))

(deftest invalid-write-rejected-for-non-idempotent-ops
  (testing "adapter rejects invalid records on non-idempotent record ops"
    (let [adapters (in-memory/make {:common-git-dir-fn fake-common-git-dir})
          obs      (:observations adapters)]
      (is (thrown? clojure.lang.ExceptionInfo
                  ((:record-ingestion-run! obs) (invalid-record)))
          "record-ingestion-run! must reject invalid records")
      (is (thrown? clojure.lang.ExceptionInfo
                  ((:record-checkpoint! obs) (invalid-record)))
          "record-checkpoint! must reject invalid records")
      (is (thrown? clojure.lang.ExceptionInfo
                  ((:record-section-extraction! obs) (invalid-record)))
          "record-section-extraction! must reject invalid records")
      (is (thrown? clojure.lang.ExceptionInfo
                  ((:record-revision-at-path! obs) (invalid-record)))
          "record-revision-at-path! must reject invalid records"))))

(deftest idempotent-replay-stable
  (testing "same request-ID twice returns nil (no mutation)"
    (let [adapters (in-memory/make {:common-git-dir-fn fake-common-git-dir})
          obs      (:observations adapters)
          rid      #uuid "22222222-3333-4444-5555-666666666666"
          record   (valid-repository-location rid)]
      ((:record-repository-location! obs) record)
      (is (nil? ((:record-repository-location! obs) record))
          "Replay with identical content returns nil (success)")
      (is (= record ((:find-by-request-id obs) rid))
          "Stored fact is unchanged"))))

(deftest changed-content-replay-conflicts
  (testing "same request-ID with different content returns conflict"
    (let [adapters (in-memory/make {:common-git-dir-fn fake-common-git-dir})
          obs      (:observations adapters)
          rid      #uuid "33333333-4444-5555-6666-777777777777"
          record1  (valid-repository-location rid)
          record2  (assoc record1 :observation/id (random-uuid))]
      ((:record-repository-location! obs) record1)
      (let [result ((:record-repository-location! obs) record2)]
        (is (= :idempotency-conflict (:code result))
            "Must return :idempotency-conflict")
        (is (= rid (:request-id result))
            "Conflict must include the request-id"))
      (is (= record1 ((:find-by-request-id obs) rid))
          "Stored fact must be the original, not the conflicting one"))))

;; ---------------------------------------------------------------------------
;; ENG-005A: Review-decision events (append-only, idempotent, queryable)

(deftest review-decision-recorded-and-listed
  (testing "a recorded decision is queryable by resource and by candidate"
    (let [obs (:observations (in-memory/make {:common-git-dir-fn fake-common-git-dir}))
          cid (random-uuid)
          rec (review-decision-observation cid :accepted)]
      (is (nil? ((:record-review-decision! obs) rec)) "first write returns nil (success)")
      (is (= [rec] ((:list-review-decisions obs) resource-id)))
      (is (= [rec] ((:list-review-decisions-by-candidate obs) cid)))
      (is (empty? ((:list-review-decisions-by-candidate obs) (random-uuid)))
          "an unrelated candidate has no decisions"))))

(deftest review-decision-idempotent-by-request-id
  (testing "a retry carrying the same request-id does not duplicate the decision"
    (let [obs (:observations (in-memory/make {:common-git-dir-fn fake-common-git-dir}))
          rid (random-uuid)
          rec (review-decision-observation (random-uuid) :rejected :request-id rid :reason "stale")]
      ((:record-review-decision! obs) rec)
      (is (nil? ((:record-review-decision! obs) rec)) "replay returns nil")
      (is (= 1 (count ((:list-review-decisions obs) resource-id)))
          "the decision appears exactly once after a retry"))))

(deftest review-decision-invalid-record-rejected
  (testing "a schema-invalid record throws and leaves state unchanged"
    (let [obs (:observations (in-memory/make {:common-git-dir-fn fake-common-git-dir}))
          before ((:export-all obs))]
      (is (thrown? clojure.lang.ExceptionInfo
                   ((:record-review-decision! obs) (invalid-record))))
      (is (= before ((:export-all obs))) "state is byte-identical after a rejected write")
      (is (empty? ((:list-review-decisions obs) resource-id))))))

(deftest review-decisions-queryable-by-type-and-time
  (testing "domain query helpers work over the durable list (AC: by candidate, relation type, time)"
    (let [obs (:observations (in-memory/make {:common-git-dir-fn fake-common-git-dir}))
          cid (random-uuid)]
      ((:record-review-decision! obs) (review-decision-observation cid :accepted))
      ((:record-review-decision! obs) (review-decision-observation cid :rejected :reason "dup"))
      (let [all ((:list-review-decisions obs) resource-id)]
        (is (= 2 (count all)))
        (is (= 1 (count (review/by-decision-type all :accepted))))
        (is (= 2 (count (review/by-time-range all nil nil))))))))

(deftest review-decisions-survive-export-import
  (testing "review decisions round-trip through export-all/import-all"
    (let [src (:observations (in-memory/make {:common-git-dir-fn fake-common-git-dir}))
          dst (:observations (in-memory/make {:common-git-dir-fn fake-common-git-dir}))
          rec (review-decision-observation (random-uuid) :do-not-suggest :suppressed true)]
      ((:record-review-decision! src) rec)
      ((:import-all dst) ((:export-all src)))
      (is (= [rec] ((:list-review-decisions dst) resource-id))))))
