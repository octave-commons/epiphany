(ns epiphany.infra.adapters.mongo-test
  "Integration tests for the MongoDB observations adapter.
   Requires a running MongoDB instance (localhost:27017).
   Tagged ^:integration so they only run with the :integration profile."
  (:require [clojure.test :refer [deftest is use-fixtures testing]]
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
  "MongoDB URI with authentication for integration tests."
  "mongodb://openplanner:GamG7Ly2g7eyMJoIa-4zS17eAUlWiUup@127.0.0.1:27017/openplanner?authSource=openplanner")

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
    (try
      (setup-db!)
      (f)
      (finally
        (teardown-db!)))))

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
               (get-in found [:repository/path :path/raw])))))))

(deftest ^:integration idempotent-insert-returns-existing
  (testing "Inserting the same request-id twice returns :idempotent"
    (let [obs-adapter (mongo/make-observations-adapter @conn)
          rid         #uuid "22222222-3333-4444-5555-666666666666"
          record      (test-observation {:observation/request-id rid})]
      (is (= :inserted ((:record-repository-location! obs-adapter) record)))
      (is (= :idempotent ((:record-repository-location! obs-adapter) record)))
      (is (= record ((:find-by-request-id obs-adapter) rid))))))

(deftest ^:integration idempotency-conflict-throws
  (testing "Same request-id with different content throws :idempotency/conflict"
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
      (is (thrown-with-msg? clojure.lang.ExceptionInfo
                            #"Idempotency conflict"
                            ((:record-repository-location! obs-adapter) record-b))))))

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
      (is (every? #{:inserted} results)))))

(deftest ^:integration find-nonexistent-returns-nil
  (testing "Looking up a nonexistent request-id returns nil"
    (let [obs-adapter (mongo/make-observations-adapter @conn)]
      (is (nil? ((:find-by-request-id obs-adapter) #uuid "99999999-0000-1111-2222-333333333333"))))))

(deftest ^:integration validates-schema
  (testing "Invalid observation is rejected with schema error"
    (let [obs-adapter (mongo/make-observations-adapter @conn)]
      (is (thrown-with-msg? clojure.lang.ExceptionInfo
                             #"Invalid repository-location observation"
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
      ;; Verify the run was recorded (find by _id)
      (let [coll (:ingestion-run-collection @conn)
            doc (-> (.find coll)
                    (.filter (Document. "_id" (str rid)))
                    (.first))]
        (is (some? doc))
        (is (= 42 (.getLong doc "commit_count")))
        (is (= 1 (.getLong doc "failure_count")))))))

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
      (let [coll (:projection-checkpoint-collection @conn)
            doc (-> (.find coll)
                    (.filter (Document. "projection_name" "revision-at-path"))
                    (.first))]
        (is (some? doc))
        (is (= 42 (.getLong doc "processed_count")))
        (is (= "completed" (.getString doc "status")))))))
