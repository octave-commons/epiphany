(ns epiphany.domain.backup-test
  (:require [clojure.test :refer [deftest is testing use-fixtures]]
            [clojure.java.io :as io]
            [epiphany.domain.backup :as backup]
            [epiphany.infra.adapters.in-memory :as in-memory]))

(def ^:private test-dir (str (System/getProperty "java.io.tmpdir") "/epiphany-backup-test-" (System/currentTimeMillis)))

(defn- test-adapters []
  (in-memory/make {:common-git-dir-fn (fn [path] (str path "/.git"))}))

(defn- test-observation [id request-id]
  {:observation/type :repository/location-observed
   :observation/request-id request-id
   :observation/id id
   :observation/observed-at (java.util.Date.)
   :observation/adapter-version "test-v1"
   :observation/schema-version 1
   :resource-id (random-uuid)
   :repository/path {:path/raw "/test/repo" :path/source :filesystem-argument :path/comparison :exact}
   :repository/common-git-dir {:path/raw "/test/repo/.git" :path/source :filesystem-argument :path/comparison :exact}})

(defn- test-ingestion-run [id resource-id]
  {:observation/type :ingestion/run-completed
   :observation/request-id (random-uuid)
   :observation/id id
   :observation/observed-at (java.util.Date.)
   :observation/adapter-version "test-v1"
   :observation/schema-version 1
   :resource-id resource-id
   :ingestion/repo-path {:path/raw "/test/repo" :path/source :filesystem-argument :path/comparison :exact}
   :ingestion/selected-refs ["refs/heads/main"]
   :ingestion/commit-count 10
   :ingestion/failure-count 0
   :ingestion/failures []})

;; ---------------------------------------------------------------------------
;; export/import round-trip

(deftest export-import-round-trip
  (testing "export then import preserves all data"
    (let [adapters (test-adapters)
          obs (:observations adapters)
          rid (random-uuid)
          id1 (random-uuid)
          id2 (random-uuid)
          obs1 (test-observation id1 (random-uuid))
          obs2 (test-observation id2 (random-uuid))
          run1 (test-ingestion-run (random-uuid) rid)]

      ;; Record some data
      ((:record-repository-location! obs) obs1)
      ((:record-repository-location! obs) obs2)
      ((:record-ingestion-run! obs) run1)

      ;; Export
      (let [export-data ((:export-all obs))]
        (is (= 2 (count (get export-data "repository-location"))))
        (is (= 1 (count (get export-data "ingestion-run"))))

        ;; Create fresh adapters and import
        (let [adapters2 (test-adapters)
              obs2 (:observations adapters2)]
          ;; Import the export data
          ((:import-all obs2) export-data)

          ;; Verify import
          (let [imported ((:export-all obs2))]
            (is (= 2 (count (get imported "repository-location"))))
            (is (= 1 (count (get imported "ingestion-run"))))))))))

(deftest export-empty-db
  (testing "export of empty db returns empty collections"
    (let [adapters (test-adapters)
          obs (:observations adapters)
          export-data ((:export-all obs))]
      (is (= 0 (count (get export-data "repository-location"))))
      (is (= 0 (count (get export-data "ingestion-run"))))
      (is (= 0 (count (get export-data "projection-checkpoint"))))
      (is (= 0 (count (get export-data "section-extraction")))))))

;; ---------------------------------------------------------------------------
;; file export/import

(deftest export-to-file-and-import
  (testing "file-based export/import round-trips"
    (let [adapters (test-adapters)
          obs (:observations adapters)
          rid (random-uuid)
          id1 (random-uuid)
          id2 (random-uuid)
          obs1 (test-observation id1 (random-uuid))
          obs2 (test-observation id2 (random-uuid))
          run1 (test-ingestion-run (random-uuid) rid)
          backup-file (str test-dir "/test-backup.edn")]

      ;; Record data
      ((:record-repository-location! obs) obs1)
      ((:record-repository-location! obs) obs2)
      ((:record-ingestion-run! obs) run1)

      ;; Export to file
      (let [result (backup/export-to-file obs backup-file)]
        (is (= 3 (:total-docs result)))
        (is (= 2 (get-in result [:collection-counts "repository-location"])))
        (is (= 1 (get-in result [:collection-counts "ingestion-run"])))
        (is (.exists (io/file backup-file))))

      ;; Import into fresh adapters
      (let [adapters2 (test-adapters)
            obs2 (:observations adapters2)
            import-result (backup/import-from-file obs2 backup-file)]
        (is (= 2 (get import-result "repository-location")))
        (is (= 1 (get import-result "ingestion-run")))))))

;; ---------------------------------------------------------------------------
;; inaccessible sources

(deftest inaccessible-sources-check
  (testing "detects inaccessible repository paths"
    (let [adapters (test-adapters)
          git (:git adapters)
          backup-data {"repository-location"
                       [{:repository/path {:path/raw "/nonexistent/repo"
                                           :path/source :user
                                           :path/comparison :exact}
                         :resource-id (random-uuid)}]}]
      (let [inaccessible (backup/inaccessible-sources git backup-data)]
        (is (= 1 (count inaccessible)))
        (is (= "/nonexistent/repo" (:path (first inaccessible))))))))
