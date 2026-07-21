(ns epiphany.domain.candidates
  "Durable lineage-candidate records: pure construction, the seam that
  turns lineage projections into storable candidates, query filters, and
  the disposition join against review-decision events.

  A candidate is generated at the PROVISIONAL tier (retrieval / heuristic
  / model output). It is NEVER accepted or rejected by its own record;
  promotion flows only through `epiphany.domain.review` decision events
  keyed on `:lineage-candidate/id`. `disposition` performs that join, and
  `established?`/`surfaced?` enforce that a rejected or do-not-suggest
  candidate is never surfaced as an established relation (ADR / CLAUDE.md
  epistemic ladder — never collapse PROVISIONAL into ACCEPTED silently).

  Records are append-only and idempotent by request-id at the port; this
  namespace is pure — no I/O, no adapter knowledge."
  (:require [epiphany.domain.review :as review]))

(def relation-types
  "Valid lineage-candidate relation types. This is the SAME vocabulary
  `epiphany.domain.lineage/relation-types` produces, reused by value.
  Declared here (and in the law schema) rather than imported to keep the
  dependency graph acyclic; kept identical to lineage/relation-types."
  #{:near-duplicate :continues :refines :references
    :possibly-derived-from :possibly-supersedes :possible-contradiction})

;; ---------------------------------------------------------------------------
;; Construction (pure)

(defn make-span
  "Build one evidence endpoint (a `lineage-candidate/span`): an exact
  observed path, its heading path, and the Git commit it was seen at."
  [{:keys [path-raw heading-path commit-oid]}]
  {:span/path-raw path-raw
   :span/heading-path (vec heading-path)
   :span/commit-oid commit-oid})

(defn make-candidate
  "Create a provisional lineage candidate relating `source-span` to
  `target-span` under `relation`.

   Parameters:
     relation     — keyword from relation-types
     source-span  — a span map (see make-span)
     target-span  — a span map (see make-span)
     options:
       :confidence double [0,1] — required generation confidence
       :generator-version string — required generator identity
       :request-id UUID — idempotency key (generated if absent)
       :candidate-id UUID — stable candidate identity (generated if absent)
       :generated-at java.util.Date — generation time (default: now)

   Returns an internal candidate map (payload + :lineage-candidate/request-id).
   Wrap it with `candidate->observation` for durable persistence."
  [relation source-span target-span
   & {:keys [confidence generator-version request-id candidate-id generated-at]
      :or {request-id (java.util.UUID/randomUUID)
           candidate-id (java.util.UUID/randomUUID)
           generated-at (java.util.Date.)}}]
  (assert (contains? relation-types relation)
          (str "Invalid relation: " relation))
  (assert (number? confidence) "make-candidate requires :confidence")
  (assert generator-version "make-candidate requires :generator-version")
  {:lineage-candidate/id candidate-id
   :lineage-candidate/relation relation
   :lineage-candidate/confidence (double confidence)
   :lineage-candidate/generator-version generator-version
   :lineage-candidate/source source-span
   :lineage-candidate/target target-span
   :lineage-candidate/tier :provisional
   :lineage-candidate/request-id request-id
   :lineage-candidate/generated-at generated-at})

(defn candidate->observation
  "Wrap a candidate (from `make-candidate` / `from-lineage-candidate`) into
  a durable `observation/lineage-candidate-v1` record for the observations
  port.

  Pure: the caller supplies all provenance. The candidate's
  `:lineage-candidate/request-id` becomes the observation's
  `:observation/request-id` — the idempotency key the port dedups on, so a
  retry carrying the same request-id never appends a second candidate.

   Context map:
     :resource-id     — UUID of the registered repository (required)
     :adapter-version — recording adapter/contract version string (required)
     :observed-at     — java.util.Date the platform recorded it (default: now)
     :observation-id  — envelope id (default: a fresh random UUID)

   Returns a record satisfying observation/lineage-candidate-v1."
  [candidate {:keys [resource-id adapter-version observed-at observation-id]
              :or {observed-at (java.util.Date.)
                   observation-id (java.util.UUID/randomUUID)}}]
  (assert resource-id "candidate->observation requires :resource-id")
  (assert adapter-version "candidate->observation requires :adapter-version")
  {:observation/id observation-id
   :observation/type :lineage/candidate-generated
   :observation/observed-at observed-at
   :observation/adapter-version adapter-version
   :observation/schema-version 1
   :observation/request-id (:lineage-candidate/request-id candidate)
   :resource-id resource-id
   :lineage-candidate/id (:lineage-candidate/id candidate)
   :lineage-candidate/relation (:lineage-candidate/relation candidate)
   :lineage-candidate/generator-version (:lineage-candidate/generator-version candidate)
   :lineage-candidate/confidence (:lineage-candidate/confidence candidate)
   :lineage-candidate/source (:lineage-candidate/source candidate)
   :lineage-candidate/target (:lineage-candidate/target candidate)
   :lineage-candidate/tier :provisional
   :lineage-candidate/generated-at (:lineage-candidate/generated-at candidate)})

