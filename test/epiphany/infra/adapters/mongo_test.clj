(ns epiphany.infra.adapters.mongo-test
  "Integration tests for the MongoDB observations adapter.
   Requires a running MongoDB instance (localhost:27017).
   Tagged ^:integration so they only run with the :integration profile."
  (:require [clojure.test :refer [deftest is use-fixtures testing]]
            [epiphany.domain.backup :as backup]
            [epiphany.infra.adapters.mongo :as mongo])
  (:import [org.bson Document]))

;; ---------------------------------------------------------------------------
;; Helpers

(defn- test-observation
  "Create a minimal valid repository-location observation."
  [overrides]
  (merge {:observation/type           :repository/location-observed
          :observation/request-id     #uuid "11111111-2222-3333-4444-555555555555"
          :observation/id             (java.util.UUID/randomUUID)
          :observation/observed-at    (java.util.Date.)
          :observation/adapter-version "0.1.0"
          :observation/schema-version 1
          :resource-id                (java.util.UUID/randomUUID)
          :repository/path            {:path/raw       "/home/err/spaces/epiphany"
                                       :path/source    :filesystem-argument
                                       :path/comparison :exact}
          :repository/common-git-dir  {:path/raw       "/home/err/spaces/epiphany/.git"
                                       :path/source    :git-tree-entry
                                       :path/comparison :exact}}
         overrides))

(def ^:private test-uri
  "MongoDB URI for integration tests. Credentials and explicit localhost
   opt-in must come from the environment."
  (or (System/getenv "EPIPHANY_TEST_MONGODB_URI")
      (System/getenv "MONGODB_URI")))

(def ^:private conn (atom nil))

(defn- setup-db!
  "Connect to test database and clean collections."
  []
  (when-not @conn
    (reset! conn (mongo/connect! {:uri               test-uri
                                  :database          "openplanner"
                                  :collection-prefix "epiphany_test_"})))
  (mongo/clean-test-db! @conn)
  (mongo/ensure-indexes! @conn))

(defn- teardown-db!
  "Disconnect from test database."
  []
  (when @conn
    (mongo/disconnect! @conn)
    (reset! conn nil)))

(use-fixtures :each
  (fn [f]
    (if test-uri
      (try
        (setup-db!)
        (f)
        (finally
          (teardown-db!)))
      (binding [*out* *err*]
        (println "SKIP Mongo integration test: set EPIPHANY_TEST_MONGODB_URI")))))

;; ---------------------------------------------------------------------------
;; Tests

(deftest ^:integration insert-and-find-by-request-id
  (testing "Insert an observation and retrieve it by request-id"
    (let [obs-adapter (mongo/make-observations-adapter @conn)
          rid         #uuid "11111111-2222-3333-4444-555555555555"
          record      (test-observation {:observation/request-id rid})]
      ;; Should not exist yet
      (is (nil? ((:find-by-request-id obs-adapter) rid)))
      ;; Insert
      ((:record-repository-location! obs-adapter) record)
      ;; Retrieve
      (let [found ((:find-by-request-id obs-adapter) rid)]
        (is (some? found))
        (is (= rid (:observation/request-id found)))
        (is (= (:resource-id record) (:resource-id found)))
        (is (= (get-in record [:repository/path :path/raw])
               (get-in found [:repository/path :path/raw])))
        (is (= [found] (mongo/list-repository-locations @conn))
            "registration status reads the durable repository observations")))))

(deftest ^:integration idempotent-insert-returns-existing
  (testing "Inserting the same request-id twice is a stable no-op (nil), matching the reference adapter"
    (let [obs-adapter (mongo/make-observations-adapter @conn)
          rid         #uuid "22222222-3333-4444-5555-666666666666"
          record      (test-observation {:observation/request-id rid})]
      (is (nil? ((:record-repository-location! obs-adapter) record)))
      (is (nil? ((:record-repository-location! obs-adapter) record)))
      (is (= record ((:find-by-request-id obs-adapter) rid))))))

(deftest ^:integration idempotency-conflict-returns-shared-category
  (testing "Same request-id with different content returns {:code :idempotency-conflict}, matching the reference adapter"
    (let [obs-adapter (mongo/make-observations-adapter @conn)
          rid         #uuid "33333333-4444-5555-6666-777777777777"
          record-a    (test-observation {:observation/request-id rid
                                         :repository/path {:path/raw       "/path/a"
                                                           :path/source    :filesystem-argument
                                                           :path/comparison :exact}})
          record-b    (test-observation {:observation/request-id rid
                                         :repository/path {:path/raw       "/path/b"
                                                           :path/source    :filesystem-argument
                                                           :path/comparison :exact}})]
      ((:record-repository-location! obs-adapter) record-a)
      (let [result ((:record-repository-location! obs-adapter) record-b)]
        (is (= :idempotency-conflict (:code result)))
        (is (= rid (:request-id result))))
      (is (= record-a ((:find-by-request-id obs-adapter) rid))
          "the stored fact must remain the original"))))

