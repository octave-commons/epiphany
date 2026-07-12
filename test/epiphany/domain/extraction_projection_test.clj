(ns epiphany.domain.extraction-projection-test
  (:require [clojure.test :refer [deftest testing is]]
            [epiphany.domain.extraction-projection :as ep]
            [epiphany.domain.ingestion :as ingestion]))

;; ---------------------------------------------------------------------------
;; Test fixtures

(def sample-markdown "# Title\n\nSome preamble text.\n\n## Section One\n\nFirst section body.\n\n## Section Two\n\nSecond section body.\n")

(defn- make-mock-git-port [blobs]
  {:read-blob (fn [_ oid]
                (if-let [content (get blobs oid)]
                  {:blob/oid oid
                   :blob/content content
                   :blob/size (count content)
                   :blob/failure nil}
                  {:blob/oid oid
                   :blob/content nil
                   :blob/size 0
                   :blob/failure {:failure/oid oid
                                  :failure/reason "blob-not-found"
                                  :failure/message (str "Not found: " oid)}}))})

(defn- make-mock-observations []
  (let [revisions (atom [])
        extractions (atom [])
        checkpoints (atom [])]
    {:record-revision-at-path! (fn [obs] (swap! revisions conj obs) nil)
     :record-section-extraction! (fn [obs] (swap! extractions conj obs) nil)
     :record-checkpoint! (fn [obs] (swap! checkpoints conj obs) nil)
     :list-revision-at-path-by-resource (fn [resource-id]
                                          (filterv #(= resource-id (:resource-id %))
                                                   @revisions))
     :list-section-extractions-by-revision (fn [revision-id]
                                             (filterv #(= revision-id
                                                          (:extraction/revision-at-path-id %))
                                                      @extractions))
     :list-checkpoints (fn [run-id]
                         (filterv #(= run-id (:checkpoint/ingestion-run-id %))
                                  @checkpoints))}))

(defn- make-mock-index []
  (let [docs (atom [])]
    {:index-sections! (fn [record] (swap! docs conj record) nil)}))

(defn- make-revision [resource-id commit-oid path blob-oid & {:keys [id] :or {id (java.util.UUID/randomUUID)}}]
  {:observation/type :revision/at-path-observed
   :observation/id (java.util.UUID/randomUUID)
   :observation/observed-at (java.util.Date.)
   :observation/adapter-version "test"
   :observation/schema-version 1
   :resource-id resource-id
   :revision-at-path/id id
   :revision/commit-oid commit-oid
   :revision/tree-oid "tree000"
   :revision/path-raw path
   :revision/blob-oid blob-oid
   :revision/mode 33188
   :revision/evidence :initial})

;; ---------------------------------------------------------------------------
;; Tests

