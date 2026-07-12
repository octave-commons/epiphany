(ns epiphany.domain.benchmark-test
  (:require [clojure.string :as string]
            [clojure.test :refer [deftest is testing]]
            [clojure.edn :as edn]
            [epiphany.domain.benchmark :as bench]))

;; ---------------------------------------------------------------------------
;; Test doubles

(defn- make-mock-search-fn
  "Create a mock search function that returns results based on query content."
  []
  (fn [query opts]
    (let [mode (:mode opts :hybrid)]
      (case mode
        :lexical
        [{:result/path-raw "docs/notes/design/phase-1-corpus-archaeology.md"
          :result/heading-path ["Data Authority"]
          :result/commit-oid "abc123"
          :result/score 0.9
          :result/mode :lexical
          :result/scores {:lexical 0.9}}
         {:result/path-raw "AGENTS.md"
          :result/heading-path ["Namespace law" "four quadrants" "no junk drawers"]
          :result/commit-oid "def456"
          :result/score 0.7
          :result/mode :lexical
          :result/scores {:lexical 0.7}}]

        :semantic
        [{:result/path-raw "docs/kanban/epics/epic-03-retrieval-substrate.md"
          :result/heading-path []
          :result/commit-oid "ghi789"
          :result/score 0.85
          :result/mode :semantic
          :result/scores {:semantic 0.85}}
         {:result/path-raw "src/epiphany/domain/hybrid_search.clj"
          :result/heading-path []
          :result/commit-oid "jkl012"
          :result/score 0.75
          :result/mode :semantic
          :result/scores {:semantic 0.75}}]

        :hybrid
        [{:result/path-raw "docs/notes/design/phase-1-corpus-archaeology.md"
          :result/heading-path ["Data Authority"]
          :result/commit-oid "abc123"
          :result/score 0.88
          :result/mode :hybrid
          :result/scores {:lexical 0.9 :semantic 0.85}}
         {:result/path-raw "AGENTS.md"
          :result/heading-path ["Namespace law" "four quadrants" "no junk drawers"]
          :result/commit-oid "def456"
          :result/score 0.72
          :result/mode :hybrid
          :result/scores {:lexical 0.7 :semantic 0.6}}
         {:result/path-raw "docs/kanban/epics/epic-03-retrieval-substrate.md"
          :result/heading-path []
          :result/commit-oid "ghi789"
          :result/score 0.65
          :result/mode :hybrid
          :result/scores {:lexical 0.5 :semantic 0.75}}]))))

;; ---------------------------------------------------------------------------
;; Scoring unit tests

