(ns epiphany.domain.benchmark
  "Pure benchmark harness for retrieval quality evaluation.

  Loads a query set, runs searches against injected ports, scores
  results using Recall@k and nDCG, measures latency, and produces
  a versioned report. No I/O — all side effects are injected.

  Scoring:
    Recall@k = |relevant ∩ top-k| / |relevant|
    nDCG     = DCG / ideal-DCG (binary relevance)

  Latency is measured per-query and reported as median/mean/p95."
  (:require [clojure.string :as str]))

;; ---------------------------------------------------------------------------
;; Scoring functions

(defn recall-at-k
  "Compute Recall@k: fraction of expected items found in top-k results.

   expected — set of {:path string :heading vector} maps
   results  — vector of result maps (each has :result/path-raw, :result/heading-path)
   k        — number of top results to consider

   Returns double in [0.0, 1.0]."
  [expected results k]
  (if (empty? expected)
    1.0
    (let [top-k (take k results)
          found (reduce (fn [acc result]
                          (let [path (:result/path-raw result)
                                heading (:result/heading-path result)]
                            (if (some (fn [e]
                                        (and (= path (:path e))
                                             (= heading (:heading e))))
                                      expected)
                              (inc acc)
                              acc)))
                        0
                        top-k)]
      (/ (double found) (double (count expected))))))

(defn dcg-at-k
  "Compute DCG@k with binary relevance.

   expected — set of relevant items
   results  — ranked result list
   k        — cutoff

   Relevance is 1 if the result is in expected, 0 otherwise.
   DCG = sum(rel_i / log2(i+1)) for i in 1..k."
  [expected results k]
  (let [top-k (take k results)]
    (reduce-kv
     (fn [acc i result]
       (let [rel (if (some (fn [e]
                             (and (= (:result/path-raw result) (:path e))
                                  (= (:result/heading-path result) (:heading e))))
                           expected)
                   1.0
                   0.0)]
         (+ acc (/ rel (Math/log (+ 2.0 (double i)))))))
     0.0
     (vec top-k))))

(defn ndcg-at-k
  "Compute nDCG@k: normalized discounted cumulative gain.

   Returns double in [0.0, 1.0]. 1.0 = perfect ranking."
  [expected results k]
  (let [actual-dcg (dcg-at-k expected results k)
        ;; Ideal: all relevant items ranked first
        ideal-results (concat (filter (fn [r]
                                        (some (fn [e]
                                                (and (= (:result/path-raw r) (:path e))
                                                     (= (:result/heading-path r) (:heading e))))
                                              expected))
                                      results)
                              (remove (fn [r]
                                        (some (fn [e]
                                                (and (= (:result/path-raw r) (:path e))
                                                     (= (:result/heading-path r) (:heading e))))
                                              expected))
                                      results))
        ideal-dcg (dcg-at-k expected ideal-results k)]
    (if (zero? ideal-dcg)
      0.0
      (/ actual-dcg ideal-dcg))))

;; ---------------------------------------------------------------------------
;; Latency measurement

(defn measure-latency
  "Execute fn and return {:result <fn-result> :latency-ms long}."
  [f]
  (let [start (System/nanoTime)
        result (f)
        elapsed-ms (long (/ (- (System/nanoTime) start) 1000000))]
    {:result result
     :latency-ms elapsed-ms}))

(defn percentile
  "Compute the p-th percentile of a sorted numeric collection."
  [sorted-coll p]
  (if (empty? sorted-coll)
    0
    (let [idx (min (dec (count sorted-coll))
                   (long (Math/round (* (/ p 100.0) (dec (count sorted-coll))))))]
      (nth sorted-coll idx))))

;; ---------------------------------------------------------------------------
;; Query matching

(defn result-matches-expected?
  "Check if a search result matches an expected item."
  [result expected-item]
  (and (= (:result/path-raw result) (:path expected-item))
       (= (:result/heading-path result) (:heading expected-item))))

;; ---------------------------------------------------------------------------
;; Benchmark runner

