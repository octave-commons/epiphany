(ns epiphany.domain.redundancy
  "Deterministic redundancy and contradiction detection between sections.

  Classifies pairs of sections into relation types using deterministic
  patterns: negation, mutually exclusive values, incompatible dates/decisions.

  Each proposed relationship carries source spans, a score, and a rationale.
  Classifier output can be evaluated against a human-labeled review set."
  (:require [clojure.string :as str]
            [clojure.set]))

;; ---------------------------------------------------------------------------
;; Relation types

(def relation-types
  "Set of valid redundancy/contradiction relation types."
  #{:duplicate :near-duplicate :complementary :superseded
    :possible-contradiction :unclear})

;; ---------------------------------------------------------------------------
;; Pattern detection

(def negation-patterns
  "Regex patterns that indicate negation."
  [#"is not\b" #"does not\b" #"do not\b" #"cannot\b" #"never\b"
   #"no\b" #"not\b" #"neither\b" #"nor\b" #"without\b" #"lack\b"])

(defn- detect-negation
  "Check if text contains negation patterns."
  [text]
  (some #(re-find % (str/lower-case text)) negation-patterns))

(defn- negation-in-pair
  "Returns [:negated-a | :negated-b | :both-negated | nil] if one or both
   texts contain negation patterns."
  [text-a text-b]
  (let [neg-a (detect-negation text-a)
        neg-b (detect-negation text-b)]
    (cond
      (and neg-a neg-b) :both-negated
      neg-a :negated-a
      neg-b :negated-b
      :else nil)))

(defn- extract-claims
  "Extract simple claims from text by splitting on sentence boundaries."
  [text]
  (->> (str/split text #"[.!?]+")
       (map str/trim)
       (filter seq)))

(defn- word-overlap
  "Compute word-level Jaccard similarity between two texts."
  [text-a text-b]
  (let [words-a (set (str/split (str/lower-case text-a) #"\s+"))
        words-b (set (str/split (str/lower-case text-b) #"\s+"))
        intersection (count (clojure.set/intersection words-a words-b))
        union (count (clojure.set/union words-a words-b))]
    (if (pos? union) (/ (double intersection) union) 0.0)))

(defn- sentence-overlap
  "Compute sentence-level overlap between two texts."
  [text-a text-b]
  (let [claims-a (set (extract-claims text-a))
        claims-b (set (extract-claims text-b))
        intersection (count (clojure.set/intersection claims-a claims-b))
        union (count (clojure.set/union claims-a claims-b))]
    (if (pos? union) (/ (double intersection) union) 0.0)))

(defn- detect-mutual-exclusion
  "Detect mutually exclusive values (e.g., 'X is A' vs 'X is B')."
  [text-a text-b]
  (let [claims-a (extract-claims text-a)
        claims-b (extract-claims text-b)
        ;; Look for patterns like "X is A" vs "X is B" where A != B
        extract-subject-pred (fn [claim]
                               (when-let [[_ subj pred] (re-matches #"(?i)(.+?)\s+(?:is|are|was|were)\s+(.+)" claim)]
                                 [(str/lower-case (str/trim subj)) (str/lower-case (str/trim pred))]))
        pairs-a (into {} (keep extract-subject-pred) claims-a)
        pairs-b (into {} (keep extract-subject-pred) claims-b)
        shared-keys (clojure.set/intersection (set (keys pairs-a)) (set (keys pairs-b)))]
    (some (fn [k]
            (let [val-a (get pairs-a k)
                  val-b (get pairs-b k)]
              (and val-a val-b (not= val-a val-b))))
          shared-keys)))

;; ---------------------------------------------------------------------------
;; Classification

(defn classify-pair
  "Classify the relationship between two sections.

   Parameters:
     text-a, text-b — section body text
     features-a, features-b — optional feature maps (for enhanced detection)

   Returns a map:
     {:relation keyword
      :confidence double [0, 1]
      :rationale string
      :signals [{:signal keyword :value any :weight double}]}"
  ([text-a text-b]
   (classify-pair text-a text-b {} {}))
  ([text-a text-b features-a features-b]
   (let [word-sim (word-overlap text-a text-b)
         sent-sim (sentence-overlap text-a text-b)
         neg-info (negation-in-pair text-a text-b)
         mutual-excl (detect-mutual-exclusion text-a text-b)
         signals (atom [])]

     ;; Exact duplicate: identical text
     (if (= (str/trim text-a) (str/trim text-b))
       {:relation :duplicate
        :confidence 1.0
        :rationale "Identical text"
        :signals [{:signal :exact-match :value true :weight 1.0}]}

       ;; Otherwise classify based on signals
       (do
         (when (> word-sim 0.9)
           (swap! signals conj {:signal :word-overlap :value word-sim :weight 0.4}))
         (when (> sent-sim 0.5)
           (swap! signals conj {:signal :sentence-overlap :value sent-sim :weight 0.3}))
         (when neg-info
           (swap! signals conj {:signal :negation :value neg-info :weight 0.5}))
         (when mutual-excl
           (swap! signals conj {:signal :mutual-exclusion :value true :weight 0.6}))

         (let [total-weight (reduce + 0.0 (map :weight @signals))
               neg-weight (if neg-info 0.5 0.0)
               excl-weight (if mutual-excl 0.6 0.0)]

           (cond
             ;; High word overlap without negation → near-duplicate
             (and (> word-sim 0.7) (not neg-info))
             {:relation :near-duplicate
              :confidence (* word-sim 0.9)
              :rationale (format "High word overlap (%.2f)" word-sim)
              :signals @signals}

             ;; Negation + some overlap → possible contradiction
             (and neg-info (not= neg-info :both-negated) (> word-sim 0.3))
             {:relation :possible-contradiction
              :confidence (min 1.0 (+ (* 0.4 word-sim) neg-weight))
              :rationale (format "Negation detected (%s) with moderate overlap (%.2f)"
                                 (name neg-info) word-sim)
              :signals @signals}

             ;; Mutual exclusion → possible contradiction
             mutual-excl
             {:relation :possible-contradiction
              :confidence (min 1.0 (+ (* 0.3 word-sim) excl-weight))
              :rationale "Mutually exclusive claims detected"
              :signals @signals}

             ;; Complementary: moderate overlap, different content
             (and (> word-sim 0.2) (< word-sim 0.7))
             {:relation :complementary
              :confidence (* word-sim 0.8)
              :rationale (format "Moderate overlap (%.2f), different content" word-sim)
              :signals @signals}

             ;; Superseded: one claims to replace/update the other
             (and (some #(re-find % (str/lower-case text-b))
                        [#"replaces?" #"supersedes?" #"updates?" #"newer version"])
                  (> word-sim 0.2))
             {:relation :superseded
              :confidence (min 1.0 (* word-sim 1.2))
              :rationale "Replacement language detected in target"
              :signals @signals}

             ;; Low overlap, no clear pattern
             :else
             {:relation :unclear
              :confidence (* word-sim 0.5)
              :rationale (format "Low overlap (%.2f), no clear pattern" word-sim)
              :signals @signals})))))))

;; ---------------------------------------------------------------------------
;; Batch detection

(defn detect-pairs
  "Detect redundancy/contradiction candidates across all pairs of sections.

   Parameters:
     sections — seq of {:id any :text string :path-raw string :heading-path [string]}
     options — map:
       :min-confidence double — minimum confidence threshold (default 0.3)
       :max-pairs int — maximum number of pairs to return (default 100)

   Returns a seq of candidate maps, sorted by confidence descending."
  [sections & {:keys [min-confidence max-pairs]
               :or {min-confidence 0.3 max-pairs 100}}]
  (let [pairs (for [a sections
                    b sections
                    :when (not= (:id a) (:id b))
                    :when (< (hash (:id a)) (hash (:id b)))]  ;; unique pairs
                [a b])]
    (->> pairs
         (map (fn [[a b]]
                (let [result (classify-pair (:text a) (:text b))]
                  (when (>= (:confidence result) min-confidence)
                    {:redundancy-candidate/id (java.util.UUID/randomUUID)
                     :redundancy-candidate/source-a {:section/id (:id a)
                                                     :section/path-raw (:path-raw a)
                                                     :section/heading-path (:heading-path a)
                                                     :section/text (:text a)}
                     :redundancy-candidate/source-b {:section/id (:id b)
                                                     :section/path-raw (:path-raw b)
                                                     :section/heading-path (:heading-path b)
                                                     :section/text (:text b)}
                     :redundancy-candidate/relation (:relation result)
                     :redundancy-candidate/confidence (:confidence result)
                     :redundancy-candidate/rationale (:rationale result)
                     :redundancy-candidate/signals (:signals result)
                     :redundancy-candidate/generator-version "redundancy-deterministic-v1"
                     :redundancy-candidate/status :provisional}))))
         (remove nil?)
         (sort-by :redundancy-candidate/confidence >)
         (take max-pairs)
         vec)))
