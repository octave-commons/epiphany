(ns epiphany.domain.markdown-selection
  "Pure domain: select Markdown tree entries from an observed commit
  tree using an explicit versioned selection policy.

  Selection applies include/exclude globs to the exact Git path
  strings as provided by the tree walker. No paths are normalized,
  resolved, or checked out. Each selected entry preserves the commit
  OID, exact path string, blob OID, file mode, and policy version.

  The default policy (v1) selects `**/*.md` at any depth.")

(def default-policy
  "The initial documented selection policy. Versioned; changes are a
  new policy, not a mutation of this one."
  {:selection/policy-version "markdown-tree-v1"
   :selection/include-globs ["**/*.md"]
   :selection/exclude-globs []})

(defn- seg-match?
  "Match a single path segment against a single glob segment (no slashes).
  `*` matches one-or-more non-slash characters, `?` matches exactly one."
  [^String pattern ^String segment]
  (let [pat-chars (vec (.toCharArray pattern))
        seg-chars (vec (.toCharArray segment))]
    (letfn [(match [pi si]
              (cond
                (= pi (count pat-chars)) (= si (count seg-chars))
                (= si (count seg-chars))
                (and (= \* (get pat-chars pi))
                     (match (inc pi) si))
                (= \* (get pat-chars pi))
                (or (match (inc pi) si)
                    (match pi (inc si)))
                (= \? (get pat-chars pi))
                (and (< si (count seg-chars))
                     (match (inc pi) (inc si)))
                :else
                (and (< si (count seg-chars))
                     (= (get pat-chars pi) (get seg-chars si))
                     (match (inc pi) (inc si)))))]
      (match 0 0))))

(defn- glob-match?
  "Match a path against a glob pattern. Glob grammar:
    `**` — zero or more path segments (recursive)
    `*`  — one or more non-slash characters within a segment
    `?`  — exactly one non-slash character within a segment
  Patterns may start with `**/` to match from any depth."
  [pattern path]
  (let [pat-segs (vec (remove empty? (.split ^String pattern "/" -1)))
        path-segs (vec (remove empty? (.split ^String path "/" -1)))]
    (letfn [(match-segs [pi pp]
              (cond
                (= pp (count pat-segs)) (= pi (count path-segs))
                (= pi (count path-segs))
                (and (= "**" (get pat-segs pp))
                     (match-segs pi (inc pp)))
                (= "**" (get pat-segs pp))
                (or (match-segs pi (inc pp))
                    (match-segs (inc pi) pp))
                :else
                (and (< pi (count path-segs))
                     (seg-match? (get pat-segs pp) (get path-segs pi))
                     (match-segs (inc pi) (inc pp)))))]
      (match-segs 0 0))))

(defn- matches-any?
  "True when `path` matches any glob in `globs`."
  [path globs]
  (some #(glob-match? % path) globs))

(defn select-entries
  "Apply `policy` to a sequence of raw tree-entry maps and return
  the selected Markdown entries.

  Each `entry` in `tree-entries` is a map:
    {:git/path <exact Git path string>
     :git/blob-oid <SHA string>
     :git/mode <integer mode bits>
     :git/type :file | :tree | :symlink | :gitlink | :missing}

  Returns a vector of `selection/entry` maps, each containing:
    :entry/commit-oid      — the commit being inspected
    :entry/path-raw        — exact Git path string
    :entry/blob-oid        — blob object ID
    :entry/mode            — raw mode bits
    :entry/policy-version  — the policy version used

  Selection does not check out historical revisions."
  [commit-oid policy tree-entries]
  (let [{:keys [selection/include-globs selection/exclude-globs
                selection/policy-version]} policy]
    (->> tree-entries
         (filter #(= :file (:git/type %)))
         (filter #(matches-any? (:git/path %) include-globs))
         (remove #(matches-any? (:git/path %) exclude-globs))
         (mapv (fn [{:keys [git/path git/blob-oid git/mode]}]
                 {:entry/commit-oid     commit-oid
                  :entry/path-raw       path
                  :entry/blob-oid       blob-oid
                  :entry/mode           mode
                  :entry/policy-version policy-version})))))

(defn select-markdown
  "Convenience: apply the default policy to tree entries.
  Same inputs and outputs as `select-entries`."
  [commit-oid tree-entries]
  (select-entries commit-oid default-policy tree-entries))