;; ---------------------------------------------------------------------------
;; Generation seam
;;
;; The lineage cards (ENG-004*/005B) generate candidates via
;; epiphany.domain.lineage/generate-candidates, which nests source/target
;; under :section/* keys. `from-lineage-candidate` is the tested seam that
;; maps one such candidate onto the durable span shape, ready for
;; candidate->observation. Anything richer (batch dedup, run-scoped
;; request-id derivation, model-generated candidates) is deferred to the
;; lineage cards — this card delivers a clean, tested recording seam only.

(defn from-lineage-candidate
  "Adapt a candidate produced by `epiphany.domain.lineage/generate-candidates`
  into a durable store candidate. The lineage candidate nests source/target
  under `:section/*` keys; this maps them onto the `:span/*` span shape and
  preserves relation, confidence, and generator version.

   options:
     :request-id UUID — idempotency key (generated if absent)
     :generated-at java.util.Date — generation time (default: now)

   The lineage candidate's own :lineage-candidate/id is preserved when
   present so decisions recorded against it join correctly."
  [lc & {:keys [request-id generated-at]
         :or {request-id (java.util.UUID/randomUUID)
              generated-at (java.util.Date.)}}]
  (let [->span (fn [s] (make-span {:path-raw (:section/path-raw s)
                                   :heading-path (:section/heading-path s)
                                   :commit-oid (:section/commit-oid s)}))]
    (make-candidate (:lineage-candidate/relation lc)
                    (->span (:lineage-candidate/source lc))
                    (->span (:lineage-candidate/target lc))
                    :confidence (:lineage-candidate/confidence lc)
                    :generator-version (:lineage-candidate/generator-version lc)
                    :candidate-id (or (:lineage-candidate/id lc)
                                      (java.util.UUID/randomUUID))
                    :request-id request-id
                    :generated-at generated-at)))

;; ---------------------------------------------------------------------------
;; Query helpers (pure, over persisted candidate records)

(defn by-candidate-id
  "Find the candidate record(s) with a specific :lineage-candidate/id."
  [candidates candidate-id]
  (filter #(= candidate-id (:lineage-candidate/id %)) candidates))

(defn by-relation
  "Filter candidates to a specific relation type."
  [candidates relation]
  (filter #(= relation (:lineage-candidate/relation %)) candidates))

(defn by-generator-version
  "Filter candidates to a specific generator version."
  [candidates generator-version]
  (filter #(= generator-version (:lineage-candidate/generator-version %)) candidates))

(defn by-confidence-band
  "Filter candidates whose confidence falls in [low, high]. nil bound = open."
  [candidates low high]
  (filter (fn [c]
            (let [conf (:lineage-candidate/confidence c)]
              (and (or (nil? low) (>= conf low))
                   (or (nil? high) (<= conf high)))))
          candidates))

(defn by-time-range
  "Filter candidates generated within [from, to). Both java.util.Date;
   nil means unbounded."
  [candidates from to]
  (filter (fn [c]
            (let [t (.getTime ^java.util.Date (:lineage-candidate/generated-at c))]
              (and (or (nil? from) (>= t (.getTime ^java.util.Date from)))
                   (or (nil? to) (< t (.getTime ^java.util.Date to))))))
          candidates))

;; ---------------------------------------------------------------------------
;; Disposition join (candidates + review-decision events)

(def disposition-decisions
  "Review decision types that carry a terminal disposition. relabel /
  deferred / annotated are neutral — they do not settle a candidate."
  #{:accepted :rejected :do-not-suggest})

(defn disposition
  "Resolve a candidate's current disposition by joining review-decision
  events on :lineage-candidate/id. The latest disposition-bearing decision
  (by :review-decision/decided-at) wins; with none, the candidate is
  :provisional. Returns one of :provisional / :accepted / :rejected /
  :do-not-suggest.

  Neutral decisions (relabel / deferred / annotated) never change the
  disposition — a candidate is promoted out of PROVISIONAL only by an
  explicit accept/reject/do-not-suggest event (epistemic ladder)."
  [candidate decisions]
  (let [cid (:lineage-candidate/id candidate)
        relevant (->> (review/by-candidate decisions cid)
                      (filter #(contains? disposition-decisions
                                          (:review-decision/decision %)))
                      (sort-by #(.getTime ^java.util.Date (:review-decision/decided-at %))))]
    (if-let [latest (last relevant)]
      (:review-decision/decision latest)
      :provisional)))

(defn established?
  "True only when the candidate has been explicitly ACCEPTED. Provisional,
  rejected, and do-not-suggest candidates are NOT established — this is the
  guard that stops a rejected candidate being surfaced as a settled
  relation."
  [candidate decisions]
  (= :accepted (disposition candidate decisions)))

(defn surfaced?
  "Whether a candidate may appear in default views at all. A rejected or
  do-not-suggest candidate is suppressed; provisional and accepted are
  surfaced."
  [candidate decisions]
  (not (contains? #{:rejected :do-not-suggest}
                  (disposition candidate decisions))))

(defn with-disposition
  "Annotate each candidate with its resolved :lineage-candidate/disposition."
  [candidates decisions]
  (map #(assoc % :lineage-candidate/disposition (disposition % decisions))
       candidates))

(defn established-candidates
  "The candidates that have been explicitly accepted."
  [candidates decisions]
  (filter #(established? % decisions) candidates))

(defn surfaced-candidates
  "The candidates fit to surface in default views (excludes rejected /
  do-not-suggest)."
  [candidates decisions]
  (filter #(surfaced? % decisions) candidates))