(deftest recall-at-k-test
  (testing "perfect recall"
    (let [expected #{{:path "a.md" :heading ["A"]}}
          results [{:result/path-raw "a.md" :result/heading-path ["A"]}]]
      (is (== 1.0 (bench/recall-at-k expected results 1)))))

  (testing "partial recall"
    (let [expected #{{:path "a.md" :heading ["A"]}
                     {:path "b.md" :heading ["B"]}}
          results [{:result/path-raw "a.md" :result/heading-path ["A"]}
                   {:result/path-raw "c.md" :result/heading-path ["C"]}]]
      (is (== 0.5 (bench/recall-at-k expected results 2)))))

  (testing "empty expected returns 1.0"
    (is (== 1.0 (bench/recall-at-k #{} [{:result/path-raw "x"}] 5))))

  (testing "empty results returns 0.0"
    (is (== 0.0 (bench/recall-at-k #{{:path "a" :heading []}} [] 5)))))

(deftest ndcg-at-k-test
  (testing "perfect ranking"
    (let [expected #{{:path "a.md" :heading []}}
          results [{:result/path-raw "a.md" :result/heading-path []}]]
      (is (== 1.0 (bench/ndcg-at-k expected results 1)))))

  (testing "imperfect ranking still > 0"
    (let [expected #{{:path "a.md" :heading []}}
          results [{:result/path-raw "b.md" :result/heading-path []}
                   {:result/path-raw "a.md" :result/heading-path []}]]
      (is (< 0.0 (bench/ndcg-at-k expected results 2) 1.0)))))

(deftest percentile-test
  (testing "median of odd count"
    (is (== 3 (bench/percentile [1 2 3 4 5] 50))))
  (testing "p95 of small collection"
    (is (== 5 (bench/percentile [1 2 3 4 5] 95)))))

(deftest measure-latency-test
  (testing "measures execution time"
    (let [{:keys [result latency-ms]} (bench/measure-latency (fn [] 42))]
      (is (== 42 result))
      (is (>= latency-ms 0)))))

;; ---------------------------------------------------------------------------
;; Query set loading

(deftest query-set-loading-test
  (testing "can load the query set fixture"
    (let [query-set (edn/read-string (slurp "docs/benchmarks/queries.edn"))]
      (is (= 1 (:benchmark/version query-set)))
      (is (seq (:benchmark/queries query-set)))
      (is (seq (:benchmark/semantic-queries query-set)))
      (is (seq (:benchmark/hybrid-queries query-set)))
      (is (seq (:benchmark/filter-queries query-set)))
      ;; Total should be 30+
      (is (>= (+ (count (:benchmark/queries query-set))
                  (count (:benchmark/semantic-queries query-set))
                  (count (:benchmark/hybrid-queries query-set))
                  (count (:benchmark/filter-queries query-set)))
              30)))))

;; ---------------------------------------------------------------------------
;; Benchmark runner integration

(deftest run-query-benchmark-test
  (testing "runs a single query benchmark"
    (let [search-fn (make-mock-search-fn)
          query {:id :test-001
                 :query "event log"
                 :mode :hybrid
                 :expected [{:path "docs/notes/design/phase-1-corpus-archaeology.md"
                             :heading ["Data Authority"]}]}
          result (bench/run-query-benchmark search-fn query)]
      (is (= :test-001 (:query-id result)))
      (is (= :hybrid (:mode result)))
      (is (>= (:latency-ms result) 0))
      (is (>= (:recall-1 result) 0.0))
      (is (>= (:recall-5 result) 0.0)))))

(deftest run-benchmark-test
  (testing "runs full benchmark with mock ports"
    (let [search-fn (make-mock-search-fn)
          query-set {:benchmark/version 1
                     :benchmark/queries [{:id :q1 :query "test" :mode :lexical
                                         :expected [{:path "AGENTS.md"
                                                     :heading ["Namespace law" "four quadrants" "no junk drawers"]}]}]
                     :benchmark/semantic-queries []
                     :benchmark/hybrid-queries []
                     :benchmark/filter-queries []}
          report (bench/run-benchmark {} query-set {:search-fn search-fn})]
      (is (= 1 (:benchmark/version report)))
      (is (= 1 (:query-count report)))
      (is (contains? (:summary report) :by-mode))
      (is (contains? (:summary report) :overall)))))

(deftest format-report-text-test
  (testing "formats report as readable text"
    (let [report {:benchmark/version 1
                  :query-count 5
                  :summary {:by-mode {:hybrid {:count 3 :recall-1-mean 0.8
                                               :recall-3-mean 0.9 :recall-5-mean 0.95
                                               :recall-10-mean 1.0 :ndcg-5-mean 0.85
                                               :ndcg-10-mean 0.9 :latency-median 15
                                               :latency-mean 18.5 :latency-p95 30}}
                            :overall {:recall-1-mean 0.6 :recall-5-mean 0.8
                                      :ndcg-5-mean 0.7 :latency-median 12
                                      :latency-mean 15.3 :latency-p95 25}}}
          text (bench/format-report-text report)]
      (is (string/includes? text "Benchmark Report v1"))
      (is (string/includes? text "Queries: 5"))
      (is (string/includes? text "Recall@1"))
      (is (string/includes? text "hybrid")))))
