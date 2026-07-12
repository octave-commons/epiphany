(ns epiphany.domain.lineage
  "Deterministic candidate lineage link generation.

  Given a source section and a set of candidate target sections,
  produce typed provisional lineage relations based on continuity
  signals, content analysis, and temporal ordering.

  All output is provisional — promotion to accepted requires explicit
  human review. LLM-enhanced generation is a separate future card."
  (:require [epiphany.domain.continuity :as continuity]
            [epiphany.domain.diff :as diff]))

(def lineage-generator-version
  "Version string for the deterministic lineage generator."
  "lineage-deterministic-v1")

;; ---------------------------------------------------------------------------
;; Relation types

(def relation-types
  "Set of valid deterministic relation types."
  #{:near-duplicate
    :continues
    :refines
    :references
    :possibly-derived-from
    :possibly-supersedes
    :possible-contradiction})

;; ---------------------------------------------------------------------------
;; Heuristic classification

(defn- same-heading?
  "True if two sections share the same heading path."
  [source target]
  (= (:heading-path source) (:heading-path target)))

(defn- later-in-time?
  "True if target was committed after source."
  [source target]
  (and (:timestamp source) (:timestamp target)
       (.before ^java.util.Date (:timestamp source)
                ^java.util.Date (:timestamp target))))

(defn- text-got-longer?
  "True if target body is substantially longer than source body."
  [source target]
  (let [src-len (count (or (:body source) ""))
        tgt-len (count (or (:body target) ""))]
    (> tgt-len (* 1.3 src-len))))

