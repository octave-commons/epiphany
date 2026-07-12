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
         "**Generator:** " (:packet/generator-version packet) "\n\n"
         (str/join "\n\n" sections))))
