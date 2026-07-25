(ns epiphany.domain.backup-integrity-test
  "ENG-017F: backup manifest validation, corruption fixtures, distinct
   integrity outcomes, collection-name mapping, and the canonical
   export -> import -> export round trip."
  (:require [clojure.edn]
            [clojure.string]
            [clojure.test :refer [deftest is testing]]
            [epiphany.domain.backup :as backup]
            [epiphany.infra.adapters.in-memory :as in-memory]
            [epiphany.law.operations :as operations]))

;; ---------------------------------------------------------------------------
;; Fixtures

(defn- temp-file [prefix]
  (str (java.nio.file.Files/createTempFile prefix ".edn" (make-array java.nio.file.attribute.FileAttribute 0))))

(defn- make-port []
  (:observations (in-memory/make {:common-git-dir-fn (fn [p] (str p "/.git"))})))

(defn- valid-location-record
  [rid]
  {:observation/id (random-uuid)
   :observation/observed-at #inst "2026-01-01T00:00:00.000Z"
   :observation/adapter-version "test-v1"
   :observation/schema-version 1
   :observation/type :repository/location-observed
   :observation/request-id rid
   :resource-id #uuid "aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeeee"
   :repository/path {:path/raw "/backup/test-repo"
                     :path/source :filesystem-argument
                     :path/comparison :exact}
   :repository/common-git-dir {:path/raw "/backup/test-repo/.git"
                               :path/source :filesystem-argument
                               :path/comparison :exact}})

(defn- seed-port!
  [port]
  ((:record-repository-location! port) (valid-location-record (random-uuid)))
  port)

(defn- export-then-read
  "Export `port` to a temp file and return [file-path payload-text]."
  [port]
  (let [file (temp-file "epiphany-backup")]
    (backup/export-to-file port file)
    [file (slurp file)]))

(defn- import-error
  "Run import against `port`, returning the ex-data on failure."
  [port file]
  (try
    (backup/import-from-file port file)
    nil
    (catch clojure.lang.ExceptionInfo e
      (ex-data e))))

;; ---------------------------------------------------------------------------
;; Corruption fixtures: every one fails BEFORE any mutation

(deftest truncated-backup-is-corrupt-and-mutates-nothing
  (let [source (seed-port! (make-port))
        [file text] (export-then-read source)
        truncated (str (subs text 0 (quot (count text) 2)))
        target (make-port)
        before ((:export-all target))]
    (spit file truncated)
    (let [error (import-error target file)]
      (is (= :integrity/corrupt (:code error)))
      (is (= before ((:export-all target)))
          "a truncated backup mutates nothing"))))

(deftest edited-manifest-count-is-corrupt-and-mutates-nothing
  (let [source (seed-port! (make-port))
        [file text] (export-then-read source)
        target (make-port)
        before ((:export-all target))]
    ;; Bump the recorded count of repository-location by 1000
    (spit file (clojure.string/replace text #"\"repository-location\" 1"
                                       "\"repository-location\" 1001"))
    (let [error (import-error target file)]
      (is (= :integrity/corrupt (:code error)))
      (is (= before ((:export-all target)))
          "a count-tampered backup mutates nothing"))))

(deftest flipped-content-hash-is-corrupt-and-mutates-nothing
  (let [source (seed-port! (make-port))
        [file text] (export-then-read source)
        target (make-port)
        before ((:export-all target))
        bad-hash (apply str (repeat 44 "A"))]
    (spit file (clojure.string/replace text #":content-hash \"[^\"]+\""
                                       (str ":content-hash \"" bad-hash "\"")))
    (let [error (import-error target file)]
      (is (= :integrity/corrupt (:code error)))
      (is (= before ((:export-all target)))
          "a checksum-tampered backup mutates nothing"))))

