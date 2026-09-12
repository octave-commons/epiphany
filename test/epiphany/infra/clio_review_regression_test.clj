(ns epiphany.infra.clio-review-regression-test
  (:require [clio.extern.jvm.fs :as fs]
            [clojure.test :refer [deftest is]]
            [epiphany.domain.hybrid-search :as search]
            [epiphany.domain.observation-admission :as admission]
            [epiphany.extern.clio-observations :as host]
            [epiphany.infra.adapters.clio :as clio]
            [epiphany.law.registry :as registry]
            [epiphany.law-suite.observations-laws :as laws]))

(defn- temporary-directory []
  (str (System/getProperty "java.io.tmpdir") "/epiphany-review-" (random-uuid)))

(defn- fixture [operation]
  ((get-in laws/op-fixtures [operation :make-valid]) (random-uuid)))

(deftest central-registry-validates-recursive-markdown
  (let [span {:span/start-byte 0 :span/end-byte 3 :span/start-line 1 :span/end-line 1}
        document {:doc/source-length 3
                  :doc/body [{:block/type :block-quote :block/span span
                              :block-children [{:block/type :paragraph :block/span span
                                                :paragraph/inlines []}]}]}]
    (is (registry/valid? "md/document" document))
    (is (not (registry/valid? "md/document" (assoc-in document [:doc/body 0 :block/type] :unknown))))))

(deftest configured-ledger-path-retains-parent-segments-and-unicode
  (let [supplied "/tmp/.ημ/child/../ledger"]
    (is (= supplied (host/directory-path supplied)))))