(defn run-query-benchmark
  "Run a single query benchmark.

   search-fn — (fn [query opts]) -> vector of results
   query-map — {:id :query :mode :expected :filters ...}

   Returns map with scores and latency."
  [search-fn {:keys [query mode expected filters id]}]
  (let [search-opts (cond-> {:query query
                              :mode mode
                              :limit 20}
                      filters (assoc :filters filters))
        {:keys [result latency-ms]} (measure-latency #(search-fn query search-opts))
        results (vec result)]
    {:query-id id
     :query query
     :mode mode
     :latency-ms latency-ms
     :result-count (count results)
     :recall-1 (recall-at-k expected results 1)
     :recall-3 (recall-at-k expected results 3)
     :recall-5 (recall-at-k expected results 5)
     :recall-10 (recall-at-k expected results 10)
     :ndcg-5 (ndcg-at-k expected results 5)
     :ndcg-10 (ndcg-at-k expected results 10)}))

(defn run-benchmark
  "Run the full benchmark suite against provided ports.

   ports — application port map (must include :index and :embeddings)
   query-set — parsed query set from queries.edn
   opts — optional overrides:
     :search-fn  — custom search function (default: hs/search with ports)
     :query-ids  — set of :ids to run (default: all)

   Returns {:version int, :queries [result-map], :summary {}} "
  [ports query-set & [{:keys [search-fn query-ids]
                        :or {search-fn nil}}]]
  (require 'epiphany.domain.hybrid-search)
  (let [hs-search (or search-fn
                      (fn [query opts]
                        ((resolve 'epiphany.domain.hybrid-search/search) ports opts)))
        all-queries (concat (:benchmark/queries query-set)
                            (:benchmark/semantic-queries query-set)
                            (:benchmark/hybrid-queries query-set)
                            (:benchmark/filter-queries query-set))
        queries (if query-ids
                  (filter #(query-ids (:id %)) all-queries)
                  all-queries)
        results (mapv (fn [q]
                        (try
                          (run-query-benchmark hs-search q)
                          (catch Exception e
                            {:query-id (:id q)
                             :error (.getMessage e)
                             :recall-1 0.0 :recall-3 0.0 :recall-5 0.0 :recall-10 0.0
                             :ndcg-5 0.0 :ndcg-10 0.0 :latency-ms 0})))
                      queries)
        ;; Aggregate by mode
        by-mode (group-by :mode results)
        mode-summary (into {}
                           (map (fn [[mode mode-results]]
                                  (let [latencies (map :latency-ms mode-results)
                                        sorted-lat (sort latencies)]
                                    [mode {:count (count mode-results)
                                           :recall-1-mean (double (/ (reduce + (map :recall-1 mode-results))
                                                                      (count mode-results)))
                                           :recall-3-mean (double (/ (reduce + (map :recall-3 mode-results))
                                                                      (count mode-results)))
                                           :recall-5-mean (double (/ (reduce + (map :recall-5 mode-results))
                                                                      (count mode-results)))
                                           :recall-10-mean (double (/ (reduce + (map :recall-10 mode-results))
                                                                       (count mode-results)))
                                           :ndcg-5-mean (double (/ (reduce + (map :ndcg-5 mode-results))
                                                                    (count mode-results)))
                                           :ndcg-10-mean (double (/ (reduce + (map :ndcg-10 mode-results))
                                                                      (count mode-results)))
                                           :latency-median (nth sorted-lat (/ (count sorted-lat) 2) 0)
                                           :latency-mean (double (/ (reduce + latencies) (count latencies)))
                                           :latency-p95 (percentile sorted-lat 95)}]))
                                by-mode))
        ;; Overall summary
        all-latencies (map :latency-ms results)
        sorted-all (sort all-latencies)]
    {:benchmark/version (:benchmark/version query-set)
     :benchmark/description (:benchmark/description query-set)
     :query-count (count results)
     :results results
     :summary {:by-mode mode-summary
               :overall {:recall-1-mean (double (/ (reduce + (map :recall-1 results))
                                                    (count results)))
                         :recall-5-mean (double (/ (reduce + (map :recall-5 results))
                                                    (count results)))
                         :ndcg-5-mean (double (/ (reduce + (map :ndcg-5 results))
                                                  (count results)))
                         :latency-median (nth sorted-all (/ (count sorted-all) 2) 0)
                         :latency-mean (double (/ (reduce + all-latencies) (count all-latencies)))
                         :latency-p95 (percentile sorted-all 95)}}}))

;; ---------------------------------------------------------------------------
;; Report formatting

(defn format-report-text
  "Format a benchmark report as human-readable text."
  [report]
  (let [{:keys [benchmark/version query-count summary]} report
        overall (:overall summary)]
    (str "=== Benchmark Report v" version " ===\n"
         "Queries: " query-count "\n\n"
         "--- Overall ---\n"
         "  Recall@1:  " (format "%.3f" (:recall-1-mean overall)) "\n"
         "  Recall@5:  " (format "%.3f" (:recall-5-mean overall)) "\n"
         "  nDCG@5:    " (format "%.3f" (:ndcg-5-mean overall)) "\n"
         "  Latency:   " (:latency-median overall) "ms (median), "
         (:latency-mean overall) "ms (mean), "
         (:latency-p95 overall) "ms (p95)\n\n"
         "--- By Mode ---\n"
         (clojure.string/join
          "\n"
          (map (fn [[mode stats]]
                 (str "  " (name mode) ":\n"
                      "    Queries:     " (:count stats) "\n"
                      "    Recall@1:    " (format "%.3f" (:recall-1-mean stats)) "\n"
                      "    Recall@5:    " (format "%.3f" (:recall-5-mean stats)) "\n"
                      "    nDCG@5:      " (format "%.3f" (:ndcg-5-mean stats)) "\n"
                      "    Latency:     " (:latency-median stats) "ms (median)"))
               (:by-mode summary))))))
