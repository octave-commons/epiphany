(ns epiphany.infra.adapters.in-memory-test
  (:require [clojure.test :refer [deftest is testing]]
            [epiphany.infra.adapters.in-memory :as in-memory]
            [epiphany.domain.review :as review]
            [epiphany.domain.candidates :as candidates]
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
;; Rebuildable index behavior

(deftest in-memory-index-stats-are-resource-scoped
  (let [index (:index (in-memory/make {:common-git-dir-fn fake-common-git-dir}))
        other-resource-id (random-uuid)]
    ((:index-sections! index)
     {:resource-id resource-id
      :extraction/sections [{:section/ordinal 0} {:section/ordinal 1}]})
    ((:index-sections! index)
     {:resource-id other-resource-id
      :extraction/sections [{:section/ordinal 0}]})
    (is (= {:document-count 2} ((:index-stats index) resource-id)))
    (is (= {:document-count 1} ((:index-stats index) other-resource-id)))))

(deftest in-memory-embedding-replay-is-idempotent
  (let [index (:index (in-memory/make {:common-git-dir-fn fake-common-git-dir}))
        embedding {:resource-id resource-id
                   :embedding/path-raw "docs/a.md"
                   :embedding/commit-oid "1111111111111111111111111111111111111111"
                   :embedding/heading-path ["A"]
                   :embedding/ordinal 0
                   :embedding/model "test"
                   :embedding-version 1
                   :embedding/vector [1.0 0.0]}]
    ((:index-embeddings! index) [embedding])
    ((:index-embeddings! index) [embedding])
    (is (= 1
           (count ((:knn-search index)
                   {:vector [1.0 0.0] :k 10 :embedding-version 1}))))))

(deftest ingestion-run-and-checkpoint-replays-are-idempotent
  (let [observations (:observations
                      (in-memory/make {:common-git-dir-fn fake-common-git-dir}))
        run-id (random-uuid)
        run {:observation/type :ingestion/run-completed
             :observation/id run-id
             :observation/observed-at (java.util.Date.)
             :observation/adapter-version "test"
             :observation/schema-version 1
             :resource-id resource-id
             :ingestion/repo-path {:path/raw "/repo"
                                   :path/source :filesystem-argument
                                   :path/comparison :exact}
             :ingestion/selected-refs ["HEAD"]
             :ingestion/commit-count 0
             :ingestion/failure-count 0
             :ingestion/failures []}
        checkpoint {:observation/type :projection/checkpoint-recorded
                    :observation/id (random-uuid)
                    :observation/observed-at (java.util.Date.)
                    :observation/adapter-version "test"
                    :observation/schema-version 1
                    :resource-id resource-id
                    :checkpoint/projection-name "embedding"
                    :checkpoint/projection-version 1
                    :checkpoint/ingestion-run-id run-id
                    :checkpoint/status :completed
                    :checkpoint/processed-count 0}]
    ((:record-ingestion-run! observations) run)
    ((:record-ingestion-run! observations) run)
    ((:record-checkpoint! observations) checkpoint)
    ((:record-checkpoint! observations) checkpoint)
    (let [snapshot ((:export-all observations))]
      (is (= 1 (count (get snapshot "ingestion-run"))))
      (is (= 1 (count (get snapshot "projection-checkpoint")))))))

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

;; ---------------------------------------------------------------------------
;; ENG-005G: Lineage-candidate store (append-only, idempotent, queryable)

(def ^:private oid-a "1111111111111111111111111111111111111111")
(def ^:private oid-b "2222222222222222222222222222222222222222")

(defn- lineage-candidate-observation
  "Build a valid observation/lineage-candidate-v1 record via the real
   domain builder, so the test exercises the production path."
  [& {:keys [relation confidence generator-version request-id candidate-id generated-at]
      :or {relation :continues confidence 0.7 generator-version "gen-v1"}}]
  (let [c (candidates/make-candidate
           relation
           (candidates/make-span {:path-raw "docs/a.md" :heading-path ["A"] :commit-oid oid-a})
           (candidates/make-span {:path-raw "docs/b.md" :heading-path ["B"] :commit-oid oid-b})
           :confidence confidence
           :generator-version generator-version
           :request-id (or request-id (random-uuid))
           :candidate-id (or candidate-id (random-uuid))
           :generated-at (or generated-at (java.util.Date.)))]
    (candidates/candidate->observation c {:resource-id resource-id :adapter-version "test"})))

(deftest lineage-candidate-recorded-and-listed
  (testing "a recorded candidate is queryable by resource and by candidate id"
    (let [obs (:observations (in-memory/make {:common-git-dir-fn fake-common-git-dir}))
          cid (random-uuid)
          rec (lineage-candidate-observation :candidate-id cid)]
      (is (nil? ((:record-lineage-candidate! obs) rec)) "first write returns nil (success)")
      (is (= [rec] ((:list-lineage-candidates obs) resource-id)))
      (is (= rec ((:find-lineage-candidate-by-id obs) cid)))
      (is (nil? ((:find-lineage-candidate-by-id obs) (random-uuid)))
          "an unknown candidate id resolves to nil"))))

(deftest lineage-candidate-idempotent-by-request-id
  (testing "a retry carrying the same request-id does not duplicate the candidate"
    (let [obs (:observations (in-memory/make {:common-git-dir-fn fake-common-git-dir}))
          rid (random-uuid)
          rec (lineage-candidate-observation :request-id rid)]
      ((:record-lineage-candidate! obs) rec)
      (is (nil? ((:record-lineage-candidate! obs) rec)) "replay returns nil")
      (is (= 1 (count ((:list-lineage-candidates obs) resource-id)))
          "the candidate appears exactly once after a retry"))))

(deftest lineage-candidate-invalid-record-rejected
  (testing "a schema-invalid record throws and leaves state byte-identical"
    (let [obs (:observations (in-memory/make {:common-git-dir-fn fake-common-git-dir}))
          before ((:export-all obs))]
      (is (thrown? clojure.lang.ExceptionInfo
                   ((:record-lineage-candidate! obs) (invalid-record))))
      (is (= before ((:export-all obs))) "state is byte-identical after a rejected write")
      (is (empty? ((:list-lineage-candidates obs) resource-id))))))

(deftest lineage-candidates-queryable-by-every-dimension
  (testing "domain filters work over the durable list (AC3: id, relation, generator, confidence, time)"
    (let [obs (:observations (in-memory/make {:common-git-dir-fn fake-common-git-dir}))
          now (java.util.Date.)
          earlier (java.util.Date. (- (.getTime now) 3600000))
          cid (random-uuid)]
      ((:record-lineage-candidate! obs)
       (lineage-candidate-observation :candidate-id cid :relation :continues
                                      :generator-version "gen-v1" :confidence 0.9 :generated-at now))
      ((:record-lineage-candidate! obs)
       (lineage-candidate-observation :relation :refines
                                      :generator-version "gen-v2" :confidence 0.3 :generated-at earlier))
      (let [all ((:list-lineage-candidates obs) resource-id)]
        (is (= 2 (count all)))
        (is (= 1 (count (candidates/by-candidate-id all cid))))
        (is (= 1 (count (candidates/by-relation all :continues))))
        (is (= 1 (count (candidates/by-generator-version all "gen-v2"))))
        (is (= 1 (count (candidates/by-confidence-band all 0.8 1.0))))
        (is (= 1 (count (candidates/by-time-range all earlier now)))
            "half-open [earlier, now) includes only the earlier candidate")))))

(deftest lineage-candidates-survive-export-import
  (testing "lineage candidates round-trip through export-all/import-all"
    (let [src (:observations (in-memory/make {:common-git-dir-fn fake-common-git-dir}))
          dst (:observations (in-memory/make {:common-git-dir-fn fake-common-git-dir}))
          rec (lineage-candidate-observation :relation :possibly-supersedes)]
      ((:record-lineage-candidate! src) rec)
      ((:import-all dst) ((:export-all src)))
      (is (= [rec] ((:list-lineage-candidates dst) resource-id))))))