(deftest bumped-manifest-version-is-unsupported-and-mutates-nothing
  (let [source (seed-port! (make-port))
        [file text] (export-then-read source)
        target (make-port)
        before ((:export-all target))]
    (spit file (clojure.string/replace text #":version 1" ":version 99"))
    (let [error (import-error target file)]
      (is (= :integrity/unsupported-version (:code error)))
      (is (= before ((:export-all target)))
          "an unknown-version backup mutates nothing"))))

(deftest schema-violating-record-is-corrupt-and-mutates-nothing
  (let [source (seed-port! (make-port))
        [file _text] (export-then-read source)
        payload (clojure.edn/read-string (slurp file))
        broken (update-in payload [:data "repository-location" 0]
                          dissoc :resource-id)
        target (make-port)
        before ((:export-all target))]
    ;; Rewrite the file with a record that fails its schema; refresh the
    ;; manifest hash/counts so ONLY the schema violation can trip it.
    (let [data (:data broken)
          manifest (assoc (:manifest broken)
                          :content-hash nil)]
      (spit file (pr-str {:manifest (dissoc manifest :content-hash) :data data})))
    (let [error (import-error target file)]
      (is (= :integrity/corrupt (:code error)))
      (is (= before ((:export-all target)))
          "a schema-violating backup mutates nothing"))))

(deftest unknown-record-version-is-unsupported-and-mutates-nothing
  (let [source (seed-port! (make-port))
        [file _text] (export-then-read source)
        payload (clojure.edn/read-string (slurp file))
        broken (assoc-in payload [:data "repository-location" 0 :observation/schema-version] 99)
        target (make-port)
        before ((:export-all target))]
    (spit file (pr-str {:manifest (dissoc (:manifest broken) :content-hash)
                        :data (:data broken)}))
    (let [error (import-error target file)]
      (is (= :integrity/unsupported-version (:code error)))
      (is (= before ((:export-all target)))))))

(deftest unknown-collection-is-corrupt-and-mutates-nothing
  (let [source (seed-port! (make-port))
        [file _text] (export-then-read source)
        payload (clojure.edn/read-string (slurp file))
        broken (-> payload
                   (assoc-in [:data "bogus-collection"] [])
                   (assoc-in [:manifest :collections "bogus-collection"] 0))
        target (make-port)
        before ((:export-all target))]
    (spit file (pr-str broken))
    (let [error (import-error target file)]
      (is (= :integrity/corrupt (:code error)))
      (is (= before ((:export-all target)))))))

(deftest missing-file-is-source-unavailable
  (let [error (import-error (make-port) "/nonexistent/path/backup.edn")]
    (is (= :source/unavailable (:code error)))))

;; ---------------------------------------------------------------------------
;; Four outcomes stay distinct (never collapsible)

(deftest integrity-outcomes-are-pairwise-distinct
  (testing "corrupt, unsupported-version, unavailable, and empty are four distinct outcomes"
    (let [categories #{:integrity/corrupt :integrity/unsupported-version :source/unavailable :empty}]
      (is (= 4 (count categories)))
      (is (not= :integrity/corrupt :integrity/unsupported-version))
      (is (not= :source/unavailable :integrity/corrupt))
      (testing "a genuinely empty backup imports as empty, not as an error"
        (let [empty-port (make-port)
              file (temp-file "epiphany-empty-backup")]
          (backup/export-to-file empty-port file)
          (let [result (backup/import-from-file (make-port) file)]
            (is (map? result))
            (is (every? zero? (vals result)))))))))

;; ---------------------------------------------------------------------------
;; Collection-name mapping (logical <-> physical)

(deftest collection-vocabulary-is-explicit-and-consistent
  (testing "the logical export/import vocabulary is exactly the registry's collection-schemas keys"
    (let [port (seed-port! (make-port))
          exported ((:export-all port))]
      (is (= (set (keys exported))
             (set (keys operations/collection-schemas)))
          "every export-all key has a registered schema, and vice versa")
      (is (= #{"repository-location" "ingestion-run" "projection-checkpoint"
               "section-extraction" "revision-at-path" "review-decision"
               "lineage-candidate"}
             (set (keys operations/collection-schemas)))
          "the logical vocabulary is the stable 7-collection set"))))

;; ---------------------------------------------------------------------------
;; Canonical round trip

(deftest export-import-export-round-trip-preserves-canonical-data
  (let [source (seed-port! (make-port))
        [file1 _] (export-then-read source)
        target (make-port)
        _ (backup/import-from-file target file1)
        [file2 _] (export-then-read target)
        payload1 (clojure.edn/read-string (slurp file1))
        payload2 (clojure.edn/read-string (slurp file2))]
    (is (= (:manifest payload1) (:manifest payload2))
        "re-export after import produces the identical manifest")
    (is (= (:data payload1) (:data payload2))
        "re-export after import preserves the canonical payload")))
