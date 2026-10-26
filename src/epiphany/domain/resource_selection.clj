(ns epiphany.domain.resource-selection
  "Pure scope decisions shared by revision, extraction and embedding stages."
  (:require [epiphany.domain.markdown-selection :as markdown]))

(defn includes-path?
  "A missing policy retains legacy Markdown selection; explicit paths are literal."
  [policy path]
  (if policy
    (contains? (set (:selection/paths policy)) path)
    (markdown/includes-path? markdown/default-policy path)))

(defn select-entries
  "Select regular Git files only, preserving exact paths, OIDs and file modes."
  [commit policy entries]
  (into [] (comp (filter #(= :file (:git/type %)))
                 (filter #(includes-path? policy (:git/path %)))
                 (map (fn [entry]
                        {:entry/commit-oid commit :entry/path-raw (:git/path entry)
                         :entry/blob-oid (:git/blob-oid entry) :entry/mode (:git/mode entry)
                         :entry/policy-version "exact-resource-paths-v1"}))) entries))

(defn filter-revisions
  "A narrowed run never rehydrates old out-of-scope revisions from a reused store."
  [revisions policy selected-commit-oids]
  (filterv #(and (includes-path? policy (:revision/path-raw %))
                 (or (nil? selected-commit-oids)
                     (contains? selected-commit-oids (:revision/commit-oid %)))) revisions))

(defn assert-retry!
  "Refuse changed scope under an already accepted ingestion request."
  [previous policy refs repository-path resolved-commit-oids]
  (when (and previous
             (not= [(:ingestion/selection previous) (:ingestion/selected-refs previous)
                    (get-in previous [:ingestion/repo-path :path/raw])
                    (when policy (:ingestion/resolved-commit-oids previous))]
                   [policy refs repository-path (when policy (vec (sort resolved-commit-oids)))]))
    (throw (ex-info "Ingestion request ID already has a different repository, refs or selection"
                    {:code :idempotency-conflict}))))
