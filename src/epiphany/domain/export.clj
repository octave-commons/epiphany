(ns epiphany.domain.export
  "Export evidence packets as Markdown plus EDN/JSON.

  The packet separates observed facts, inferred candidates, accepted
  interpretations, and open questions. Every claim carries an evidence
  reference or an explicit interpretation/no-direct-source label.
  Identifiers suffice to reproduce every lookup locally."
  (:require [clojure.string :as str]
            [clojure.data.json :as json]))

;; ---------------------------------------------------------------------------
;; Packet structure

(defn make-packet
  "Create an empty evidence packet.

   Parameters:
     options — map:
       :resource-id UUID — repository resource ID
       :label string — human-readable label for the packet
       :generator-version string — version of the export generator

   Returns a packet map."
  [& {:keys [resource-id label generator-version]
      :or {label "Evidence Packet" generator-version "export-v1"}}]
  {:packet/id (java.util.UUID/randomUUID)
   :packet/resource-id resource-id
   :packet/label label
   :packet/created-at (java.util.Date.)
   :packet/generator-version generator-version
   :packet/observed-facts []
   :packet/inferred-candidates []
   :packet/accepted-interpretations []
   :packet/open-questions []})

;; ---------------------------------------------------------------------------
;; Evidence references

(defn make-evidence-ref
  "Create an evidence reference.

   Parameters:
     source — map with :path-raw, :heading-path, :text, :section-id
     options — map:
       :context string — additional context
       :span {:start int :end int} — source spans

   Returns an evidence reference map."
  [source & {:keys [context span]}]
  {:evidence/section-id (:section-id source)
   :evidence/path-raw (:path-raw source)
   :evidence/heading-path (:heading-path source)
   :evidence/text (:text source)
   :evidence/context context
   :evidence/span span})

(defn make-no-source
  "Create an explicit no-direct-source label.

   Parameters:
     reason string — why there is no direct source

   Returns a source label map."
  [reason]
  {:source/type :no-direct-source
   :source/reason reason})

(defn make-interpretation-source
  "Create an interpretation source label.

   Parameters:
     interpretation string — the interpretation text
     interpreter string — who or what made the interpretation

   Returns a source label map."
  [interpretation interpreter]
  {:source/type :interpretation
   :source/interpretation interpretation
   :source/interpreter interpreter})

;; ---------------------------------------------------------------------------
;; Claim construction

(defn make-claim
  "Create a claim map.

   Parameters:
     claim-type keyword — :observed :inferred :accepted :question
     statement string — the claim
     source — evidence reference or source label
     options — map:
       :confidence double — confidence score
       :rationale string — rationale
       :identifiers map — reproduction identifiers

   Returns a claim map."
  [claim-type statement source & {:keys [confidence rationale identifiers]
                                   :or {confidence 0.0 rationale "" identifiers {}}}]
  {:claim/type claim-type
   :claim/statement statement
   :claim/source source
   :claim/confidence confidence
   :claim/rationale rationale
   :claim/identifiers identifiers})

;; ---------------------------------------------------------------------------
;; Packet population

(defn add-observed-fact
  "Add an observed fact to the packet."
  [packet claim]
  (update packet :packet/observed-facts conj claim))

(defn add-inferred-candidate
  "Add an inferred candidate to the packet."
  [packet claim]
  (update packet :packet/inferred-candidates conj claim))

(defn add-accepted-interpretation
  "Add an accepted interpretation to the packet."
  [packet claim]
  (update packet :packet/accepted-interpretations conj claim))

(defn add-open-question
  "Add an open question to the packet."
  [packet claim]
  (update packet :packet/open-questions conj claim))

;; ---------------------------------------------------------------------------
;; Bulk population from domain data

