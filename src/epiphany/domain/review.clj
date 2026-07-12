(ns epiphany.domain.review
  "Review decision events for lineage candidates.

  Review actions are append-only: they never rewrite the candidate or
  Git evidence. Rejected candidates remain in audit mode; do-not-suggest
  suppresses similar candidates in default views.

  Events carry request IDs so retries never duplicate decisions.
  Decisions are queryable by candidate, relation type, and time.")

(def review-decision-types
  "Set of valid review decision types."
  #{:accepted :rejected :relabel :deferred :annotated :do-not-suggest})

;; ---------------------------------------------------------------------------
;; Decision creation (pure)

(defn make-decision
  "Create a durable review decision event.

   Parameters:
     candidate-id — UUID of the lineage candidate being reviewed
     decision-type — keyword from review-decision-types
     options — optional map:
       :request-id UUID — for idempotent retries (generated if absent)
       :reason string — human-readable reason
       :relabel-to keyword — new relation type (for :relabel decisions)
       :annotation string — free-text annotation (for :annotated decisions)
       :suppressed boolean — whether to suppress in default views (for :do-not-suggest)

   Returns a decision map."
  [candidate-id decision-type & {:keys [request-id reason relabel-to
                                         annotation suppressed]
                                  :or {request-id (java.util.UUID/randomUUID)}}]
  (assert (contains? review-decision-types decision-type)
          (str "Invalid decision type: " decision-type))
  (cond-> {:review-decision/id (java.util.UUID/randomUUID)
           :review-decision/candidate-id candidate-id
           :review-decision/decision decision-type
           :review-decision/request-id request-id
           :review-decision/decided-at (java.util.Date.)}
    reason        (assoc :review-decision/reason reason)
    relabel-to    (assoc :review-decision/relabel-to relabel-to)
    annotation    (assoc :review-decision/annotation annotation)
    (some? suppressed) (assoc :review-decision/suppressed suppressed)))

;; ---------------------------------------------------------------------------
;; Query helpers (pure, over in-memory collections)

(defn by-candidate
  "Filter decisions to those targeting a specific candidate."
  [decisions candidate-id]
  (filter #(= candidate-id (:review-decision/candidate-id %)) decisions))

(defn by-decision-type
  "Filter decisions to those with a specific decision type."
  [decisions decision-type]
  (filter #(= decision-type (:review-decision/decision %)) decisions))

(defn by-time-range
  "Filter decisions to those within a time range [from, to).
   Both from and to are java.util.Date; nil means unbounded."
  [decisions from to]
  (filter (fn [d]
            (let [t (.getTime ^java.util.Date (:review-decision/decided-at d))]
              (and (or (nil? from) (>= t (.getTime ^java.util.Date from)))
                   (or (nil? to) (< t (.getTime ^java.util.Date to))))))
          decisions))

(defn by-request-id
  "Find a decision by its request ID (for idempotent retry detection)."
  [decisions request-id]
  (first (filter #(= request-id (:review-decision/request-id %)) decisions)))

(defn rejected-candidates
  "Return the set of candidate IDs that have been rejected or do-not-suggest'd."
  [decisions]
  (->> decisions
       (filter #(contains? #{:rejected :do-not-suggest} (:review-decision/decision %)))
       (map :review-decision/candidate-id)
       set))

(defn visible-decisions
  "Filter decisions for default view: excludes do-not-suggest decisions
   unless include-suppressed? is true."
  ([decisions]
   (visible-decisions decisions false))
  ([decisions include-suppressed?]
   (if include-suppressed?
     decisions
     (remove #(and (= :do-not-suggest (:review-decision/decision %))
                   (:review-decision/suppressed %))
             decisions))))