(deftest extract-revision-success
  (testing "extract sections from a single revision"
    (let [ports {:git (make-mock-git-port {"blob1" sample-markdown})
                 :observations (make-mock-observations)
                 :index (make-mock-index)}
          revision (make-revision #uuid "00000000-0000-0000-0000-000000000001"
                                   "commit1" "doc.md" "blob1")
          result (ep/extract-revision ports revision)]
      (is (some? (:extraction/record result)))
      (is (nil? (:extraction/error result)))
      (is (= (:revision-at-path/id revision) (:extraction/revision-id result)))
      (let [obs (:extraction/record result)]
        (is (= :section/extraction-completed (:observation/type obs)))
        (is (= #uuid "00000000-0000-0000-0000-000000000001" (:resource-id obs)))
        (is (pos? (:extraction/section-count obs)))
        (is (= "doc.md" (:extraction/path-raw obs)))))))

(deftest extract-revision-blob-error
  (testing "handle blob read failure gracefully"
    (let [ports {:git (make-mock-git-port {})
                 :observations (make-mock-observations)
                 :index (make-mock-index)}
          revision (make-revision #uuid "00000000-0000-0000-0000-000000000001"
                                   "commit1" "doc.md" "missing-blob")
          result (ep/extract-revision ports revision)]
      (is (nil? (:extraction/record result)))
      (is (some? (:extraction/error result)))
      (is (string? (get-in result [:extraction/error :failure/message]))))))

(deftest run-extraction-projection-full
  (testing "full projection run over multiple revisions"
    (let [resource-id #uuid "00000000-0000-0000-0000-000000000001"
          run-id #uuid "00000000-0000-0000-0000-000000000002"
          obs (make-mock-observations)
          ports {:git (make-mock-git-port {"blob1" sample-markdown
                                            "blob2" "# Other\n\nOther content.\n"})
                 :observations obs
                 :index (make-mock-index)}
          ;; Record two revisions
          rev1 (make-revision resource-id "commit1" "doc1.md" "blob1")
          rev2 (make-revision resource-id "commit2" "doc2.md" "blob2")]
      ((:record-revision-at-path! obs) rev1)
      ((:record-revision-at-path! obs) rev2)
      (let [result (ep/run-extraction-projection
                    ports {:resource-id resource-id
                           :ingestion-run-id run-id
                           :repository-path "/test/repo"})]
        (is (= 2 (:projection/revisions-scanned result)))
        (is (= 2 (:projection/sections-extracted result)))
        (is (true? (:projection/completed result)))
        (is (empty? (:projection/failures result)))))))

(deftest run-extraction-projection-idempotent
  (testing "re-running projection skips already-extracted revisions"
    (let [resource-id #uuid "00000000-0000-0000-0000-000000000001"
          run-id #uuid "00000000-0000-0000-0000-000000000002"
          obs (make-mock-observations)
          ports {:git (make-mock-git-port {"blob1" sample-markdown})
                 :observations obs
                 :index (make-mock-index)}
          rev1 (make-revision resource-id "commit1" "doc1.md" "blob1")]
      ((:record-revision-at-path! obs) rev1)
      ;; First run
      (ep/run-extraction-projection
       ports {:resource-id resource-id
              :ingestion-run-id run-id
              :repository-path "/test/repo"})
      ;; Second run — should extract nothing
      (let [result (ep/run-extraction-projection
                    ports {:resource-id resource-id
                           :ingestion-run-id run-id
                           :repository-path "/test/repo"})]
        (is (= 1 (:projection/revisions-scanned result)))
        (is (= 0 (:projection/sections-extracted result)))
        (is (true? (:projection/completed result)))))))

(deftest run-extraction-projection-checkpointing
  (testing "checkpoints are recorded during projection"
    (binding [ep/checkpoint-interval 2]
      (let [resource-id #uuid "00000000-0000-0000-0000-000000000001"
            run-id #uuid "00000000-0000-0000-0000-000000000002"
            obs (make-mock-observations)
            ports {:git (make-mock-git-port {"blob1" sample-markdown
                                              "blob2" sample-markdown
                                              "blob3" sample-markdown})
                   :observations obs
                   :index (make-mock-index)}]
        ;; Record 3 revisions
        (doseq [i (range 3)]
          ((:record-revision-at-path! obs)
           (make-revision resource-id (str "commit" i) (str "doc" i ".md") (str "blob" (inc i)))))
        (let [result (ep/run-extraction-projection
                      ports {:resource-id resource-id
                             :ingestion-run-id run-id
                             :repository-path "/test/repo"})
              checkpoints ((:list-checkpoints obs) run-id)]
          (is (= 3 (:projection/sections-extracted result)))
          (is (pos? (count checkpoints)))
          (is (some #(= :completed (:checkpoint/status %)) checkpoints)))))))

(deftest run-extraction-projection-mixed-success-failure
  (testing "projection handles mix of success and failure"
    (let [resource-id #uuid "00000000-0000-0000-0000-000000000001"
          run-id #uuid "00000000-0000-0000-0000-000000000002"
          obs (make-mock-observations)
          ports {:git (make-mock-git-port {"blob1" sample-markdown})
                 :observations obs
                 :index (make-mock-index)}
          rev1 (make-revision resource-id "commit1" "doc1.md" "blob1")
          rev2 (make-revision resource-id "commit2" "doc2.md" "missing")]
      ((:record-revision-at-path! obs) rev1)
      ((:record-revision-at-path! obs) rev2)
      (let [result (ep/run-extraction-projection
                    ports {:resource-id resource-id
                           :ingestion-run-id run-id
                           :repository-path "/test/repo"})]
        (is (= 2 (:projection/revisions-scanned result)))
        (is (= 1 (:projection/sections-extracted result)))
        (is (= 1 (count (:projection/failures result))))
        (is (true? (:projection/completed result)))))))

(deftest run-extraction-projection-indexes
  (testing "extraction records are fed to the index"
    (let [resource-id #uuid "00000000-0000-0000-0000-000000000001"
          run-id #uuid "00000000-0000-0000-0000-000000000002"
          index-docs (atom [])
          obs (make-mock-observations)
          ports {:git (make-mock-git-port {"blob1" sample-markdown})
                 :observations obs
                 :index {:index-sections! (fn [record] (swap! index-docs conj record) nil)}}
          rev1 (make-revision resource-id "commit1" "doc1.md" "blob1")]
      ((:record-revision-at-path! obs) rev1)
      (ep/run-extraction-projection
       ports {:resource-id resource-id
              :ingestion-run-id run-id
              :repository-path "/test/repo"})
      (is (= 1 (count @index-docs)))
      (is (= :section/extraction-completed (:observation/type (first @index-docs)))))))
