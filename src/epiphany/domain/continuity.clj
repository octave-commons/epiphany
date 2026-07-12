(ns epiphany.domain.continuity
  "Deterministic continuity features between adjacent revisions of the same path.

  For a pair of revisions (previous, current) of the same file path,
  compute raw signals and a scored continuity result. All features are
  deterministic — no LLM, no embeddings.

  The continuity score names the policy/configuration version that
  produced it, enabling reproducibility and version tracking."
  (:require [clojure.string :as str]
            [clojure.set :as set]))

;; ---------------------------------------------------------------------------
;; Policy version

(def policy-version
  "Version string for the continuity scoring policy.
   Bump when scoring logic changes to create new, independent results."
  "continuity-v1")

;; ---------------------------------------------------------------------------
;; Raw feature computation

(defn text-similarity
  "Compute text similarity between two blobs as a ratio [0, 1].
   Uses character-level bigram overlap (Jaccard-like)."
  [blob-a blob-b]
  (if (and (seq blob-a) (seq blob-b))
    (let [bigrams (fn [s] (set (map #(subs s % (+ % 2))
                                     (range (dec (count s))))))
          a (bigrams blob-a)
          b (bigrams blob-b)
          intersection (count (set/intersection a b))
          union (count (set/union a b))]
      (if (pos? union)
        (double (/ intersection union))
        0.0))
    0.0))

(defn- extract-frontmatter
  "Extract YAML front-matter string from a Markdown blob, or nil."
  [blob]
  (when (and blob (str/starts-with? blob "---"))
    (let [rest-blob (subs blob 3)
          end (str/index-of rest-blob "---")]
      (when end
        (subs rest-blob 0 end)))))