(defn populate-from-lineage
  "Populate packet from lineage candidate links.

   Parameters:
     packet — packet map
     candidates — seq of lineage candidate maps

   Returns updated packet."
  [packet candidates]
  (reduce (fn [pkt c]
            (let [source-a (make-evidence-ref
                            {:section-id (get-in c [:lineage/source :section/id])
                             :path-raw (get-in c [:lineage/source :section/path-raw])
                             :heading-path (get-in c [:lineage/source :section/heading-path])
                             :text (get-in c [:lineage/source :section/text])})
                  source-b (make-evidence-ref
                            {:section-id (get-in c [:lineage/target :section/id])
                             :path-raw (get-in c [:lineage/target :section/path-raw])
                             :heading-path (get-in c [:lineage/target :section/heading-path])
                             :text (get-in c [:lineage/target :section/text])})]
              (add-inferred-candidate
               pkt
               (make-claim :inferred
                           (format "%s candidate: %s"
                                   (name (:lineage/relation c))
                                   (or (:lineage/rationale c) ""))
                           source-a
                           :confidence (:lineage/confidence c 0.0)
                           :rationale (:lineage/rationale c "")
                           :identifiers {:lineage/relation (:lineage/relation c)
                                         :lineage/confidence (:lineage/confidence c)
                                         :source-b source-b}))))
          packet
          candidates))

(defn populate-from-redundancy
  "Populate packet from redundancy candidates.

   Parameters:
     packet — packet map
     candidates — seq of redundancy candidate maps

   Returns updated packet."
  [packet candidates]
  (reduce (fn [pkt c]
            (let [src-a (:redundancy-candidate/source-a c)
                  src-b (:redundancy-candidate/source-b c)
                  evidence-a (make-evidence-ref
                              {:section-id (:section/id src-a)
                               :path-raw (:section/path-raw src-a)
                               :heading-path (:section/heading-path src-a)
                               :text (:section/text src-a)})
                  evidence-b (make-evidence-ref
                              {:section-id (:section/id src-b)
                               :path-raw (:section/path-raw src-b)
                               :heading-path (:section/heading-path src-b)
                               :text (:section/text src-b)})]
              (add-inferred-candidate
               pkt
               (make-claim :inferred
                           (format "%s: %s"
                                   (name (:redundancy-candidate/relation c))
                                   (or (:redundancy-candidate/rationale c) ""))
                           evidence-a
                           :confidence (:redundancy-candidate/confidence c 0.0)
                           :rationale (:redundancy-candidate/rationale c "")
                           :identifiers {:relation (:redundancy-candidate/relation c)
                                         :evidence-b evidence-b}))))
          packet
          candidates))

(defn populate-from-decisions
  "Populate packet from review decisions.

   Parameters:
     packet — packet map
     decisions — seq of review decision maps

   Returns updated packet."
  [packet decisions]
  (reduce (fn [pkt d]
            (let [decision-type (:review/decision d)
                  source (if (:review/evidence d)
                           (first (:review/evidence d))
                           (make-no-source "No evidence in decision record"))]
              (if (= :accepted decision-type)
                (add-accepted-interpretation
                 pkt
                 (make-claim :accepted
                             (or (:review/rationale d) "Accepted decision")
                             source
                             :confidence (:review/confidence d 0.0)
                             :rationale (str "Decision: " (name decision-type))
                             :identifiers {:review/id (:review/id d)
                                           :review/request-id (:review/request-id d)}))
                pkt)))
          packet
          decisions))