(deftest ^:integration unicode-paths-preserved
  (testing "Unicode path strings are preserved byte-for-byte"
    (let [obs-adapter (mongo/make-observations-adapter @conn)
          rid         #uuid "44444444-5555-6666-7777-888888888888"
          record      (test-observation {:observation/request-id rid
                                         :repository/path {:path/raw       "/home/user/.ημ/notes"
                                                           :path/source    :filesystem-argument
                                                           :path/comparison :exact}
                                         :repository/common-git-dir {:path/raw       "/home/user/.ημ/notes/.git"
                                                                     :path/source    :git-tree-entry
                                                                     :path/comparison :exact}})]
      ((:record-repository-location! obs-adapter) record)
      (let [found ((:find-by-request-id obs-adapter) rid)]
        (is (= "/home/user/.ημ/notes"
               (get-in found [:repository/path :path/raw])))
        (is (= "/home/user/.ημ/notes/.git"
               (get-in found [:repository/common-git-dir :path/raw])))))))

(deftest ^:integration concurrent-inserts
  (testing "Concurrent inserts with different request-ids succeed"
    (let [obs-adapter (mongo/make-observations-adapter @conn)
          threads     (mapv (fn [i]
                              (let [rid (java.util.UUID/fromString
                                         (format "55555555-%04d-6666-7777-88888888%04d" i i))]
                                (future
                                  ((:record-repository-location! obs-adapter)
                                   (test-observation {:observation/request-id rid})))))
                            (range 10))
          results     (mapv deref threads)]
      (is (every? nil? results)))))