(deftest simultaneous-revision-observations-share-one-durable-identity
  (let [directory (temporary-directory)]
    (try
      (let [store (clio/open-store directory)
            ports (repeatedly 2 #(clio/make-observations-adapter (clio/open-store directory)))
            base (fixture :record-revision-at-path!)
            records [base (assoc base :observation/id (random-uuid) :revision-at-path/id (random-uuid))]
            start (promise)
            writes (mapv (fn [port record]
                           (future @start ((:record-revision-at-path! port) record))) ports records)]
        (deliver start true)
        (doseq [write writes] (is (nil? (deref write 10000 ::timeout))))
        (is (= 1 (count (clio/history store))))
        (is (= 1 (count ((:list-revision-at-path-by-resource (first ports)) (:resource-id base))))))
      (finally (fs/remove-tree! directory)))))

(deftest direct-retries-reject-changed-material-content-before-acknowledging
  (let [directory (temporary-directory)
        later #inst "2026-09-12T13:00:00.000Z"]
    (try
      (let [store (clio/open-store directory)
            port (clio/make-observations-adapter store)]
        (doseq [[operation generated material-field changed]
                [[:record-repository-location! {:observation/id (random-uuid)} :resource-id (random-uuid)]
                 [:record-ingestion-run! {} :ingestion/commit-count 99]
                 [:record-checkpoint! {} :checkpoint/processed-count 99]
                 [:record-section-extraction! {} :extraction/extractor-version "changed"]
                 [:record-revision-at-path! {:observation/id (random-uuid) :revision-at-path/id (random-uuid)}
                  :revision/blob-oid (apply str (repeat 40 "c"))]
                 [:record-review-decision! {:observation/id (random-uuid) :review-decision/id (random-uuid)
                                            :review-decision/decided-at later} :review-decision/decision :rejected]
                 [:record-lineage-candidate! {:observation/id (random-uuid) :lineage-candidate/id (random-uuid)
                                              :lineage-candidate/generated-at later} :lineage-candidate/confidence 0.99]]]
          (let [record (fixture operation)
                retry (merge record generated {:observation/observed-at later})]
            ((get port operation) record)
            (let [accepted (fs/read-text (:file store))
                  snapshot ((:export-all port))]
              (is (nil? ((get port operation) retry)) (str operation " preserves the original accepted fact"))
              (is (thrown-with-msg? clojure.lang.ExceptionInfo #"different accepted content"
                                    ((get port operation) (assoc retry material-field changed)))
                  (str operation " must not silently acknowledge changed content"))
              (is (thrown? clojure.lang.ExceptionInfo
                           ((get port operation) (dissoc retry :resource-id)))
                  "Invalid retries must validate before duplicate filtering")
              (is (= accepted (fs/read-text (:file store))))
              (is (= snapshot ((:export-all (clio/make-observations-adapter (clio/open-store directory))))))))))
      (finally (fs/remove-tree! directory)))))

(deftest concurrent-extractions-share-logical-identity-across-command-ids
  (let [directory (temporary-directory)]
    (try
      (let [store (clio/open-store directory)
            ports (mapv (fn [_] (clio/make-observations-adapter (clio/open-store directory))) (range 2))
            base (fixture :record-section-extraction!)
            retry (assoc base :observation/id (random-uuid) :observation/request-id (random-uuid)
                         :observation/observed-at #inst "2026-09-12T13:00:00.000Z")
            start (promise)
            writes (mapv (fn [port record]
                           (future @start ((:record-section-extraction! port) record))) ports [base retry])]
        (deliver start true)
        (doseq [write writes] (is (nil? (deref write 10000 ::timeout))))
        (is (= 1 (count (clio/history store))))
        (is (= 1 (count ((:list-section-extractions-by-revision (first ports))
                         (:extraction/revision-at-path-id base)))))
        (let [reopened (clio/make-observations-adapter (clio/open-store directory))
              accepted (fs/read-text (:file store))
              imported (assoc retry :observation/id (random-uuid) :observation/request-id (random-uuid))]
          (is (nil? ((:import-all reopened) {"section-extraction" [imported]})))
          (is (= accepted (fs/read-text (:file store))))
          (is (thrown-with-msg? clojure.lang.ExceptionInfo #"different accepted content"
                                ((:record-section-extraction! reopened)
                                 (assoc imported :extraction/content-sha256 "changed-content"))))
          (is (= accepted (fs/read-text (:file store))))
          (is (nil? ((:record-section-extraction! reopened)
                     (assoc imported :observation/id (random-uuid) :observation/request-id (random-uuid)
                            :extraction/extractor-version "extractor-v2"))))
          (is (= 2 (count ((:list-section-extractions-by-revision reopened)
                           (:extraction/revision-at-path-id base)))))
          (is (= 2 (count (clio/history (clio/open-store directory)))))))
      (finally (fs/remove-tree! directory)))))

(deftest equivalent-historical-extractions-retain-all-accepted-envelope-identities
  (let [first-record (fixture :record-section-extraction!)
        duplicate (assoc first-record :observation/id (random-uuid) :observation/request-id (random-uuid))
        conflicting (assoc duplicate :observation/request-id (random-uuid)
                           :extraction/extractor-version "different-extractor")]
    (is (thrown-with-msg? clojure.lang.ExceptionInfo #"different accepted content"
                          (admission/missing-records "section-extraction" [first-record duplicate] [conflicting])))
    (is (thrown-with-msg? clojure.lang.ExceptionInfo #"different accepted content"
                          (admission/missing-records "section-extraction" [] [first-record duplicate conflicting])))))

(deftest repeated-bulk-import-is-a-durable-noop-for-every-append-collection
  (let [directory (temporary-directory)]
    (try
      (let [store (clio/open-store directory)
            port (clio/make-observations-adapter store)
            backup (into {} (map (fn [[collection operation]] [collection [(fixture operation)]]))
                         {"ingestion-run" :record-ingestion-run!
                          "projection-checkpoint" :record-checkpoint!
                          "section-extraction" :record-section-extraction!
                          "revision-at-path" :record-revision-at-path!})]
        ((:import-all port) backup)
        (let [snapshot ((:export-all port))
              accepted (fs/read-text (:file store))]
          ((:import-all port) backup)
          (is (= snapshot ((:export-all port))))
          (is (= accepted (fs/read-text (:file store))))
          (is (= snapshot ((:export-all (clio/make-observations-adapter (clio/open-store directory))))))))
      (finally (fs/remove-tree! directory)))))

(deftest hybrid-results-retain-distinct-repositories-and-commits
  (let [base {:result/path-raw "same.md" :result/heading-path ["Same"] :result/score 1.0}
        rows [(assoc base :resource-id "one" :result/commit-oid "old")
              (assoc base :resource-id "one" :result/commit-oid "new")
              (assoc base :resource-id "two" :result/commit-oid "new")]
        ports {:index {:search (constantly rows) :knn-search (constantly rows)}
               :embeddings {:embed-query (constantly [1.0])}}
        result (search/search ports {:query "same" :mode :hybrid :limit 10})]
    (is (= #{["one" "old"] ["one" "new"] ["two" "new"]}
           (set (map (juxt :resource-id :result/commit-oid) result))))))

(deftest logical-retry-cannot-acknowledge-an-unflushed-append
  (let [directory (temporary-directory)]
    (try
      (let [store (clio/open-store directory)
            port (clio/make-observations-adapter store)
            record (fixture :record-revision-at-path!)
            fail-force (fn [_] (throw (ex-info "Injected file force failure" {:fault :force})))
            write #((:record-revision-at-path! port) record)]
        (is (thrown-with-msg? clojure.lang.ExceptionInfo #"Injected file force failure"
                              (with-redefs [fs/force-file! fail-force] (write))))
        (is (= 1 (count (clio/history store))))
        (let [visible (fs/read-text (:file store))]
          (is (thrown-with-msg? clojure.lang.ExceptionInfo #"Injected file force failure"
                                (with-redefs [fs/force-file! fail-force] (write))))
          (is (= visible (fs/read-text (:file store))))
          (is (nil? (write)))
          (is (= visible (fs/read-text (:file store))))
          (is (= 1 (count (clio/history (clio/open-store directory)))))))
      (finally (fs/remove-tree! directory)))))

(deftest duplicate-admission-validates-content-before-acknowledging
  (let [directory (temporary-directory)]
    (try
      (let [store (clio/open-store directory)
            port (clio/make-observations-adapter store)
            record (fixture :record-revision-at-path!)]
        ((:record-revision-at-path! port) record)
        (let [visible (fs/read-text (:file store))]
          (is (thrown-with-msg? clojure.lang.ExceptionInfo #"different accepted content"
                                ((:record-revision-at-path! port)
                                 (assoc record :revision/blob-oid (apply str (repeat 40 "c"))))))
          (is (thrown? clojure.lang.ExceptionInfo
                       ((:import-all port) {"revision-at-path" [(dissoc record :revision/blob-oid)]})))
          (is (= visible (fs/read-text (:file store))))))
      (finally (fs/remove-tree! directory)))))

(deftest timestamped-record-retries-and-imports-preserve-accepted-facts
  (let [directory (temporary-directory)]
    (try
      (let [store (clio/open-store directory)
            port (clio/make-observations-adapter store)]
        (doseq [[collection operation material-field changed-value]
                [["ingestion-run" :record-ingestion-run! :ingestion/commit-count 99]
                 ["projection-checkpoint" :record-checkpoint! :checkpoint/processed-count 99]
                 ["section-extraction" :record-section-extraction! :extraction/extractor-version "changed"]]]
          (let [record (fixture operation)
                retry (assoc record :observation/observed-at #inst "2026-09-12T12:00:00.000Z")]
            ((get port operation) record)
            (let [before (fs/read-text (:file store))
                  snapshot ((:export-all port))]
              (is (nil? ((get port operation) retry)) "Direct observation-ID retries already keep the first fact")
              (is (nil? ((:import-all port) {collection [retry]})) "Import retries must also ignore observation time")
              (is (= before (fs/read-text (:file store))))
              (is (= snapshot ((:export-all port))))
              (is (thrown-with-msg? clojure.lang.ExceptionInfo #"different accepted content"
                                    ((:import-all port) {collection [(assoc retry material-field changed-value)]})))))))
      (finally (fs/remove-tree! directory)))))
