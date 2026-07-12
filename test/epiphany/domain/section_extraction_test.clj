(ns epiphany.domain.section-extraction-test
  (:require [clojure.test :refer [deftest is testing]]
            [epiphany.domain.section-extraction :as se]
            [epiphany.shape.markdown :as md]))

(defn- parse [source]
  (md/parse source))

;; ---------------------------------------------------------------------------
;; extract-sections

(deftest simple-headings-test
  (testing "two top-level headings"
    (let [doc (parse "# First\n\nSome text.\n\n# Second\n\nMore text.")
          sections (se/extract-sections doc)]
      (is (= 2 (count sections)))
      (is (= ["First"] (:section/heading-path (first sections))))
      (is (= ["Second"] (:section/heading-path (second sections))))
      (is (= 1 (:section/level (first sections))))
      (is (= 0 (:section/ordinal (first sections))))
      (is (= 0 (:section/ordinal (second sections))))
      (is (= 1 (count (:section/body-blocks (first sections)))))
      (is (= 1 (count (:section/body-blocks (second sections))))))))

(deftest nested-headings-test
  (testing "hierarchical heading structure"
    (let [doc (parse "# Top\n\nText.\n\n## Sub A\n\nSub text.\n\n### Sub Sub\n\nDeep.\n\n## Sub B\n\nMore.")
          sections (se/extract-sections doc)]
      (is (= 4 (count sections)))
      (is (= ["Top"] (:section/heading-path (first sections))))
      (is (= ["Top" "Sub A"] (:section/heading-path (second sections))))
      (is (= ["Top" "Sub A" "Sub Sub"] (:section/heading-path (nth sections 2))))
      (is (= ["Top" "Sub B"] (:section/heading-path (nth sections 3))))
      (is (= 1 (:section/level (first sections))))
      (is (= 2 (:section/level (second sections))))
      (is (= 3 (:section/level (nth sections 2))))
      (is (= 2 (:section/level (nth sections 3)))))))

(deftest ordinal-among-siblings-test
  (testing "ordinal is 0-indexed among same-level siblings"
    (let [doc (parse "## A\n\n1\n\n## B\n\n2\n\n## C\n\n3")
          sections (se/extract-sections doc)]
      (is (= 3 (count sections)))
      (is (= 0 (:section/ordinal (first sections))))
      (is (= 0 (:section/ordinal (second sections))))
      (is (= 0 (:section/ordinal (nth sections 2)))))))

(deftest body-blocks-grouping-test
  (testing "body blocks belong to the preceding heading"
    (let [doc (parse "# Title\n\nPara 1.\n\nPara 2.\n\n## Sub\n\nSub para.")
          sections (se/extract-sections doc)]
      (is (= 2 (count sections)))
      ;; First section has 2 paragraphs
      (is (= 2 (count (:section/body-blocks (first sections)))))
      ;; Second section has 1 paragraph
      (is (= 1 (count (:section/body-blocks (second sections))))))))

(deftest code-block-in-section-test
  (testing "fenced code blocks are captured in sections"
    (let [doc (parse "## Code\n\n```clojure\n(+ 1 2)\n```\n\nAfter.")
          sections (se/extract-sections doc)]
      (is (= 1 (count sections)))
      (let [code-section (first sections)]
        (is (= ["Code"] (:section/heading-path code-section)))
        (is (>= (count (:section/body-blocks code-section)) 1))
        (is (= :fenced-code (:block/type (first (:section/body-blocks code-section)))))))))

(deftest preamble-before-first-heading-test
  (testing "content before first heading is a preamble section"
    (let [doc (parse "Preamble text.\n\n# First\n\nBody.")
          sections (se/extract-sections doc)]
      (is (= 2 (count sections)))
      (is (= [] (:section/heading-path (first sections))))
      (is (= ["First"] (:section/heading-path (second sections)))))))

(deftest empty-body-section-test
  (testing "heading with no body gets empty body-blocks"
    (let [doc (parse "# Alone\n\n# Also Alone")
          sections (se/extract-sections doc)]
      (is (= 2 (count sections)))
      (is (= [] (:section/body-blocks (first sections))))
      (is (= [] (:section/body-blocks (second sections)))))))

