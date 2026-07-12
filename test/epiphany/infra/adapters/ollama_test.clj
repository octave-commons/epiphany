(ns epiphany.infra.adapters.ollama-test
  (:require [clojure.test :refer [deftest is testing]]
            [epiphany.infra.adapters.ollama :as ollama]
            [epiphany.domain.section-extraction :as se]
            [epiphany.shape.markdown :as md]))

(defn- make-test-record [source path commit-oid blob-oid]
  (let [doc (md/parse source)
        sections (se/extract-sections doc)
        blob source]
    (se/make-extraction-record sections
                               #uuid "00000000-0000-0000-0000-000000000001"
                               commit-oid path blob-oid blob "test-v1")))

;; ---------------------------------------------------------------------------
;; Live Ollama integration tests (require running Ollama with nomic-embed-text)

(deftest ^:integration embed-query-test
  (testing "single text embedding returns correct dimensions"
    (let [adapter (ollama/make-embeddings-adapter {:base-url "http://localhost:11434"
                                                    :model "nomic-embed-text"})
          vector ((:embed-query adapter) "Hello world")]
      (is (vector? vector))
      (is (= 768 (count vector)))
      (is (every? float? vector)))))

(deftest ^:integration embed-sections-test
  (testing "batch embedding of extraction records"
    (let [adapter (ollama/make-embeddings-adapter {:base-url "http://localhost:11434"
                                                    :model "nomic-embed-text"
                                                    :batch-size 2})
          record (make-test-record "# First\n\nAlpha.\n\n# Second\n\nBeta."
                                   "doc.md" "c1" "b1")
          results ((:embed-sections! adapter) [record])]
      (is (= 2 (count results)))
      (is (= "doc.md" (:embedding/path-raw (first results))))
      (is (= "c1" (:embedding/commit-oid (first results))))
      (is (= ["First"] (:embedding/heading-path (first results))))
      (is (= 768 (:embedding/dimensions (first results))))
      (is (= "nomic-embed-text" (:embedding/model (first results))))
      (is (vector? (:embedding/vector (first results))))
      (is (= 768 (count (:embedding/vector (first results))))))))

(deftest ^:integration embed-version-test
  (testing "version is deterministic for same config"
    (let [a1 (ollama/make-embeddings-adapter {:model "nomic-embed-text"})
          a2 (ollama/make-embeddings-adapter {:model "nomic-embed-text"})
          a3 (ollama/make-embeddings-adapter {:model "other-model"})]
      (is (= ((:embedding-version a1)) ((:embedding-version a2))))
      (is (not= ((:embedding-version a1)) ((:embedding-version a3)))))))

(deftest ^:integration embed-batch-size-test
  (testing "batching works with small batch size"
    (let [adapter (ollama/make-embeddings-adapter {:base-url "http://localhost:11434"
                                                    :model "nomic-embed-text"
                                                    :batch-size 1})
          r1 (make-test-record "# A\n\nText." "a.md" "c1" "b1")
          r2 (make-test-record "# B\n\nText." "b.md" "c2" "b2")
          results ((:embed-sections! adapter) [r1 r2])]
      (is (= 2 (count results)))
      (is (every? #(= 768 (:embedding/dimensions %)) results)))))