;; ---------------------------------------------------------------------------
;; Population from the real durable stores (ENG-005A/005G)
;;
;; populate-from-lineage/populate-from-decisions above predate the durable
;; candidate (ENG-005G) and review-decision (ENG-005A) stores and were
;; written against a speculative :lineage/*/:review/* shape no producer
;; ever emitted. The functions below consume the real
;; :lineage-candidate/*/:review-decision/* records. Review decisions and
;; candidates are joined before claims are emitted so a reviewed candidate
;; cannot remain live in the inferred tier or lose its source evidence.

(defn- span->evidence-ref
  [span]
  (make-evidence-ref
   {:path-raw (:span/path-raw span)
    :heading-path (:span/heading-path span)}
   :context (when-let [commit-oid (:span/commit-oid span)]
              (str "commit " commit-oid))))

(defn- decision-time-ms
  [value]
  (cond
    (instance? java.util.Date value) (.getTime ^java.util.Date value)
    (number? value) (long value)
    :else Long/MIN_VALUE))

(defn- ordered-decisions
  [decisions]
  (sort-by (fn [decision]
             [(decision-time-ms (:review-decision/decided-at decision))
              (decision-time-ms (:observation/observed-at decision))
              (str (:review-decision/id decision))])
           decisions))

(defn- review-state
  "Fold append-only decisions into one effective candidate disposition.
   Relabels persist across later decisions; annotations add context without
   silently reopening an accepted or rejected candidate."
  [candidate decisions]
  (reduce
   (fn [state decision]
     (let [decision-type (:review-decision/decision decision)
           state (assoc state :latest-decision decision)]
       (case decision-type
         :accepted (assoc state :disposition :accepted)
         :rejected (assoc state :disposition :rejected)
         :do-not-suggest (assoc state :disposition :do-not-suggest)
         :deferred (assoc state :disposition :deferred)
         :relabel (cond-> state
                    (:review-decision/relabel-to decision)
                    (assoc :relation (:review-decision/relabel-to decision)))
         :annotated state
         state)))
   {:disposition :inferred
    :relation (:lineage-candidate/relation candidate)
    :decisions (vec (ordered-decisions decisions))}
   (ordered-decisions decisions)))

(defn- candidate-identifiers
  [candidate target {:keys [disposition relation decisions latest-decision]}]
  (cond-> {:lineage-candidate/id (:lineage-candidate/id candidate)
           :lineage-candidate/relation relation
           :lineage-candidate/original-relation (:lineage-candidate/relation candidate)
           :lineage-candidate/generator-version (:lineage-candidate/generator-version candidate)
           :source-commit-oid (get-in candidate [:lineage-candidate/source :span/commit-oid])
           :target-commit-oid (get-in candidate [:lineage-candidate/target :span/commit-oid])
           :target target
           :review/disposition disposition
           :review-decision/ids (mapv :review-decision/id decisions)}
    latest-decision
    (assoc :review-decision/id (:review-decision/id latest-decision)
           :review-decision/decision (:review-decision/decision latest-decision)
           :review-decision/decided-at (:review-decision/decided-at latest-decision))

    (:review-decision/relabel-to latest-decision)
    (assoc :review-decision/relabel-to (:review-decision/relabel-to latest-decision))

    (:review-decision/suppressed latest-decision)
    (assoc :review-decision/suppressed (:review-decision/suppressed latest-decision))))

(defn- review-rationale
  [candidate decisions]
  (str/join
   "; "
   (remove str/blank?
           (concat
            [(or (:lineage-candidate/rationale candidate)
                 (:lineage-candidate/evidence-summary candidate))]
            (mapcat (juxt :review-decision/reason
                          :review-decision/annotation)
                    decisions)))))

(defn- populate-candidate
  [packet candidate decisions]
  (let [state (review-state candidate decisions)
        relation (or (:relation state) :related)
        disposition (:disposition state)
        source (span->evidence-ref (:lineage-candidate/source candidate))
        target (span->evidence-ref (:lineage-candidate/target candidate))
        source-path (get-in candidate [:lineage-candidate/source :span/path-raw])
        target-path (get-in candidate [:lineage-candidate/target :span/path-raw])
        rationale (review-rationale candidate (:decisions state))
        identifiers (candidate-identifiers candidate target state)
        statement (format "%s candidate: %s -> %s"
                          (name relation) source-path target-path)]
    (cond
      (= :accepted disposition)
      (add-accepted-interpretation
       packet
       (make-claim :accepted
                   (str "Accepted " statement)
                   source
                   :confidence (:lineage-candidate/confidence candidate 0.0)
                   :rationale rationale
                   :identifiers identifiers))

      (contains? #{:rejected :do-not-suggest} disposition)
      (add-observed-fact
       packet
       (make-claim :observed
                   (str (if (= :rejected disposition) "Rejected " "Suppressed ")
                        statement)
                   source
                   :confidence 1.0
                   :rationale rationale
                   :identifiers identifiers))

      :else
      (add-inferred-candidate
       packet
       (make-claim :inferred
                   statement
                   source
                   :confidence (:lineage-candidate/confidence candidate 0.0)
                   :rationale rationale
                   :identifiers identifiers)))))

(defn populate-from-reviewed-lineage-candidates
  "Populate a packet by joining durable candidates with their append-only
   review decisions. Accepted candidates move to the accepted tier with
   their original evidence; rejected and do-not-suggest candidates become
   explicit observed review outcomes; relabels change the effective
   inferred relation; undecided/deferred candidates remain inferred."
  [packet candidates decisions]
  (let [decisions-by-candidate
        (group-by :review-decision/candidate-id decisions)]
    (reduce
     (fn [result candidate]
       (populate-candidate
        result
        candidate
        (get decisions-by-candidate (:lineage-candidate/id candidate) [])))
     packet
     candidates)))

(defn populate-from-lineage-candidates
  "Populate packet from durable lineage-candidate records (the real
   ENG-005G shape -- :lineage-candidate/source and :target are :span/*
   maps, not the earlier speculative :lineage/* shape).

   Parameters:
     packet — packet map
     candidates — seq of durable lineage-candidate records

   Returns updated packet."
  [packet candidates]
  (populate-from-reviewed-lineage-candidates packet candidates []))

(defn populate-from-concepts
  "Populate packet from concepts and research questions.

   Parameters:
     packet — packet map
     concepts — seq of concept maps
     research-questions — seq of research question maps

   Returns updated packet."
  [packet concepts research-questions]
  (let [pkt (reduce (fn [pkt c]
                      (let [source (if (seq (:concept/evidence-links c))
                                     (first (:concept/evidence-links c))
                                     (make-no-source "Concept has no linked evidence"))]
                        (add-accepted-interpretation
                         pkt
                         (make-claim :accepted
                                     (str "Concept: " (:concept/name c))
                                     source
                                     :confidence 1.0
                                     :rationale (:concept/description c)
                                     :identifiers {:concept/id (:concept/id c)
                                                   :concept/tags (:concept/tags c)}))))
                    packet
                    concepts)
        pkt2 (reduce (fn [pkt rq]
                       (let [source (if (seq (:research-question/evidence-links rq))
                                      (first (:research-question/evidence-links rq))
                                      (make-no-source "Research question has no linked evidence"))]
                         (add-open-question
                          pkt
                          (make-claim :question
                                      (:research-question/question rq)
                                      source
                                      :confidence (case (:research-question/priority rq)
                                                    :high 0.9
                                                    :medium 0.6
                                                    :low 0.3
                                                    0.5)
                                      :rationale (:research-question/interpretation rq)
                                      :identifiers {:rq/id (:research-question/id rq)
                                                    :rq/status (:research-question/status rq)}))))
                     pkt
                     research-questions)]
    pkt2))

(defn populate-from-gaps
  "Populate packet from research gaps.

   Parameters:
     packet — packet map
     gaps — seq of gap maps

   Returns updated packet."
  [packet gaps]
  (reduce (fn [pkt g]
            (add-open-question
             pkt
             (make-claim :question
                         (:gap/description g)
                         (if (seq (:gap/evidence g))
                           (first (:gap/evidence g))
                           (make-no-source "Gap has no linked evidence"))
                         :confidence (:gap/confidence g 0.0)
                         :rationale (str "Gap type: " (name (:gap/type g)))
                         :identifiers {:gap/type (:gap/type g)
                                       :gap/suggested-action (:gap/suggested-action g)})))
          packet
          gaps))

;; ---------------------------------------------------------------------------
;; Tamper evidence
;;
;; A packet previously carried only a random :packet/id, :packet/created-at,
;; and a generator-version string -- no way to detect a claim silently
;; edited after export (the 2026-07-13 review's flagged gap).

(defn- sha256-base64
  [^String s]
  (let [digest (.digest (java.security.MessageDigest/getInstance "SHA-256")
                        (.getBytes s "UTF-8"))]
    (.encodeToString (java.util.Base64/getEncoder) digest)))

(def ^:private claim-section-keys
  [:packet/observed-facts :packet/inferred-candidates
   :packet/accepted-interpretations :packet/open-questions])

(defn add-content-hash
  "Compute and attach a SHA-256 :packet/content-hash over the packet's
   claim sections. Call this LAST, after all population and before
   serializing -- the hash covers only the claims (not :packet/id or
   :packet/created-at, which are provenance metadata that would make two
   exports of the identical claims at different times spuriously differ)."
  [packet]
  (assoc packet :packet/content-hash
         (sha256-base64 (pr-str (select-keys packet claim-section-keys)))))

(defn content-hash-valid?
  "True when a packet's :packet/content-hash matches its actual claims --
   false if either was edited independently after export, or if no hash
   was ever attached (nil never silently 'passes')."
  [packet]
  (and (some? (:packet/content-hash packet))
       (= (:packet/content-hash packet)
          (sha256-base64 (pr-str (select-keys packet claim-section-keys))))))

;; ---------------------------------------------------------------------------
;; Serialization

(defn packet->edn
  "Serialize packet to EDN string."
  [packet]
  (pr-str packet))

(defn packet->json
  "Serialize packet to JSON string."
  [packet]
  (json/write-str packet :key-fn #(subs (str %) 1)))

;; ---------------------------------------------------------------------------
;; Markdown rendering

(defn- render-evidence
  "Render an evidence reference as Markdown."
  [source]
  (cond
    (:source/type source)
    (str "_[" (:source/type source) ": " (:source/reason source
                                                       (:source/interpretation source)) "]_")

    (:evidence/path-raw source)
    (str "`" (:evidence/path-raw source) "`"
         (when (seq (:evidence/heading-path source))
           (str " > " (str/join " > " (:evidence/heading-path source))))
         (when (:evidence/context source)
           (str " — " (:evidence/context source))))

    :else "_no source_"))

(defn- render-claim
  "Render a claim as Markdown."
  [claim]
  (let [type-label (case (:claim/type claim)
                     :observed "**Observed**"
                     :inferred "_Inferred_"
                     :accepted "**Accepted**"
                     :question "_Question_")]
    (str "- " type-label " " (:claim/statement claim)
         "\n  Source: " (render-evidence (:claim/source claim))
         (when (pos? (:claim/confidence claim))
           (str "\n  Confidence: " (format "%.2f" (:claim/confidence claim))))
         (when (seq (:claim/rationale claim))
           (str "\n  Rationale: " (:claim/rationale claim))))))

(defn packet->markdown
  "Render packet as Markdown."
  [packet]
  (let [sections []
        sections (if (seq (:packet/observed-facts packet))
                   (conj sections (str "## Observed Facts\n\n"
                                       (str/join "\n" (map render-claim (:packet/observed-facts packet)))))
                   sections)
        sections (if (seq (:packet/inferred-candidates packet))
                   (conj sections (str "## Inferred Candidates\n\n"
                                       (str/join "\n" (map render-claim (:packet/inferred-candidates packet)))))
                   sections)
        sections (if (seq (:packet/accepted-interpretations packet))
                   (conj sections (str "## Accepted Interpretations\n\n"
                                       (str/join "\n" (map render-claim (:packet/accepted-interpretations packet)))))
                   sections)
        sections (if (seq (:packet/open-questions packet))
                   (conj sections (str "## Open Questions\n\n"
                                       (str/join "\n" (map render-claim (:packet/open-questions packet)))))
                   sections)]
    (str "# " (:packet/label packet) "\n\n"
         "**Resource:** " (:packet/resource-id packet) "\n"
         "**Created:** " (:packet/created-at packet) "\n"
         "**Generator:** " (:packet/generator-version packet) "\n"
         (when (:packet/content-hash packet)
           (str "**Content-hash:** " (:packet/content-hash packet) "\n"))
         "\n"
         (str/join "\n\n" sections))))