(defn classify-relation
  "Classify the relation between a source and target section based on
   their continuity features and structural properties.

   Returns a set of relation types (a section may have multiple relations)."
  [source target features]
  (let [sim (:text-similarity features)
        link-ratio (:link-overlap-ratio features)
        name-overlap (:name-token-overlap features)
        fm-changed (:frontmatter-changed features)
        same-head (same-heading? source target)
        later (later-in-time? source target)
        longer (text-got-longer? source target)]
    (cond
      ;; Near-duplicate: very high similarity, same heading
      (and same-head (>= sim 0.9))
      #{:near-duplicate}

      ;; Refines: same heading + later + text got more detailed (check before :continues)
      (and same-head later (>= sim 0.5) longer)
      #{:refines :continues}

      ;; Continues: same heading, moderate-to-high similarity, later in time
      (and same-head later (>= sim 0.5) (< sim 0.9))
      #{:continues}

      ;; References: significant link overlap
      (>= link-ratio 0.5)
      #{:references}

      ;; Possibly-derived-from: shared name tokens + some text similarity
      (and (>= name-overlap 0.3) (>= sim 0.3))
      #{:possibly-derived-from}

      ;; Possibly-supersedes: later, frontmatter changed, lower similarity
      (and later fm-changed (< sim 0.5))
      #{:possibly-supersedes}

      ;; Possible-contradiction: same heading, low similarity (content diverged)
      (and same-head (< sim 0.3))
      #{:possible-contradiction}

      :else
      #{})))

;; ---------------------------------------------------------------------------
;; Confidence scoring

(defn compute-confidence
  "Compute a confidence score [0, 1] for a candidate lineage link.
   Higher when multiple signals agree and the relation is strong."
  [relation-type features]
  (let [sim (:text-similarity features)
        link-ratio (:link-overlap-ratio features)
        name-overlap (:name-token-overlap features)
        base (case relation-type
               :near-duplicate            sim
               :continues                 (* 0.6 sim (* 0.4 name-overlap))
               :refines                   (* 0.5 sim (* 0.3 name-overlap) 0.2)
               :references                link-ratio
               :possibly-derived-from     (* 0.5 sim (* 0.5 name-overlap))
               :possibly-supersedes       (* 0.4 sim (* 0.3 name-overlap) 0.3)
               :possible-contradiction    (- 1.0 sim)
               0.0)]
    (max 0.0 (min 1.0 (double base)))))

;; ---------------------------------------------------------------------------
;; Evidence spans

(defn- build-evidence-spans
  "Build a list of evidence spans describing what signals contributed
   to this candidate relation."
  [relation-type features]
  (let [spans (atom [])]
    (when (>= (:text-similarity features) 0.3)
      (swap! spans conj {:evidence/signal :text-similarity
                         :evidence/value (:text-similarity features)
                         :evidence/weight 0.4}))
    (when (>= (:link-overlap-ratio features) 0.1)
      (swap! spans conj {:evidence/signal :link-overlap
                         :evidence/value (:link-overlap-ratio features)
                         :evidence/weight 0.2
                         :evidence/detail {:common (:link-common features)
                                           :added (:link-added features)
                                           :removed (:link-removed features)}}))
    (when (>= (:name-token-overlap features) 0.1)
      (swap! spans conj {:evidence/signal :name-token-overlap
                         :evidence/value (:name-token-overlap features)
                         :evidence/weight 0.2
                         :evidence/detail {:common (:name-token-common features)}}))
    (when (:frontmatter-changed features)
      (swap! spans conj {:evidence/signal :frontmatter-change
                         :evidence/value 1.0
                         :evidence/weight 0.1}))
    (when-let [tg (:time-gap-seconds features)]
      (swap! spans conj {:evidence/signal :time-gap
                         :evidence/value tg
                         :evidence/weight 0.1}))
    @spans))

;; ---------------------------------------------------------------------------
;; Candidate generation

(defn- make-candidate
  "Create a single lineage candidate."
  [source target relation-type features]
  {:lineage-candidate/id (java.util.UUID/randomUUID)
   :lineage-candidate/source {:section/path-raw (:path-raw source)
                              :section/heading-path (:heading-path source)
                              :section/commit-oid (:commit-oid source)}
   :lineage-candidate/target {:section/path-raw (:path-raw target)
                              :section/heading-path (:heading-path target)
                              :section/commit-oid (:commit-oid target)}
   :lineage-candidate/relation relation-type
   :lineage-candidate/confidence (compute-confidence relation-type features)
   :lineage-candidate/evidence-spans (build-evidence-spans relation-type features)
   :lineage-candidate/features features
   :lineage-candidate/generator-version lineage-generator-version
   :lineage-candidate/status :provisional})

(defn generate-candidates
  "Generate lineage candidates for a source section against a collection
   of target sections.

   Parameters:
     source   — map with :path-raw, :heading-path, :commit-oid, :body, :timestamp
     targets  — seq of maps with same shape as source
     features-fn — (fn [source target] → continuity features map)

   Returns a seq of candidate maps, one per non-empty relation."
  [source targets features-fn]
  (for [target targets
        :let [features (features-fn source target)
              relations (classify-relation source target features)]
        :when (seq relations)]
    (for [rel relations]
      (make-candidate source target rel features))))

;; ---------------------------------------------------------------------------
;; Idempotent merge

(defn- candidate-key
  "Stable key for deduplication: source + target + relation type."
  [candidate]
  [(:section/path-raw (:lineage-candidate/source candidate))
   (:section/heading-path (:lineage-candidate/source candidate))
   (:section/commit-oid (:lineage-candidate/source candidate))
   (:section/path-raw (:lineage-candidate/target candidate))
   (:section/heading-path (:lineage-candidate/target candidate))
   (:section/commit-oid (:lineage-candidate/target candidate))
   (:lineage-candidate/relation candidate)])

(defn merge-candidates
  "Merge two seqs of candidates idempotently. When the same
   (source, target, relation) appears in both, keep the one with
   higher confidence and union the evidence spans."
  [candidates-a candidates-b]
  (let [by-key (reduce (fn [acc c]
                         (let [k (candidate-key c)]
                           (if-let [existing (get acc k)]
                             (if (> (:lineage-candidate/confidence c)
                                    (:lineage-candidate/confidence existing))
                               (assoc acc k c)
                               acc)
                             (assoc acc k c))))
                       {}
                       (concat candidates-a candidates-b))]
    (vals by-key)))
