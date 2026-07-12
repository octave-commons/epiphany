(ns epiphany.domain.research-gap
  "Surface research gaps in the lineage view.

  Heuristics over trace/review data to find:
  - Unresolved contradictions
  - Recurring TODO markers / open questions
  - Abrupt low-continuity transitions
  - Isolated late claims
  - Repeated near-duplicates

  Every suggested gap links to exact evidence, never a bare generated statement.
  A gap can become a research-question record in one action."
  (:require [clojure.string :as str]
            [epiphany.domain.concept :as concept]))

;; ---------------------------------------------------------------------------
;; Gap types

(def gap-types
  "Set of valid gap types."
  #{:unresolved-contradiction
    :todo-marker
    :low-continuity-transition
    :isolated-claim
    :repeated-near-duplicate})

;; ---------------------------------------------------------------------------
;; Evidence link helpers

(defn- make-evidence-link
  "Create an evidence link map for a gap."
  [section-id path-raw heading-path text & {:keys [context]
                                            :or {context ""}}]
  {:evidence/section-id section-id
   :evidence/path-raw path-raw
   :evidence/heading-path heading-path
   :evidence/text text
   :evidence/context context})

;; ---------------------------------------------------------------------------
;; TODO marker detection

(def todo-patterns
  "Patterns indicating open questions or TODOs."
  [#"TODO\b" #"FIXME\b" #"HACK\b" #"XXX\b"
   #"\?\?\?" #"(?i)research question" #"(?i)open question"
   #"(?i)needs? (?:further|more) (?:research|investigation)"
   #"(?i)unclear" #"(?i)unresolved"])

(defn detect-todo-markers
  "Find sections containing TODO/question markers.

   Parameters:
     sections — seq of {:id any :text string :path-raw string :heading-path [string]}

   Returns a seq of gap maps."
  [sections]
  (for [section sections
        :let [text (:text section)
              matches (mapcat (fn [pat] (re-seq pat text)) todo-patterns)]
        :when (seq matches)]
    {:gap/type :todo-marker
     :gap/description (format "TODO/question markers found: %s"
                              (str/join ", " (map str matches)))
     :gap/confidence 0.8
     :gap/evidence [(make-evidence-link
                     (:id section) (:path-raw section) (:heading-path section)
                     text
                     :context (format "Found: %s" (str/join ", " (map str matches))))]
     :gap/suggested-action :create-research-question
     :gap/generator-version "research-gap-deterministic-v1"
     :gap/status :provisional}))

;; ---------------------------------------------------------------------------
;; Low-continuity transition detection

(defn detect-low-continuity-transitions
  "Find sections with abrupt transitions (low continuity scores).

   Parameters:
     sections — seq of {:id any :text string :path-raw string :heading-path [string]}
     continuity-scores — map of section-id → double (0.0 = no continuity)

   Returns a seq of gap maps."
  [sections continuity-scores & {:keys [threshold]
                                  :or {threshold 0.2}}]
  (let [scored (map (fn [s]
                       {:section s
                        :score (get continuity-scores (:id s) 1.0)})
                     sections)
        low-scored (filter #(< (:score %) threshold) scored)]
    (for [{:keys [section score]} low-scored]
      {:gap/type :low-continuity-transition
       :gap/description (format "Low continuity (%.2f) — abrupt transition detected" score)
       :gap/confidence (double (- 1.0 score))
       :gap/evidence [(make-evidence-link
                       (:id section) (:path-raw section) (:heading-path section)
                       (:text section)
                       :context (format "Continuity score: %.2f" score))]
       :gap/suggested-action :investigate-transition
       :gap/generator-version "research-gap-deterministic-v1"
       :gap/status :provisional})))

;; ---------------------------------------------------------------------------
;; Contradiction detection (from review decisions)

(defn detect-contradictions
  "Find unresolved contradictions from review decisions.

   Parameters:
     decisions — seq of review decision maps (with :review/decision :possible-contradiction)

   Returns a seq of gap maps."
  [decisions]
  (let [contradictions (filter #(= :possible-contradiction (:review/relation %))
                               decisions)]
    (for [d contradictions
          :let [evidence (or (:review/evidence d) [])
                confidence (:review/confidence d 0.5)]]
      {:gap/type :unresolved-contradiction
       :gap/description (format "Unresolved contradiction (confidence %.2f): %s"
                                confidence (or (:review/rationale d) "no rationale"))
       :gap/confidence (double confidence)
       :gap/evidence (vec evidence)
       :gap/suggested-action :resolve-contradiction
       :gap/generator-version "research-gap-deterministic-v1"
       :gap/status :provisional})))

;; ---------------------------------------------------------------------------
;; Isolated late claim detection

(defn detect-isolated-claims
  "Find claims that appear late with few connections.

   Parameters:
     sections — seq of {:id any :text string :path-raw string :heading-path [string] :position int}
     lineage-links — seq of lineage link maps (each with :lineage/source :lineage/target)
     options — map:
       :min-position double — minimum position fraction to be 'late' (default 0.8)
       :max-connections int — maximum connections to be 'isolated' (default 1)

   Returns a seq of gap maps."
  [sections lineage-links & {:keys [min-position max-connections]
                              :or {min-position 0.8 max-connections 1}}]
  (let [total (count sections)
        source-counts (frequencies (map :lineage/source lineage-links))
        target-counts (frequencies (map :lineage/target lineage-links))
        connection-counts (merge-with + source-counts target-counts)
        late-sections (filter #(>= (double (/ (:position %) (max total 1)))
                                   min-position)
                              sections)]
    (for [section late-sections
          :let [connections (get connection-counts (:id section) 0)]
          :when (<= connections max-connections)]
      {:gap/type :isolated-claim
       :gap/description (format "Isolated claim at position %d with %d connections"
                                (:position section) connections)
       :gap/confidence 0.6
       :gap/evidence [(make-evidence-link
                       (:id section) (:path-raw section) (:heading-path section)
                       (:text section)
                       :context (format "Position %d/%d, connections: %d"
                                       (:position section) total connections))]
       :gap/suggested-action :investigate-isolation
       :gap/generator-version "research-gap-deterministic-v1"
       :gap/status :provisional})))

;; ---------------------------------------------------------------------------
;; Near-duplicate detection (from redundancy candidates)

(defn detect-near-duplicates
  "Find repeated near-duplicates from redundancy candidates.

   Parameters:
     redundancy-candidates — seq of redundancy candidate maps

   Returns a seq of gap maps."
  [redundancy-candidates]
  (let [near-dupes (filter #(= :near-duplicate (:redundancy-candidate/relation %))
                           redundancy-candidates)]
    (for [rd near-dupes
          :let [confidence (:redundancy-candidate/confidence rd 0.5)
                source-a (:redundancy-candidate/source-a rd)
                source-b (:redundancy-candidate/source-b rd)]]
      {:gap/type :repeated-near-duplicate
       :gap/description (format "Near-duplicate sections (confidence %.2f)" confidence)
       :gap/confidence (double confidence)
       :gap/evidence (cond-> []
                       source-a (conj (make-evidence-link
                                       (:section/id source-a)
                                       (:section/path-raw source-a)
                                       (:section/heading-path source-a)
                                       (:section/text source-a)
                                       :context "Source A"))
                       source-b (conj (make-evidence-link
                                       (:section/id source-b)
                                       (:section/path-raw source-b)
                                       (:section/heading-path source-b)
                                       (:section/text source-b)
                                       :context "Source B")))
       :gap/suggested-action :consolidate-or-deduplicate
       :gap/generator-version "research-gap-deterministic-v1"
       :gap/status :provisional})))

;; ---------------------------------------------------------------------------
;; Aggregate surfacing

(defn surface-gaps
  "Surface research gaps from all available data.

   Parameters:
     data — map with keys:
       :sections — seq of section maps
       :continuity-scores — map of section-id → double
       :decisions — seq of review decision maps
       :lineage-links — seq of lineage link maps
       :redundancy-candidates — seq of redundancy candidate maps
     options — map:
       :continuity-threshold double — threshold for low continuity (default 0.2)
       :late-position double — threshold for late claims (default 0.8)
       :max-isolation-connections int — max connections for isolated (default 1)

   Returns a seq of gap maps, sorted by confidence descending."
  [data & {:keys [continuity-threshold late-position max-isolation-connections]
           :or {continuity-threshold 0.2 late-position 0.8 max-isolation-connections 1}}]
  (let [sections (get data :sections [])
        gaps (vec
               (concat
                 (when (seq sections)
                   (detect-todo-markers sections))
                 (when (and (seq sections) (seq (:continuity-scores data)))
                   (detect-low-continuity-transitions
                    sections (:continuity-scores data)
                    :threshold continuity-threshold))
                 (when (seq (:decisions data))
                   (detect-contradictions (:decisions data)))
                 (when (and (seq sections) (seq (:lineage-links data)))
                   (detect-isolated-claims
                    sections (:lineage-links data)
                    :min-position late-position
                    :max-connections max-isolation-connections))
                 (when (seq (:redundancy-candidates data))
                   (detect-near-duplicates (:redundancy-candidates data)))))]
    (->> gaps
         (sort-by :gap/confidence >)
         vec)))

;; ---------------------------------------------------------------------------
;; Gap → research question conversion

(defn gap->research-question
  "Convert a gap into a research-question record.

   Links the gap's evidence to the new research question.

   Parameters:
     gap — a gap map from surface-gaps

   Returns a research-question map (compatible with concept/make-research-question)."
  [gap]
  (concept/make-research-question
   (:gap/description gap)
   :interpretation (format "Detected by %s heuristic" (name (:gap/type gap)))
   :evidence-links (:gap/evidence gap)
   :created-by "research-gap-detector"
   :priority (case (:gap/type gap)
               :unresolved-contradiction :high
               :todo-marker :medium
               :low-continuity-transition :medium
               :isolated-claim :low
               :repeated-near-duplicate :low
               :medium)
   :status :open))
