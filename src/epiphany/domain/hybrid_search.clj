(ns epiphany.domain.hybrid-search
  "Pure application service for hybrid search over sections.

  Combines lexical (full-text) and semantic (KNN vector) retrieval into
  a single query boundary with explicit mode selection. Structural
  filters restrict results without affecting scoring.

  No I/O. No Git. No storage. Pure data transformation over port functions."
  (:require [clojure.string :as str]))

;; ---------------------------------------------------------------------------
;; Search modes

(def valid-modes
  "Set of recognized search modes."
  #{:lexical :semantic :hybrid})

(defn valid-mode? [mode]
  (contains? valid-modes mode))

;; ---------------------------------------------------------------------------
;; Ranking configuration

(def ^:private default-ranking
  "Default ranking weights for hybrid mode. Versioned so changes are
   detectable and reproducible."
  {:ranking/version 1
   :ranking/lexical-weight 0.4
   :ranking/semantic-weight 0.6})

(defn- normalize-scores
  "Normalize a sequence of scores to [0, 1] range relative to the
   max score in the set. Returns empty vector for empty input."
  [scores]
  (if (empty? scores)
    []
    (let [max-score (apply max scores)]
      (if (zero? max-score)
        (vec (repeat (count scores) 0.0))
        (mapv #(/ (double %) (double max-score)) scores)))))

(defn- combine-hybrid-scores
  "Combine normalized lexical and semantic scores using weighted sum.
   Missing scores (nil) are treated as 0."
  [lexical-weight semantic-weight lexical-score semantic-score]
  (let [ls (or lexical-score 0.0)
        ss (or semantic-score 0.0)]
    (+ (* lexical-weight ls) (* semantic-weight ss))))

;; ---------------------------------------------------------------------------
;; Filter application

(defn- matches-path-prefix?
  "Does the path start with the given prefix?"
  [path prefix]
  (or (nil? prefix)
      (str/starts-with? path prefix)))

(defn- matches-repository?
  "Does the result match the repository filter?
   Repository filter is a map with optional :family and :instance keys."
  [result repo-filter]
  (or (nil? repo-filter)
      (and (or (nil? (:family repo-filter))
               (= (:family repo-filter) (:result/repo-family result)))
           (or (nil? (:instance repo-filter))
               (= (:instance repo-filter) (:result/repo-instance result))))))

(defn- matches-date-range?
  "Does the result's commit date fall within the date range?
   Date range is a map with optional :after and :before keys (ISO-8601 strings)."
  [result date-range]
  (or (nil? date-range)
      (let [commit-date (:result/commit-date result)]
        (when commit-date
          (and (or (nil? (:after date-range))
                   (>= (compare commit-date (:after date-range)) 0))
               (or (nil? (:before date-range))
                   (<= (compare commit-date (:before date-range)) 0)))))))

(defn- matches-ref?
  "Does the result match the ref filter?"
  [result ref-filter]
  (or (nil? ref-filter)
      (= ref-filter (:result/ref result))))

(defn- apply-filters
  "Apply all structural filters to a sequence of results."
  [results filters]
  (filter (fn [r]
            (and (matches-path-prefix? (:result/path-raw r) (:path-prefix filters))
                 (matches-repository? r (:repository filters))
                 (matches-date-range? r (:date-range filters))
                 (matches-ref? r (:ref filters))))
          results))

;; ---------------------------------------------------------------------------
;; Result enrichment

(defn- enrich-result
  "Add score breakdown and retrieval mode to a result."
  [result mode lexical-score semantic-score combined-score]
  (cond-> result
    true (assoc :result/mode mode
                :result/score combined-score
                :result/scores {:lexical lexical-score
                                :semantic semantic-score})
    (nil? lexical-score) (update :result/scores dissoc :lexical)
    (nil? semantic-score) (update :result/scores dissoc :semantic)))

;; ---------------------------------------------------------------------------
;; Query dispatch

(defn- dedupe-results
  "Deduplicate results by path+heading-path, keeping the highest-scored version."
  [results]
  (let [grouped (group-by (juxt :result/path-raw :result/heading-path) results)]
    (mapv (fn [results-for-key]
            (first (sort-by :result/score > results-for-key)))
          (vals grouped))))

(defn- fetch-lexical
  "Fetch lexical search results from the index port."
  [ports query-string limit]
  (let [search-fn (:search (:index ports))]
    (search-fn query-string)))

(defn- fetch-semantic
  "Fetch semantic search results from the index port.
   Requires an embed-query function from the embeddings port to convert
   text to a vector, then calls knn-search."
  [ports query-string k embedding-version]
  (let [embed-fn (:embed-query (:embeddings ports))
        knn-fn (:knn-search (:index ports))
        vector (embed-fn query-string)]
    (knn-fn {:vector vector
             :k k
             :embedding-version embedding-version})))

;; ---------------------------------------------------------------------------
;; Public API

(defn search
  "Execute a hybrid search query.

   Parameters:
     ports   — application port map (must include :index and :embeddings)
     request — map with keys:
       :query              string — search text
       :mode               keyword — :lexical, :semantic, or :hybrid (default :hybrid)
       :limit              int — max results (default 20)
       :filters            map — structural filters (optional):
         :path-prefix      string — match paths starting with this prefix
         :repository       map — {:family string :instance string}
         :date-range        map — {:after string :before string} (ISO-8601)
         :ref              string — Git ref name
       :embedding-version  int — filter semantic results by model version (optional)
       :ranking            map — override ranking weights (optional)

   Returns a vector of result maps, each containing:
     :result/path-raw      — file path
     :result/commit-oid    — commit hash
     :result/heading-path  — heading hierarchy
     :result/score         — final combined score
     :result/mode          — :lexical, :semantic, or :hybrid
     :result/scores        — {:lexical double :semantic double} score breakdown"
  [ports {:keys [query mode limit filters embedding-version ranking]
          :or {mode :hybrid limit 20}
          :as request}]
  (when-not (valid-mode? mode)
    (throw (ex-info (str "Invalid search mode: " (pr-str mode)
                         ". Valid modes: " (pr-str valid-modes))
                    {:mode mode :valid-modes valid-modes})))
  (when-not (and query (not (str/blank? query)))
    (throw (ex-info "Search query cannot be blank" {:query query})))
  (let [ranking-cfg (merge default-ranking ranking)
        lexical-w (:ranking/lexical-weight ranking-cfg)
        semantic-w (:ranking/semantic-weight ranking-cfg)]
    (case mode
      :lexical
      (let [raw (fetch-lexical ports query limit)
            filtered (take limit (vec (apply-filters raw filters)))
            normalized (normalize-scores (map :result/score filtered))]
        (mapv (fn [result norm-score]
                (enrich-result result :lexical norm-score nil norm-score))
              filtered normalized))

      :semantic
      (let [raw (fetch-semantic ports query limit embedding-version)
            filtered (take limit (vec (apply-filters raw filters)))
            normalized (normalize-scores (map :result/score filtered))]
        (mapv (fn [result norm-score]
                (enrich-result result :semantic nil norm-score norm-score))
              filtered normalized))

      :hybrid
      (let [lexical-raw (fetch-lexical ports query limit)
            semantic-raw (fetch-semantic ports query limit embedding-version)
            lex-normalized (normalize-scores (map :result/score lexical-raw))
            sem-normalized (normalize-scores (map :result/score semantic-raw))
            ;; Index lexical results by key
            lex-index (into {} (map (fn [r s] [((juxt :result/path-raw :result/heading-path) r)
                                               {:result r :lexical-score s :semantic-score nil}])
                                    lexical-raw lex-normalized))
            ;; Merge semantic results into index (prefer semantic as base)
            merged (reduce-kv
                     (fn [acc k {:keys [result semantic-score]}]
                       (let [existing (get acc k)]
                         (if existing
                           ;; Merge: add semantic score, prefer semantic result as base
                           (assoc acc k {:result (:result existing)
                                         :lexical-score (:lexical-score existing)
                                         :semantic-score semantic-score})
                           ;; New entry
                           (assoc acc k {:result result
                                         :lexical-score nil
                                         :semantic-score semantic-score}))))
                     lex-index
                     (into {} (map (fn [r s] [((juxt :result/path-raw :result/heading-path) r)
                                              {:result r :semantic-score s}])
                                   semantic-raw sem-normalized)))
            ;; Build combined results
            combined (mapv (fn [[k {:keys [result lexical-score semantic-score]}]]
                             (let [combined-score (combine-hybrid-scores
                                                    lexical-w semantic-w
                                                    lexical-score semantic-score)]
                               (enrich-result result :hybrid
                                               lexical-score semantic-score
                                               combined-score)))
                           merged)
            filtered (apply-filters combined filters)
            sorted (sort-by :result/score > filtered)]
        (take limit sorted)))))
