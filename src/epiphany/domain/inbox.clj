(ns epiphany.domain.inbox
  "Review inbox: filterable queue of unreviewed candidates with evidence.

  Provides a ranked, filtered view of lineage candidates that haven't
  been reviewed yet, enriched with evidence spans and context for
  efficient triage."
  (:require [epiphany.domain.review :as review]
            [epiphany.domain.candidates :as candidates]
            [clojure.string :as str]))

;; ---------------------------------------------------------------------------
;; Evidence summary

(defn summarize-evidence
  "Build a human-readable evidence summary for a candidate."
  [candidate]
  (let [features (:lineage-candidate/features candidate)
        spans (:lineage-candidate/evidence-spans candidate)
        rel (:lineage-candidate/relation candidate)
        conf (:lineage-candidate/confidence candidate)
        parts []]
    (let [parts (cond-> parts
                  true (conj (format "%s (confidence: %.2f)" (name rel) conf))
                  (:text-similarity features)
                  (conj (format "text-similarity: %.2f" (:text-similarity features)))
                  (:link-overlap-ratio features)
                  (conj (format "link-overlap: %.2f" (:link-overlap-ratio features)))
                  (:name-token-overlap features)
                  (conj (format "name-overlap: %.2f" (:name-token-overlap features)))
                  (:frontmatter-changed features)
                  (conj "frontmatter changed")
                  (seq spans)
                  (conj (format "%d evidence span(s)" (count spans))))]
      (str/join "; " parts))))

;; ---------------------------------------------------------------------------
;; Filtering

(defn- matches-relation-type?
  [candidate relation-types]
  (or (nil? relation-types)
      (empty? relation-types)
      (contains? (set relation-types) (:lineage-candidate/relation candidate))))

(defn- matches-confidence-band?
  [candidate [low high]]
  (let [conf (:lineage-candidate/confidence candidate)]
    (and (or (nil? low) (>= conf low))
         (or (nil? high) (<= conf high)))))

(defn- matches-generator?
  [candidate generators]
  (or (nil? generators)
      (empty? generators)
      (contains? (set generators) (:lineage-candidate/generator-version candidate))))

(defn- matches-date-range?
  [candidate [after before]]
  (let [ts (or (:lineage-candidate/timestamp candidate)
               (:lineage-candidate/generated-at candidate))]
    (or (nil? ts)
        (and (or (nil? after) (>= (.getTime ^java.util.Date ts)
                                  (.getTime ^java.util.Date after)))
             (or (nil? before) (< (.getTime ^java.util.Date ts)
                                  (.getTime ^java.util.Date before)))))))

(defn- matches-repository-family?
  "Repository-family filter: a candidate belongs to a repository family
   when its durable record's :resource-id is in the given set. A raw
   (non-durable) candidate map with no :resource-id never matches a
   repository-family filter -- it can't be scoped to a repo it doesn't
   carry an identity for."
  [candidate resource-ids]
  (or (nil? resource-ids)
      (empty? resource-ids)
      (contains? (set resource-ids) (:resource-id candidate))))

(defn- apply-filters
  [candidates filters]
  (let [{:keys [relation-types confidence-band generators date-range repository-families]} filters]
    (cond->> candidates
      relation-types (filter #(matches-relation-type? % relation-types))
      confidence-band (filter #(matches-confidence-band? % confidence-band))
      generators (filter #(matches-generator? % generators))
      date-range (filter #(matches-date-range? % date-range))
      repository-families (filter #(matches-repository-family? % repository-families)))))

;; ---------------------------------------------------------------------------
;; Ranking

(defn rank-by-confidence
  "Sort candidates by confidence descending (highest first)."
  [candidates]
  (sort-by #(:lineage-candidate/confidence %) > candidates))

(defn rank-by-evidence
  "Sort candidates by number of evidence spans descending."
  [candidates]
  (sort-by #(count (:lineage-candidate/evidence-spans %)) > candidates))

;; ---------------------------------------------------------------------------
;; Inbox building

(defn build-inbox
  "Build a review inbox from candidates and decisions.

   A candidate's disposition (epiphany.domain.candidates/disposition) is
   resolved from `decisions` by candidate id: only a terminal decision
   (accepted / rejected / do-not-suggest) settles it, matching ADR/CLAUDE.md's
   epistemic ladder -- relabel/deferred/annotated are neutral and leave a
   candidate in the queue. This is deliberately NOT the same thing as
   \"has any decision at all\": a suppressed (rejected / do-not-suggest)
   candidate is distinct from an already-decided one, and an :accepted
   candidate never resurfaces regardless of :include-suppressed?, since it's
   resolved rather than suppressed.

   Parameters:
     candidates — seq of lineage candidate maps (durable records carry
                  :resource-id, used by the repository-family filter)
     decisions — seq of review decision maps
     filters — map:
       :relation-types [keyword] — filter by relation type
       :confidence-band [double double] — [low high] confidence range
       :generators [string] — filter by generator version
       :date-range [Date Date] — [after before) timestamp range
       :repository-families [uuid] — filter by the candidate's :resource-id
     options — map:
       :limit int — max items (default 50)
       :sort keyword — :confidence (default) or :evidence
       :include-suppressed? boolean — also include rejected/do-not-suggest
         candidates (still excludes accepted, which are resolved, not
         suppressed) (default false)

   Returns a seq of inbox items, each a map:
     {:inbox/candidate candidate-map
      :inbox/decision-status keyword — the candidate's resolved disposition
        (:provisional / :rejected / :do-not-suggest; never :accepted, since
        accepted candidates are excluded)
      :inbox/evidence-summary string}"
  ([candidates decisions]
   (build-inbox candidates decisions {} {}))
  ([candidates decisions filters]
   (build-inbox candidates decisions filters {}))
  ([candidates decisions filters {:keys [limit sort include-suppressed?]
                                   :or {limit 50 sort :confidence
                                        include-suppressed? false}}]
   (let [queue (remove #(candidates/established? % decisions) candidates)
         queue (if include-suppressed?
                 queue
                 (filter #(candidates/surfaced? % decisions) queue))
         filtered (apply-filters queue filters)
         ranked (case sort
                  :evidence (rank-by-evidence filtered)
                  (rank-by-confidence filtered))
         limited (take limit ranked)]
     (map (fn [candidate]
            {:inbox/candidate candidate
             :inbox/decision-status (candidates/disposition candidate decisions)
             :inbox/evidence-summary (summarize-evidence candidate)})
          limited))))

(defn inbox-item-detail
  "Get detailed information for an inbox item."
  [item]
  (let [candidate (:inbox/candidate item)]
    {:source (get candidate :lineage-candidate/source)
     :target (get candidate :lineage-candidate/target)
     :relation (:lineage-candidate/relation candidate)
     :confidence (:lineage-candidate/confidence candidate)
     :features (:lineage-candidate/features candidate)
     :evidence-spans (:lineage-candidate/evidence-spans candidate)
     :generator-version (:lineage-candidate/generator-version candidate)}))
