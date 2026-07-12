(ns epiphany.domain.boundary
  "Deterministic boundary proposal from continuity signals.

  A boundary proposal records a transition between revisions where the
  continuity signals indicate a significant break. Proposals are always
  provisional — accepting or rejecting them is a separate durable event.

  Design rules:
  - A long time gap alone never creates a boundary (drift, not boundary).
  - Low continuity alone without corroborating signals is drift.
  - A boundary requires multiple signals to agree."
  (:require [epiphany.domain.continuity :as continuity]))

(def boundary-policy-version
  "Version string for the boundary proposal policy."
  "boundary-v1")

(def default-threshold
  "Default continuity score threshold below which a boundary is considered."
  0.40)

;; ---------------------------------------------------------------------------
;; Drift vs boundary classification

(defn- content-change-signal?
  "Returns true if the continuity features indicate actual content change
   (not just time passing). A content change signal is present when:
   - frontmatter changed, OR
   - text similarity is below 0.7, OR
   - link overlap is below 0.5, OR
   - name token overlap is below 0.5"
  [features]
  (or (:frontmatter-changed features)
      (< (:text-similarity features) 0.7)
      (< (:link-overlap-ratio features) 0.5)
      (< (:name-token-overlap features) 0.5)))

(defn- time-gap-dominated?
  "Returns true if the time gap is the dominant contributor to a low score.
   Recomputes the score with a neutral time contribution and checks whether
   the score would still be below threshold."
  [features threshold]
  (let [neutral-time-score 0.5
        original-time-score (if (:time-gap-seconds features)
                              (max 0.0 (- 1.0 (/ (:time-gap-seconds features)
                                                 (* 365 24 3600))))
                              0.5)
        ;; Recompute with neutral time
        adjusted-score (+ (* 0.4 (:text-similarity features))
                          (* 0.2 (:link-overlap-ratio features))
                          (* 0.2 (:name-token-overlap features))
                          (* 0.1 (if (:frontmatter-changed features) 0.0 1.0))
                          (* 0.1 neutral-time-score))]
    ;; If removing the time gap effect brings us above threshold,
    ;; then time gap was the dominant factor
    (>= adjusted-score threshold)))

;; ---------------------------------------------------------------------------
;; Boundary proposal

(defn classify-continuity
  "Classify a continuity score into a human-readable drift class."
  [score]
  (cond
    (>= score 0.90) :strong-continuity
    (>= score 0.70) :substantial-continuity
    (>= score 0.40) :partial-transforming
    (>= score 0.15) :weak-continuity
    :else :apparent-discontinuity))

(defn propose-boundary
  "Decide whether to propose a boundary based on continuity signals.

   Returns a map:
     {:proposed? boolean
      :drift-only? boolean — true when low score is due to time/drift, not content change
      :classification keyword
      :proposal (when proposed? {:boundary/score double
                                 :boundary/threshold double
                                 :boundary/policy-version string
                                 :boundary/from-scores map
                                 :continuity/features map})}

   A boundary is proposed ONLY when:
   1. The continuity score is below the threshold
   2. There is a content-change signal (not just a time gap)"
  ([continuity-result]
   (propose-boundary continuity-result default-threshold))
  ([continuity-result threshold]
   (let [score (:continuity/score continuity-result)
         features (:continuity/features continuity-result)
         below-threshold (< score threshold)
         classification (classify-continuity score)]
     (if (and below-threshold (content-change-signal? features))
       ;; Boundary proposal: low score AND content changed
       {:proposed? true
        :drift-only? false
        :classification classification
        :proposal {:boundary/score score
                   :boundary/threshold threshold
                   :boundary/policy-version boundary-policy-version
                   :boundary/from-scores {:text-similarity (:text-similarity features)
                                          :link-overlap-ratio (:link-overlap-ratio features)
                                          :name-token-overlap (:name-token-overlap features)
                                          :frontmatter-changed (:frontmatter-changed features)
                                          :time-gap-seconds (:time-gap-seconds features)}
                   :continuity/features features}}
       ;; No boundary: either above threshold or drift-only
       {:proposed? false
        :drift-only? (and below-threshold (not (content-change-signal? features)))
        :classification classification
        :proposal nil}))))

;; ---------------------------------------------------------------------------
;; Accept / reject durable events

(defn accept-proposal
  "Create a durable acceptance event for a boundary proposal.
   Returns a map representing the decision."
  [proposal]
  {:boundary-decision/id (java.util.UUID/randomUUID)
   :boundary-decision/decision :accepted
   :boundary-decision/proposal proposal
   :boundary-decision/decided-at (java.util.Date.)})

(defn reject-proposal
  "Create a durable rejection event for a boundary proposal.
   Returns a map representing the decision."
  ([proposal]
   (reject-proposal proposal nil))
  ([proposal reason]
   {:boundary-decision/id (java.util.UUID/randomUUID)
    :boundary-decision/decision :rejected
    :boundary-decision/proposal proposal
    :boundary-decision/reason reason
    :boundary-decision/decided-at (java.util.Date.)}))

(defn filter-visible
  "Filter boundary proposals to only those not rejected.
   Takes a sequence of {:proposal ... :decisions [...]} maps.
   A proposal is visible if it has no rejection decision."
  [proposals-with-decisions]
  (remove (fn [{:keys [decisions]}]
            (some #(= :rejected (:boundary-decision/decision %)) decisions))
          proposals-with-decisions))
