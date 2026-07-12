(ns epiphany.domain.revision-at-path
  "Pure domain: construct immutable file-at-commit observations that
  link selected tree entries to their Git evidence.

  An observation records one path at one commit, with evidence type
  (add/modify/delete/continuity/initial) determined by comparing the
  commit's tree entries against its parent's. No I/O is performed;
  tree entry maps arrive as plain data.

  The observation carries repository family/instance context
  (:resource-id), exact Git OIDs (commit, tree, blob), the exact
  path string, file mode, parent comparison context where available,
  observed time, and schema version.")

(defn- parent-entry-map
  "Index parent tree entries by path for O(1) lookup."
  [parent-entries]
  (into {} (map (juxt :git/path identity) parent-entries)))

(defn evidence-type
  "Determine the evidence type for a path in a commit's tree by
  comparing against the parent's tree entries.

  Returns one of:
    :initial     — no parent (root commit)
    :add         — path present in child, absent in parent
    :modify      — path present in both, different blob OID
    :continuity  — path present in both, same blob OID
    :delete      — path present in parent, absent in child

  For :delete, the entry is from the parent tree. For all others,
  the entry is from the child tree."
  ([commit-entry]
   (evidence-type commit-entry nil))
  ([commit-entry parent-entries]
   (if (nil? parent-entries)
     :initial
     (let [parent-map (parent-entry-map parent-entries)
           path (:git/path commit-entry)
           parent-entry (get parent-map path)]
       (cond
         (nil? parent-entry)       :add
         (= (:git/blob-oid commit-entry)
            (:git/blob-oid parent-entry)) :continuity
         :else                     :modify)))))

(defn evidence-for-deleted
  "Determine evidence for a path that exists in the parent but not
  in the child. Always returns :delete."
  [_parent-entry _child-entries]
  :delete)

(defn revision-at-path
  "Construct a revision-at-path observation map from a single entry.

  `entry` is a selection/entry map:
    {:entry/commit-oid  ...
     :entry/path-raw    ...
     :entry/blob-oid    ...
     :entry/mode        ...
     :entry/policy-version ...}

  `context` is a map with:
    :resource-id       — repository family/instance UUID
    :tree-oid          — commit's tree OID string
    :parent-commit-oid — parent commit OID (nil for root)
    :parent-blob-oid   — parent blob OID for same path (nil if absent)
    :observed-at       — inst? timestamp
    :parent-entries    — vector of parent tree entry maps (nil for root)"
  [entry context]
  (let [{:keys [resource-id tree-oid parent-commit-oid parent-blob-oid
                observed-at parent-entries]} context
        child-entry {:git/path     (:entry/path-raw entry)
                     :git/blob-oid (:entry/blob-oid entry)
                     :git/mode     (:entry/mode entry)}
        ev (if (and parent-entries (nil? parent-commit-oid))
             :initial
             (evidence-type child-entry parent-entries))]
    (cond-> {:observation/id            (random-uuid)
             :revision-at-path/id      (random-uuid)
             :observation/type         :revision/at-path-observed
             :observation/observed-at  observed-at
             :observation/adapter-version "revision-at-path-direct-0.1.0"
             :observation/schema-version 1
             :resource-id              resource-id
             :revision/commit-oid      (:entry/commit-oid entry)
             :revision/tree-oid        tree-oid
             :revision/path-raw        (:entry/path-raw entry)
             :revision/blob-oid        (:entry/blob-oid entry)
             :revision/mode            (:entry/mode entry)
             :revision/evidence        ev}
      (some? parent-commit-oid)
      (assoc :revision/parent-commit-oid parent-commit-oid)
      (some? parent-blob-oid)
      (assoc :revision/parent-blob-oid parent-blob-oid))))

(defn observation-id-key
  "Return the identity key for idempotency: the combination of
  resource-id, commit-oid, and path-raw that uniquely identifies
  one revision-at-path observation."
  [observation]
  {:resource-id      (:resource-id observation)
   :commit-oid       (:revision/commit-oid observation)
   :path-raw         (:revision/path-raw observation)})

(defn deduplicate
  "Remove duplicate observations by identity key, keeping the first.
  Idempotent reruns produce the same identity keys; this function
  collapses them."
  [observations]
  (let [seen (volatile! #{})]
    (filterv (fn [obs]
               (let [k (observation-id-key obs)]
                 (if (@seen k)
                   false
                   (do (vswap! seen conj k) true))))
             observations)))

(defn revisions-for-commit
  "Construct revision-at-path observations for all selected entries
  in a single commit, compared against its parent tree.

  `entries` — vector of selection/entry maps for this commit
  `context` — map with :resource-id, :tree-oid, :parent-commit-oid,
              :parent-entries, :observed-at (as in `revision-at-path`)

  Returns a vector of observation maps."
  [entries context]
  (mapv #(revision-at-path % context) entries))
