(ns epiphany.infra.adapters.lucene-knn-test
  (:require [clojure.test :refer [deftest is testing use-fixtures]]
            [epiphany.infra.adapters.lucene :as lucene]
            [epiphany.domain.section-extraction :as se]
            [epiphany.shape.markdown :as md])
  (:import [java.nio.file Files]
           [java.nio.file.attribute FileAttribute]))

(defn- temp-index-dir []
  (Files/createTempDirectory "lucene-knn-test" (into-array FileAttribute [])))

(defn- make-test-record [source path commit-oid blob-oid]
  (let [doc (md/parse source)
        sections (se/extract-sections doc)]
    (se/make-extraction-record sections
                               #uuid "00000000-0000-0000-0000-000000000001"
                               commit-oid path blob-oid source "test-v1")))

(defn- make-embedding [path heading-path vector model version]
  {:embedding/path-raw path
   :embedding/commit-oid "commit1"
   :embedding/heading-path heading-path
   :embedding/level (count heading-path)
   :embedding/ordinal 0
   :embedding/vector vector
   :embedding/model model
   :embedding/dimensions (count vector)
   :embedding-version version})

;; ---------------------------------------------------------------------------
;; index-embeddings! + knn-search

(deftest knn-basic-search-test
  (testing "KNN search returns nearest neighbors"
    (let [dir (temp-index-dir)
          adapter (lucene/make-index-adapter {:index-dir dir})
          ;; Create embeddings with known vectors
          e1 (make-embedding "a.md" ["Alpha"] [1.0 0.0 0.0] "test-model" 1)
          e2 (make-embedding "b.md" ["Beta"] [0.0 1.0 0.0] "test-model" 1)
          e3 (make-embedding "c.md" ["Gamma"] [0.0 0.0 1.0] "test-model" 1)]
      ((:index-embeddings! adapter) [e1 e2 e3])
      ;; Query vector close to Alpha
      (let [results ((:knn-search adapter) {:vector [0.9 0.1 0.0] :k 3})]
        (is (seq results))
        (is (= "a.md" (:result/path-raw (first results))))
        (is (pos? (:result/score (first results))))))))

(deftest knn-version-filter-test
  (testing "KNN search filters by embedding version"
    (let [dir (temp-index-dir)
          adapter (lucene/make-index-adapter {:index-dir dir})
          e1 (make-embedding "a.md" ["A"] [1.0 0.0] "model" 1)
          e2 (make-embedding "b.md" ["B"] [1.0 0.0] "model" 2)]
      ((:index-embeddings! adapter) [e1 e2])
      ;; Search version 1 only
      (let [results ((:knn-search adapter) {:vector [1.0 0.0] :k 10 :embedding-version 1})]
        (is (= 1 (count results)))
        (is (= "a.md" (:result/path-raw (first results)))))
      ;; Search version 2 only
      (let [results ((:knn-search adapter) {:vector [1.0 0.0] :k 10 :embedding-version 2})]
        (is (= 1 (count results)))
        (is (= "b.md" (:result/path-raw (first results)))))
      ;; Search all versions
      (let [results ((:knn-search adapter) {:vector [1.0 0.0] :k 10})]
        (is (= 2 (count results)))))))

(deftest knn-model-reported-test
  (testing "KNN results include model name and version"
    (let [dir (temp-index-dir)
          adapter (lucene/make-index-adapter {:index-dir dir})
          e1 (make-embedding "a.md" ["A"] [1.0 0.0] "nomic-embed-text" 42)]
      ((:index-embeddings! adapter) [e1])
      (let [results ((:knn-search adapter) {:vector [1.0 0.0] :k 1})]
        (is (= "nomic-embed-text" (:result/model (first results))))
        (is (= "42" (:result/embedding-version (first results))))))))

(deftest knn-empty-index-test
  (testing "KNN search on empty index returns empty"
    (let [dir (temp-index-dir)
          adapter (lucene/make-index-adapter {:index-dir dir})]
      (is (= [] ((:knn-search adapter) {:vector [1.0 0.0] :k 5}))))))

(deftest knn-mixed-doc-types-test
  (testing "KNN search does not return section documents"
    (let [dir (temp-index-dir)
          adapter (lucene/make-index-adapter {:index-dir dir})
          record (make-test-record "# Hello\n\nWorld." "doc.md" "c1" "b1")
          e1 (make-embedding "a.md" ["Alpha"] [1.0 0.0] "model" 1)]
      ;; Index both sections and embeddings
      ((:index-sections! adapter) record)
      ((:index-embeddings! adapter) [e1])
      ;; KNN search should only return embedding docs
      (let [results ((:knn-search adapter) {:vector [1.0 0.0] :k 10})]
        (is (= 1 (count results)))
        (is (= "a.md" (:result/path-raw (first results))))))))

(deftest knn-similarity-ranking-test
  (testing "results are ranked by cosine similarity"
    (let [dir (temp-index-dir)
          adapter (lucene/make-index-adapter {:index-dir dir})
          ;; Three vectors — far is closest to query by cosine
          e1 (make-embedding "far.md" ["Far"] [1.0 0.0] "model" 1)
          e2 (make-embedding "mid.md" ["Mid"] [0.0 1.0] "model" 1)
          e3 (make-embedding "close.md" ["Close"] [0.7 0.7] "model" 1)]
      ((:index-embeddings! adapter) [e1 e2 e3])
      ;; Query close to far.md (cosine similarity: far=0.95, close=0.85, mid=0.05)
      (let [results ((:knn-search adapter) {:vector [0.95 0.05] :k 3})]
        (is (= 3 (count results)))
        (is (= "far.md" (:result/path-raw (first results))))
        (is (>= (:result/score (first results))
                (:result/score (second results))))))))

(deftest lexical-search-still-works-test
  (testing "lexical search is unaffected by embedding documents"
    (let [dir (temp-index-dir)
          adapter (lucene/make-index-adapter {:index-dir dir})
          record (make-test-record "# Hello World\n\nContent." "doc.md" "c1" "b1")
          e1 (make-embedding "other.md" ["Other"] [1.0 0.0] "model" 1)]
      ((:index-sections! adapter) record)
      ((:index-embeddings! adapter) [e1])
      ;; Lexical search finds section doc
      (let [results ((:search adapter) "Hello")]
        (is (= 1 (count results)))
        (is (= "doc.md" (:result/path-raw (first results))))))))