(deftest ^:integration find-nonexistent-returns-nil
  (testing "Looking up a nonexistent request-id returns nil"
    (let [obs-adapter (mongo/make-observations-adapter @conn)]
      (is (nil? ((:find-by-request-id obs-adapter) #uuid "99999999-0000-1111-2222-333333333333"))))))

(deftest ^:integration validates-schema
  (testing "Invalid observation is rejected with the shared schema-validation category"
    (let [obs-adapter (mongo/make-observations-adapter @conn)]
      (is (thrown-with-msg? clojure.lang.ExceptionInfo
                             #"Schema validation failed for :record-repository-location!"
                             ((:record-repository-location! obs-adapter)
                              {:observation/type :repository/location-observed
                               ;; missing required fields
                               }))))))

;; ---------------------------------------------------------------------------
;; Ingestion run adapter tests

(deftest ^:integration record-and-find-ingestion-run
  (testing "Record an ingestion run and retrieve it"
    (let [obs-adapter (mongo/make-observations-adapter @conn)
          rid         #uuid "66666666-7777-8888-9999-aaaaaaaaaaaa"
          run-id      #uuid "77777777-8888-9999-aaaa-bbbbbbbbbbbb"
          record      {:observation/type           :ingestion/run-completed
                       :observation/request-id     run-id
                       :observation/id             rid
                       :observation/observed-at    (java.util.Date.)
                       :observation/adapter-version "0.1.0"
                       :observation/schema-version 1
                       :resource-id                #uuid "aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeeee"
                       :ingestion/repo-path        {:path/raw       "/repo"
                                                     :path/source    :filesystem-argument
                                                     :path/comparison :exact}
                       :ingestion/selected-refs    ["refs/heads/main"]
                       :ingestion/commit-count     42
                       :ingestion/failure-count    1
                       :ingestion/failures         [{:failure/reason "object-unreadable"
                                                      :failure/message "boom"}]}]
      ((:record-ingestion-run! obs-adapter) record)
      ((:record-ingestion-run! obs-adapter) record)
      ;; Verify the run was recorded (find by _id)
      (let [coll (:ingestion-run-collection @conn)
            doc (-> (.find coll)
                    (.filter (Document. "_id" (str rid)))
                    (.first))]
        (is (some? doc))
        (is (= 42 (.getLong doc "commit_count")))
        (is (= 1 (.getLong doc "failure_count")))
        (is (= 1 (.countDocuments coll (Document. "_id" (str rid))))
            "a retried run record is not duplicated")))))

(deftest ^:integration record-checkpoint
  (testing "Record a projection checkpoint"
    (let [obs-adapter (mongo/make-observations-adapter @conn)
          rid         #uuid "88888888-9999-aaaa-bbb-cccccccccccc"
          run-id      #uuid "77777777-8888-9999-aaaa-bbbbbbbbbbbb"
          record      {:observation/type            :projection/checkpoint-recorded
                       :observation/request-id      rid
                       :observation/id              #uuid "99999999-0000-1111-2222-333333333333"
                       :observation/observed-at     (java.util.Date.)
                       :observation/adapter-version "0.1.0"
                       :observation/schema-version  1
                       :resource-id                 #uuid "aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeeee"
                       :checkpoint/projection-name  "revision-at-path"
                       :checkpoint/projection-version 1
                       :checkpoint/ingestion-run-id run-id
                       :checkpoint/status           :completed
                       :checkpoint/processed-count  42}]
      ((:record-checkpoint! obs-adapter) record)
      ((:record-checkpoint! obs-adapter) record)
      (let [coll (:projection-checkpoint-collection @conn)
            doc (-> (.find coll)
                    (.filter (Document. "projection_name" "revision-at-path"))
                    (.first))]
        (is (some? doc))
        (is (= 42 (.getLong doc "processed_count")))
        (is (= "completed" (.getString doc "status")))
        (is (= 1 (.countDocuments coll (Document. "_id" (str (:observation/id record)))))
            "a retried checkpoint is not duplicated")))))

(deftest ^:integration checkpoint-without-request-id-round-trips
  (testing "an absent optional request id remains absent when decoded"
    (let [obs-adapter (mongo/make-observations-adapter @conn)
          run-id (random-uuid)
          record {:observation/type :projection/checkpoint-recorded
                  :observation/id (random-uuid)
                  :observation/observed-at (java.util.Date.)
                  :observation/adapter-version "0.1.0"
                  :observation/schema-version 1
                  :resource-id (random-uuid)
                  :checkpoint/projection-name "section-extraction"
                  :checkpoint/projection-version 1
                  :checkpoint/ingestion-run-id run-id
                  :checkpoint/status :completed
                  :checkpoint/processed-count 1}]
      ((:record-checkpoint! obs-adapter) record)
      (let [decoded (first ((:list-checkpoints obs-adapter) run-id))]
        (is (= record decoded))
        (is (not (contains? decoded :observation/request-id)))))))

;; ---------------------------------------------------------------------------
;; ENG-017F: decode integrity — malformed stored documents are named
;; integrity findings, never silently omitted or returned as empty.

(defn- raw-location-doc
  "A repository-location document built by hand (bypassing adapter
   validation) with the given schema_version value."
  [request-id schema-version]
  (doto (Document.)
    (.put "_id" (str (random-uuid)))
    (.put "observation_type" "repository/location-observed")
    (.put "request_id" (str request-id))
    (.put "observation_id" (str (random-uuid)))
    (.put "observed_at" (java.util.Date.))
    (.put "adapter_version" "0.1.0")
    (.put "schema_version" schema-version)
    (.put "resource_id" (str (random-uuid)))
    (.put "repository_path" "/x")
    (.put "repository_path_source" "filesystem-argument")
    (.put "common_git_dir" "/x/.git")
    (.put "common_git_dir_source" "filesystem-argument")))

(deftest ^:integration malformed-stored-doc-is-integrity-corrupt-not-empty
  (testing "a stored doc that cannot be decoded is :integrity/corrupt, never []"
    (let [rid (random-uuid)
          coll (:repository-location-collection @conn)]
      (.insertOne coll (raw-location-doc rid "not-a-number"))
      (try
        ((:find-by-request-id (mongo/make-observations-adapter @conn)) rid)
        (is false "expected an integrity failure, got a clean result")
        (catch clojure.lang.ExceptionInfo e
          (is (= :integrity/corrupt (:code (ex-data e)))))))))

(deftest ^:integration future-version-stored-doc-is-unsupported-version
  (testing "a stored doc claiming an unknown future version is :integrity/unsupported-version, never decoded as nearest-known"
    (let [rid (random-uuid)
          coll (:repository-location-collection @conn)]
      (.insertOne coll (raw-location-doc rid (long 99)))
      (try
        ((:find-by-request-id (mongo/make-observations-adapter @conn)) rid)
        (is false "expected an integrity failure, got a clean result")
        (catch clojure.lang.ExceptionInfo e
          (is (= :integrity/unsupported-version (:code (ex-data e)))))))))

(deftest ^:integration export-import-export-round-trip-on-mongo
  (testing "canonical export -> clear -> import -> export preserves the manifest"
    (let [obs-adapter (mongo/make-observations-adapter @conn)
          rid (random-uuid)
          record (test-observation {:observation/request-id rid})
          file1 (str (java.nio.file.Files/createTempFile "epiphany-mongo-backup" ".edn"
                                                         (make-array java.nio.file.attribute.FileAttribute 0)))
          file2 (str file1 ".re")]
      ((:record-repository-location! obs-adapter) record)
      (let [exported (backup/export-to-file obs-adapter file1)]
        (mongo/clean-test-db! @conn)
        (backup/import-from-file obs-adapter file1)
        (let [re-exported (backup/export-to-file obs-adapter file2)]
          (is (= (:manifest exported) (:manifest re-exported))
              "round trip preserves the canonical manifest"))))))

(defn- index-names
  [collection]
  (set (map #(.getString ^Document % "name")
            (.into (.listIndexes collection) (java.util.ArrayList.)))))

(deftest ^:integration clear-all-recreates-idempotency-indexes
  (testing "restore clearing leaves every request-id uniqueness index active"
    (let [obs-adapter (mongo/make-observations-adapter @conn)]
      ((:clear-all! obs-adapter))
      (doseq [collection [(:repository-location-collection @conn)
                          (:review-decision-collection @conn)
                          (:lineage-candidate-collection @conn)]]
        (is (contains? (index-names collection) "request_id_unique_v1"))))))
