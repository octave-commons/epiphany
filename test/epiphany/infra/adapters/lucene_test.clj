(ns epiphany.infra.adapters.lucene-test
  (:require [clojure.test :refer [deftest is testing use-fixtures]]
            [clojure.java.io :as io]
            [epiphany.infra.adapters.lucene :as lucene]
            [epiphany.domain.section-extraction :as se]
            [epiphany.shape.markdown :as md])
  (:import [java.nio.file Files]
           [java.nio.file.attribute FileAttribute]))

(defn- temp-index-dir []
  (Files/createTempDirectory "lucene-test" (into-array FileAttribute [])))

(defn- make-test-record
  "Create a section extraction record from markdown source."
  [source path commit-oid blob-oid]
  (let [doc (md/parse source)
        sections (se/extract-sections doc)
        blob source]
    (se/make-extraction-record sections
                               #uuid "00000000-0000-0000-0000-000000000001"
                               commit-oid
                               path
                               blob-oid
                               blob
                               "test-v1")))

;; ---------------------------------------------------------------------------
;; index-sections! + search

(deftest index-and-search-basic-test
  (testing "index a section and find it by heading"
    (let [dir (temp-index-dir)
          adapter (lucene/make-index-adapter {:index-dir dir})
          record (make-test-record "# Hello World\n\nSome content."
                                   "docs/test.md" "abc123" "def456")]
      ((:index-sections! adapter) record)
      (let [results ((:search adapter) "Hello")]
        (is (seq results))
        (is (= "docs/test.md" (:result/path-raw (first results))))
        (is (= "abc123" (:result/commit-oid (first results))))
        (is (pos? (:result/score (first results))))))))

(deftest search-by-path-test
  (testing "search finds content by heading in file"
    (let [dir (temp-index-dir)
          adapter (lucene/make-index-adapter {:index-dir dir})
          record (make-test-record "# Architecture Overview\n\nBody text."
                                   "docs/architecture.md" "aaa" "bbb")]
      ((:index-sections! adapter) record)
      (let [results ((:search adapter) "Architecture")]
        (is (seq results))
        (is (= "docs/architecture.md" (:result/path-raw (first results))))))))

(deftest search-no-results-test
  (testing "search returns empty vector for no matches"
    (let [dir (temp-index-dir)
          adapter (lucene/make-index-adapter {:index-dir dir})
          record (make-test-record "# Alpha\n\nContent." "a.md" "x" "y")]
      ((:index-sections! adapter) record)
      (is (= [] ((:search adapter) "nonexistent"))))))

(deftest multiple-sections-test
  (testing "multiple sections from one file are indexed as separate docs"
    (let [dir (temp-index-dir)
          adapter (lucene/make-index-adapter {:index-dir dir})
          record (make-test-record "# First\n\nAlpha.\n\n# Second\n\nBeta."
                                   "doc.md" "c1" "b1")]
      ((:index-sections! adapter) record)
      (is (= 1 (count ((:search adapter) "First"))))
      (is (= 1 (count ((:search adapter) "Second")))))))

(deftest multiple-files-test
  (testing "sections from different files are indexed separately"
    (let [dir (temp-index-dir)
          adapter (lucene/make-index-adapter {:index-dir dir})
          r1 (make-test-record "# Alpha\n\nIn file A." "a.md" "c1" "b1")
          r2 (make-test-record "# Beta\n\nIn file B." "b.md" "c2" "b2")]
      ((:index-sections! adapter) r1)
      ((:index-sections! adapter) r2)
      (let [results-a ((:search adapter) "Alpha")
            results-b ((:search adapter) "Beta")]
        (is (= 1 (count results-a)))
        (is (= 1 (count results-b)))
        (is (= "a.md" (:result/path-raw (first results-a))))
        (is (= "b.md" (:result/path-raw (first results-b))))))))

;; ---------------------------------------------------------------------------
;; clear-index!

(deftest clear-index-test
  (testing "clearing index removes all documents"
    (let [dir (temp-index-dir)
          adapter (lucene/make-index-adapter {:index-dir dir})
          record (make-test-record "# X\n\nY." "f.md" "c" "b")]
      ((:index-sections! adapter) record)
      (is (seq ((:search adapter) "X")))
      ((:clear-index! adapter))
      (is (= [] ((:search adapter) "X"))))))

;; ---------------------------------------------------------------------------
;; rebuild-index!

(deftest rebuild-index-test
  (testing "rebuilding replaces old data"
    (let [dir (temp-index-dir)
          adapter (lucene/make-index-adapter {:index-dir dir})
          r1 (make-test-record "# Old\n\nStale." "old.md" "c1" "b1")
          r2 (make-test-record "# New\n\nFresh." "new.md" "c2" "b2")]
      ((:index-sections! adapter) r1)
      (is (seq ((:search adapter) "Old")))
      ((:rebuild-index! adapter) [r2])
      (is (= [] ((:search adapter) "Old")))
      (is (seq ((:search adapter) "New"))))))

;; ---------------------------------------------------------------------------
;; index-version

(deftest index-version-test
  (testing "version is set after indexing"
    (let [dir (temp-index-dir)
          adapter (lucene/make-index-adapter {:index-dir dir})]
      ;; Before indexing, version is 0
      (is (= 0 ((:index-version adapter))))
      ;; After indexing, version is set
      ((:index-sections! adapter) (make-test-record "# X\n\nY." "f.md" "c" "b"))
      (is (pos? ((:index-version adapter)))))))

(deftest version-cleared-after-clear-test
  (testing "version is removed after clear"
    (let [dir (temp-index-dir)
          adapter (lucene/make-index-adapter {:index-dir dir})]
      ((:index-sections! adapter) (make-test-record "# X\n\nY." "f.md" "c" "b"))
      (is (pos? ((:index-version adapter))))
      ((:clear-index! adapter))
      (is (= 0 ((:index-version adapter)))))))

;; ---------------------------------------------------------------------------
;; Unicode support

(deftest unicode-heading-test
  (testing "unicode headings are searchable"
    (let [dir (temp-index-dir)
          adapter (lucene/make-index-adapter {:index-dir dir})
          record (make-test-record "# .ημ\n\nContent.\n\n## Ελληνικά\n\nMore."
                                   "unicode.md" "u1" "u2")]
      ((:index-sections! adapter) record)
      ;; Search by Greek text
      (is (seq ((:search adapter) "ημ")))
      (is (seq ((:search adapter) "Ελληνικά"))))))

;; ---------------------------------------------------------------------------
;; Nested headings

(deftest nested-headings-test
  (testing "heading path is indexed as searchable text"
    (let [dir (temp-index-dir)
          adapter (lucene/make-index-adapter {:index-dir dir})
          record (make-test-record "# Top\n\nText.\n\n## Sub A\n\nSub.\n\n### Deep\n\nDeeper."
                                   "nested.md" "n1" "n2")]
      ((:index-sections! adapter) record)
      (is (seq ((:search adapter) "Top")))
      (is (seq ((:search adapter) "Sub A")))
      (is (seq ((:search adapter) "Deep"))))))

;; ---------------------------------------------------------------------------
;; Empty index search

(deftest empty-index-search-test
  (testing "searching empty index returns empty vector"
    (let [dir (temp-index-dir)
          adapter (lucene/make-index-adapter {:index-dir dir})]
      (is (= [] ((:search adapter) "anything"))))))
