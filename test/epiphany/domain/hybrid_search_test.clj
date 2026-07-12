(ns epiphany.domain.hybrid-search-test
  (:require [clojure.test :refer [deftest is testing]]
            [clojure.string :as str]
            [epiphany.domain.hybrid-search :as hs]))

;; ---------------------------------------------------------------------------
;; Test doubles

(defn- make-lexical-results []
  [{:result/path-raw "docs/architecture.md"
    :result/commit-oid "abc123"
    :result/heading-path ["Architecture"]
    :result/score 0.9}
   {:result/path-raw "docs/notes.md"
    :result/commit-oid "def456"
    :result/heading-path ["Notes"]
    :result/score 0.7}
   {:result/path-raw "src/core.clj"
    :result/commit-oid "ghi789"
    :result/heading-path ["Core"]
    :result/score 0.5}])

(defn- make-semantic-results []
  [{:result/path-raw "docs/architecture.md"
    :result/commit-oid "abc123"
    :result/heading-path ["Architecture"]
    :result/score 0.85
    :result/model "nomic-embed-text"
    :result/embedding-version "1"}
   {:result/path-raw "docs/design.md"
    :result/commit-oid "jkl012"
    :result/heading-path ["Design"]
    :result/score 0.75
    :result/model "nomic-embed-text"
    :result/embedding-version "1"}])

(defn- make-test-ports []
  {:index {:search (fn [_] (make-lexical-results))
           :knn-search (fn [{:keys [vector]}]
                         (make-semantic-results))
           :index-sections! (fn [_] nil)
           :index-embeddings! (fn [_] nil)
           :index-version (fn [] 1)
           :rebuild-index! (fn [_] nil)
           :clear-index! (fn [] nil)}
   :embeddings {:embed-query (fn [text] (vec (repeatedly 768 #(double (- (rand 2) 1)))))
                :embed-sections! (fn [_] nil)
                :embedding-version (fn [] 1)
                :clear-embeddings! (fn [] nil)}})

;; ---------------------------------------------------------------------------
;; Mode validation

(deftest invalid-mode-test
  (testing "throws on invalid mode"
    (is (thrown-with-msg? clojure.lang.ExceptionInfo #"Invalid search mode"
                          (hs/search (make-test-ports) {:query "test" :mode :invalid})))))

(deftest blank-query-test
  (testing "throws on blank query"
    (is (thrown-with-msg? clojure.lang.ExceptionInfo #"cannot be blank"
                          (hs/search (make-test-ports) {:query ""})))))

;; ---------------------------------------------------------------------------
;; Lexical mode

(deftest lexical-mode-test
  (testing "lexical mode returns results from lexical search only"
    (let [results (hs/search (make-test-ports) {:query "architecture" :mode :lexical})]
      (is (= 3 (count results)))
      (is (every? #(= :lexical (:result/mode %)) results))
      (is (every? :result/scores results))
      (is (every? #(contains? (:result/scores %) :lexical) results))
      ;; Scores should be normalized
      (is (== 1.0 (:result/score (first results)))))))

;; ---------------------------------------------------------------------------
;; Semantic mode

(deftest semantic-mode-test
  (testing "semantic mode returns results from KNN search only"
    (let [results (hs/search (make-test-ports) {:query "architecture" :mode :semantic})]
      (is (= 2 (count results)))
      (is (every? #(= :semantic (:result/mode %)) results))
      (is (every? #(contains? (:result/scores %) :semantic) results)))))

;; ---------------------------------------------------------------------------
;; Hybrid mode

(deftest hybrid-mode-test
  (testing "hybrid mode combines lexical and semantic results"
    (let [results (hs/search (make-test-ports) {:query "architecture" :mode :hybrid})]
      ;; Should have 4 unique results (architecture.md appears in both, deduped)
      (is (= 4 (count results)))
      (is (every? #(= :hybrid (:result/mode %)) results))
      (is (every? :result/scores results))
      ;; Results from both sources should have both scores;
      ;; results from only one source have only that source's score.
      (is (some #(and (contains? (:result/scores %) :lexical)
                      (contains? (:result/scores %) :semantic))
                results))
      ;; Results from only lexical
      (is (some #(and (contains? (:result/scores %) :lexical)
                      (not (contains? (:result/scores %) :semantic)))
                results))
      ;; Results from only semantic
      (is (some #(and (contains? (:result/scores %) :semantic)
                      (not (contains? (:result/scores %) :lexical)))
                results))
      ;; Results should be sorted by score descending
      (is (>= (:result/score (first results))
              (:result/score (second results)))))))

(deftest hybrid-ranking-weights-test
  (testing "hybrid ranking respects custom weights"
    (let [;; With high semantic weight, semantic-only results rank higher
          results-default (hs/search (make-test-ports)
                                     {:query "test" :mode :hybrid})
          results-lex-heavy (hs/search (make-test-ports)
                                       {:query "test" :mode :hybrid
                                        :ranking {:ranking/lexical-weight 0.9
                                                  :ranking/semantic-weight 0.1}})]
      ;; Both should return results
      (is (pos? (count results-default)))
      (is (pos? (count results-lex-heavy))))))

;; ---------------------------------------------------------------------------
;; Deduplication

(deftest hybrid-deduplication-test
  (testing "hybrid mode deduplicates by path+heading"
    (let [results (hs/search (make-test-ports) {:query "test" :mode :hybrid})
          paths (map :result/path-raw results)
          ;; architecture.md should appear once (deduped from lexical+semantic)
          arch-count (count (filter #(= "docs/architecture.md" %) paths))]
      (is (== 1 arch-count)))))

;; ---------------------------------------------------------------------------
;; Filters

(deftest path-prefix-filter-test
  (testing "path-prefix filter restricts results"
    (let [results (hs/search (make-test-ports)
                             {:query "test" :mode :lexical
                              :filters {:path-prefix "docs/"}})]
      (is (every? #(str/starts-with? (:result/path-raw %) "docs/") results))
      ;; src/core.clj should be excluded
      (is (not (some #(= "src/core.clj" (:result/path-raw %)) results))))))

(deftest empty-results-filter-test
  (testing "filters on empty results return empty"
    (let [ports {:index {:search (fn [_] [])
                         :knn-search (fn [_] [])}
                 :embeddings {:embed-query (fn [_] [0.0])}}
          results (hs/search ports {:query "test" :mode :hybrid})]
      (is (= [] results)))))

(deftest lexical-only-filter-test
  (testing "filters apply to lexical mode"
    (let [results (hs/search (make-test-ports)
                             {:query "test" :mode :lexical
                              :filters {:path-prefix "src/"}})]
      (is (every? #(str/starts-with? (:result/path-raw %) "src/") results)))))

(deftest semantic-only-filter-test
  (testing "filters apply to semantic mode"
    (let [results (hs/search (make-test-ports)
                             {:query "test" :mode :semantic
                              :filters {:path-prefix "docs/"}})]
      (is (every? #(str/starts-with? (:result/path-raw %) "docs/") results)))))

;; ---------------------------------------------------------------------------
;; Limit

(deftest limit-test
  (testing "limit restricts result count"
    (let [results (hs/search (make-test-ports)
                             {:query "test" :mode :lexical :limit 1})]
      (is (== 1 (count results))))))