(defn- parse-frontmatter-keys
  "Parse front-matter into a map of keyword -> string value."
  [fm]
  (into {} (for [line (str/split-lines fm)
                 :when (and (seq line) (not (str/starts-with? line "#")))]
             (let [[k v] (str/split line #":" 2)]
               [(str/trim k) (str/trim (or v ""))]))))

(defn frontmatter-delta
  "Extract front-matter (YAML between --- markers) and compute delta.
   Returns {:changed boolean, :keys-added [string], :keys-removed [string], :keys-modified [string]}."
  [blob-a blob-b]
  (let [fm-a (extract-frontmatter blob-a)
        fm-b (extract-frontmatter blob-b)]
    (if (and fm-a fm-b)
      (let [vals-a (parse-frontmatter-keys fm-a)
            vals-b (parse-frontmatter-keys fm-b)
            keys-a (set (keys vals-a))
            keys-b (set (keys vals-b))]
      {:changed (not= fm-a fm-b)
         :keys-added (vec (set/difference keys-b keys-a))
         :keys-removed (vec (set/difference keys-a keys-b))
         :keys-modified (vec (filter #(and (contains? keys-a %)
                                           (contains? keys-b %)
                                           (not= (get vals-a %) (get vals-b %)))
                                     (set/intersection keys-a keys-b)))})
      {:changed false
       :keys-added []
       :keys-removed []
       :keys-modified []})))

(defn explicit-link-overlap
  "Extract explicit Markdown links and compute overlap between two blobs.
   Returns {:common [string], :added [string], :removed [string], :overlap-ratio double}."
  [blob-a blob-b]
  (let [extract-links (fn [blob]
                                                 (set (map first (re-seq #"\[([^\]]+)\]\(([^)]+)\)" blob))))
        links-a (extract-links blob-a)
        links-b (extract-links blob-b)
        common (set/intersection links-a links-b)
        added (set/difference links-b links-a)
        removed (set/difference links-a links-b)
        union (set/union links-a links-b)]
    {:common (vec common)
     :added (vec added)
     :removed (vec removed)
     :overlap-ratio (if (pos? (count union))
                      (double (/ (count common) (count union)))
                      0.0)}))

(defn time-gap
  "Compute time gap in seconds between two commit timestamps."
  [timestamp-a timestamp-b]
  (if (and timestamp-a timestamp-b)
    (let [diff-ms (- (.getTime ^java.util.Date timestamp-b)
                     (.getTime ^java.util.Date timestamp-a))]
      (abs (/ diff-ms 1000.0)))
    nil))

(defn shared-name-tokens
  "Compute shared tokens between file names (path segments).
   Returns {:common [string], :overlap-ratio double}."
  [path-a path-b]
  (let [tokenize (fn [path]
                   (let [basename (last (str/split path #"/"))
                         name-part (first (str/split basename #"\." 2))]
                     (set (str/split name-part #"[^a-zA-Z0-9]+"))))
        tokens-a (tokenize path-a)
        tokens-b (tokenize path-b)
        common (set/intersection tokens-a tokens-b)
        union (set/union tokens-a tokens-b)]
    {:common (vec common)
     :overlap-ratio (if (pos? (count union))
                      (double (/ (count common) (count union)))
                      0.0)}))

;; ---------------------------------------------------------------------------
;; Continuity scoring

(defn compute-continuity-score
  "Compute a continuity score from raw features.

   Returns a map:
     {:continuity/score double        — 0.0 (no continuity) to 1.0 (identical)
      :continuity/policy-version string
      :continuity/features {:text-similarity double
                            :frontmatter-changed boolean
                            :link-overlap-ratio double
                            :time-gap-seconds double-or-nil
                            :name-token-overlap double}}"
  [features]
  (let [{:keys [text-similarity frontmatter-delta link-overlap
                time-gap-seconds name-token-overlap]} features
        ;; Weighted scoring
        ;; text-similarity: 40%
        ;; link-overlap: 20%
        ;; name-token-overlap: 20%
        ;; frontmatter penalty: 10%
        ;; time decay: 10%
        fm-score (if (:changed frontmatter-delta) 0.0 1.0)
        time-score (if time-gap-seconds
                     (max 0.0 (- 1.0 (/ time-gap-seconds (* 365 24 3600)))) ;; decay over 1 year
                     0.5)
        score (+ (* 0.4 text-similarity)
                 (* 0.2 (:overlap-ratio link-overlap))
                 (* 0.2 (:overlap-ratio name-token-overlap))
                 (* 0.1 fm-score)
                 (* 0.1 time-score))]
    {:continuity/score (max 0.0 (min 1.0 score))
     :continuity/policy-version policy-version
     :continuity/features {:text-similarity text-similarity
                           :frontmatter-changed (:changed frontmatter-delta)
                           :frontmatter-keys-added (:keys-added frontmatter-delta)
                           :frontmatter-keys-removed (:keys-removed frontmatter-delta)
                           :link-overlap-ratio (:overlap-ratio link-overlap)
                           :link-common (:common link-overlap)
                           :link-added (:added link-overlap)
                           :link-removed (:removed link-overlap)
                           :time-gap-seconds time-gap-seconds
                           :name-token-overlap (:overlap-ratio name-token-overlap)
                           :name-token-common (:common name-token-overlap)}}))

;; ---------------------------------------------------------------------------
;; Main entry point

(defn compute-continuity
  "Compute continuity features and score for a pair of revisions.

   Parameters:
     blob-a, blob-b — raw blob contents (strings)
     path-a, path-b — file paths (strings)
     timestamp-a, timestamp-b — commit timestamps (java.util.Date or nil)

   Returns a map with:
     :continuity/score double
     :continuity/policy-version string
     :continuity/features map of all raw signals"
  [blob-a blob-b path-a path-b timestamp-a timestamp-b]
  (let [features {:text-similarity (text-similarity blob-a blob-b)
                  :frontmatter-delta (frontmatter-delta blob-a blob-b)
                  :link-overlap (explicit-link-overlap blob-a blob-b)
                  :time-gap-seconds (time-gap timestamp-a timestamp-b)
                  :name-token-overlap (shared-name-tokens path-a path-b)}]
    (compute-continuity-score features)))