(deftest span-accuracy-test
  (testing "heading and body spans are correct"
    (let [source "# Hello\n\nBody text.\n\n# World\n\nDone."
          doc (parse source)
          sections (se/extract-sections doc)
          first-s (first sections)]
      ;; heading span should cover "# Hello"
      (is (= 0 (get-in first-s [:section/heading-span :span/start-byte])))
      (is (= 7 (get-in first-s [:section/heading-span :span/end-byte])))
      ;; body span should cover "Body text."
      (is (pos? (get-in first-s [:section/body-span :span/start-byte])))
      (is (> (get-in first-s [:section/body-span :span/end-byte])
             (get-in first-s [:section/body-span :span/start-byte]))))))

(deftest unicode-heading-test
  (testing "unicode in headings is preserved"
    (let [doc (parse "# .ημ\n\nContent.\n\n## Ελληνικά\n\nMore.")
          sections (se/extract-sections doc)]
      (is (= 2 (count sections)))
      (is (= [".ημ"] (:section/heading-path (first sections))))
      (is (= [".ημ" "Ελληνικά"] (:section/heading-path (second sections)))))))

;; ---------------------------------------------------------------------------
;; make-extraction-record

(deftest make-extraction-record-test
  (testing "constructs a valid extraction record"
    (let [doc (parse "# A\n\nText.\n\n## B\n\nMore.")
          sections (se/extract-sections doc)
          blob "# A\n\nText.\n\n## B\n\nMore."
          record (se/make-extraction-record sections
                                            #uuid "00000000-0000-0000-0000-000000000001"
                                            "abc123"
                                            "docs/test.md"
                                            "def456"
                                            blob
                                            "md-section-v1")]
      (is (= #uuid "00000000-0000-0000-0000-000000000001"
             (:extraction/revision-at-path-id record)))
      (is (= "abc123" (:extraction/commit-oid record)))
      (is (= "docs/test.md" (:extraction/path-raw record)))
      (is (= "def456" (:extraction/blob-oid record)))
      (is (= "md-section-v1" (:extraction/extractor-version record)))
      (is (= 2 (:extraction/section-count record)))
      (is (string? (:extraction/content-sha256 record)))
      (is (= 2 (count (:extraction/sections record))))
      ;; Section maps have flat spans
      (is (= 0 (:section/ordinal (first (:extraction/sections record)))))
      (is (= 1 (:section/level (first (:extraction/sections record)))))
      (is (= ["A"] (:section/heading-path (first (:extraction/sections record))))))))

(deftest idempotent-content-hash-test
  (testing "same blob produces same content hash"
    (let [doc1 (parse "# X\n\nContent.")
          doc2 (parse "# X\n\nContent.")
          s1 (se/extract-sections doc1)
          s2 (se/extract-sections doc2)
          blob "# X\n\nContent."
          r1 (se/make-extraction-record s1 #uuid "00000000-0000-0000-0000-000000000001" "a" "f.md" "b" blob "v1")
          r2 (se/make-extraction-record s2 #uuid "00000000-0000-0000-0000-000000000002" "a" "f.md" "b" blob "v1")]
      (is (= (:extraction/content-sha256 r1)
             (:extraction/content-sha256 r2))))))

(deftest different-blobs-different-hash-test
  (testing "different blob content produces different hash"
    (let [doc1 (parse "# X\n\nAlpha.")
          doc2 (parse "# X\n\nBeta.")
          s1 (se/extract-sections doc1)
          s2 (se/extract-sections doc2)]
      (is (not= (:extraction/content-sha256
                  (se/make-extraction-record s1 #uuid "00000000-0000-0000-0000-000000000001" "a" "f.md" "b" "# X\n\nAlpha." "v1"))
                (:extraction/content-sha256
                  (se/make-extraction-record s2 #uuid "00000000-0000-0000-0000-000000000002" "a" "f.md" "b" "# X\n\nBeta." "v1")))))))
