(ns epiphany.domain.file-lineage
  "Generate file-level lineage candidates from Git diff entries.
   Translates raw git-diff observations into provisional lineage candidates."
  (:require [epiphany.domain.lineage :as lineage]))

(def file-lineage-version "file-lineage-git-diff-v1")

(defn- classify-file-relation
  "Classify the file-level relation from a diff change type."
  [change-type]
  (case change-type
    :add    #{:possibly-derived-from}
    :modify #{:continues}
    :delete #{}
    :rename #{:possibly-derived-from}
    :copy   #{:possibly-derived-from}
    #{}))

(defn- confidence-for-change
  "Compute confidence for a file-level relation.
   Renames/copies with identical blob OIDs are exact observations."
  [change-type old-blob-oid new-blob-oid]
  (case change-type
    :add    0.5
    :modify (if (= old-blob-oid new-blob-oid) 0.0 0.6)
    :rename (if (= old-blob-oid new-blob-oid) 0.95 0.7)
    :copy   (if (= old-blob-oid new-blob-oid) 0.95 0.7)
    :delete 0.0
    0.5))

(defn- make-evidence-spans
  "Build evidence spans for a file-level candidate."
  [change-type old-blob-oid new-blob-oid]
  (let [exact-blob? (= old-blob-oid new-blob-oid)]
    (cond-> [{:evidence/signal :git-diff-change
              :evidence/value  1.0
              :evidence/weight 0.5
              :evidence/detail {:change-type change-type}}]
      exact-blob?
      (conj {:evidence/signal :exact-blob-match
             :evidence/value  1.0
             :evidence/weight 0.5
             :evidence/detail {:blob-oid old-blob-oid}}))))

(defn diff-entry->candidate
  "Convert a single diff entry into a file-level lineage candidate.
   Returns nil for deletes (no target to link to).

   source-commit and target-commit are the commit OID strings being compared."
  [diff-entry source-commit target-commit]
  (when-not (= (:diff/change-type diff-entry) :delete)
    (let [change-type (:diff/change-type diff-entry)
          old-path    (:diff/old-path diff-entry)
          new-path    (:diff/new-path diff-entry)
          old-blob    (:diff/old-blob-oid diff-entry)
          new-blob    (:diff/new-blob-oid diff-entry)
          relations   (classify-file-relation change-type)
          confidence  (confidence-for-change change-type old-blob new-blob)]
      (when (seq relations)
        (for [relation relations]
          {:lineage-candidate/id             (random-uuid)
           :lineage-candidate/source         {:section/path-raw  (or old-path new-path)
                                              :section/heading-path []
                                              :section/commit-oid source-commit}
           :lineage-candidate/target         {:section/path-raw  (or new-path old-path)
                                              :section/heading-path []
                                              :section/commit-oid target-commit}
           :lineage-candidate/relation       relation
           :lineage-candidate/confidence     confidence
           :lineage-candidate/evidence-spans (make-evidence-spans change-type old-blob new-blob)
           :lineage-candidate/features       {:git/change-type    change-type
                                              :git/same-blob?     (= old-blob new-blob)
                                              :git/rename?        (contains? #{:rename :copy} change-type)}
           :lineage-candidate/generator-version file-lineage-version
           :lineage-candidate/status         :provisional})))))

(defn diff->file-candidates
  "Convert a sequence of diff entries into file-level lineage candidates.
   Returns a flat seq of candidate maps (deletes are skipped)."
  [diff-entries source-commit target-commit]
  (->> diff-entries
       (mapcat #(diff-entry->candidate % source-commit target-commit))
       (remove nil?)
       vec))

(defn generate-file-lineage
  "Generate file-level lineage candidates from a Git diff result.
   diff-result is the output of git/diff-commits.
   Returns a vector of candidate maps."
  [diff-result]
  (when-not (:failure diff-result)
    (diff->file-candidates
     (:entries diff-result)
     (:old-commit-oid diff-result)
     (:new-commit-oid diff-result))))
