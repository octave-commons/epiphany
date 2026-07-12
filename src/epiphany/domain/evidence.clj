(ns epiphany.domain.evidence
  "Pure application service for retrieving exact historical evidence.

  Given a section expression (path + heading + commit), retrieves the
  exact source span from the Git blob with full provenance. No I/O —
  all Git access is injected via the :git port.

  The evidence reader never fabricates content. If a Git object is
  inaccessible, it reports UNAVAILABLE rather than returning partial
  or invented text."
  (:require [clojure.string :as str]))

;; ---------------------------------------------------------------------------
;; Section expression parsing

(defn parse-section-expression
  "Parse a section expression string into a structured map.

   Format: path[:heading-path][@commit-oid]
   Examples:
     docs/notes/foo.md
     docs/notes/foo.md#Architecture
     docs/notes/foo.md#Architecture>Design@abc123

   Returns {:path string, :heading vector, :commit-oid string or nil}"
  [expr]
  (let [[path-with-commit rest-expr] (str/split expr #"#" 2)
        ;; Handle @commit in path when no # is present
        [actual-path commit-from-path] (if (.contains path-with-commit "@")
                                          (let [[p c] (str/split path-with-commit #"@" 2)]
                                            [p c])
                                          [path-with-commit nil])
        ;; rest-expr may be "heading" or "heading@commit"
        [heading-str commit-from-heading] (cond
                                            (nil? rest-expr) [nil nil]
                                            (.contains rest-expr "@")
                                            (let [[h c] (str/split rest-expr #"@" 2)]
                                              [h c])
                                            :else [rest-expr nil])
        heading (when (and heading-str (not (str/blank? heading-str)))
                  (str/split heading-str #">"))
        commit-oid (or (when (and commit-from-path (not (str/blank? commit-from-path)))
                         commit-from-path)
                       (when (and commit-from-heading (not (str/blank? commit-from-heading)))
                         commit-from-heading))]
    {:path actual-path
     :heading (vec (or heading []))
     :commit-oid commit-oid}))

;; ---------------------------------------------------------------------------
;; Evidence retrieval

(defn- find-section-end
  "Find the end line (exclusive) for a section starting at line i."
  [lines i target-level]
  (loop [j (inc i)]
    (if (>= j (count lines))
      (count lines)
      (let [l (get lines j)
            m (re-matches #"^(#{1,6})\s+(.*)" l)]
        (if (and m (<= (count (first m)) target-level))
          j
          (recur (inc j)))))))

(defn- find-section-in-content
  "Find a section by heading path in Markdown content.

   Returns {:start-line int, :end-line int, :content string} or nil.

   The search is exact: heading text must match (case-insensitive).
   Nested headings (>) specify the full hierarchy."
  [content heading-path]
  (when (seq heading-path)
    (let [lines (str/split-lines content)
          target-level (count heading-path)
          target-text (str/lower-case (last heading-path))]
      (loop [i 0]
        (when (< i (count lines))
          (let [line (get lines i)]
            (if-let [[_ hashes text] (re-matches #"^(#{1,6})\s+(.*)" line)]
              (let [level (count hashes)
                    text-lower (str/lower-case (str/trim text))]
                (if (and (= level target-level)
                         (= text-lower target-text))
                  ;; Found the target heading
                  (let [end (find-section-end lines i target-level)]
                    {:start-line (inc i)  ; 1-indexed
                     :end-line end
                     :content (str/join "\n" (subvec lines i end))})
                  ;; Wrong heading — keep searching
                  (recur (inc i))))
              ;; Not a heading — continue
              (recur (inc i)))))))))

(defn retrieve-evidence
  "Retrieve exact historical evidence for a section expression.

   Parameters:
     ports   — application port map (must include :git with :read-blob)
     request — {:path string, :heading vector, :commit-oid string or nil}

   Returns a map with:
     :evidence/path           — file path
     :evidence/commit-oid     — commit OID
     :evidence/heading-path   — heading hierarchy
     :evidence/source         — exact source text (string)
     :evidence/start-line     — 1-indexed start line
     :evidence/end-line       — 1-indexed end line (exclusive)
     :evidence/commit-info    — commit metadata map (if available)
     :evidence/parent-oids    — parent commit OIDs
     :evidence/failure        — non-nil if retrieval failed
     :evidence/unavailable    — true if Git object inaccessible"
  [ports {:keys [path heading commit-oid]}]
  (let [git-fn (get-in ports [:git :read-blob])
        tree-fn (get-in ports [:git :commit-tree-entries])
        commit-fn (get-in ports [:git :reachable-commits])]
    (if-not git-fn
      {:evidence/path path
       :evidence/heading-path heading
       :evidence/failure {:failure/reason "port-missing"
                          :failure/message "Git read-blob port not available"}
       :evidence/unavailable true}
      (if-not commit-oid
        {:evidence/path path
         :evidence/heading-path heading
         :evidence/failure {:failure/reason "commit-required"
                            :failure/message "Commit OID required for historical evidence"}
         :evidence/unavailable true}
        (try
          ;; Get tree entries to find the blob OID for this path
          (let [tree-result (when tree-fn
                              (tree-fn nil commit-oid))
                entries (:entries tree-result)
                entry (first (filter #(= path (:git/path %)) entries))
                blob-oid (:git/blob-oid entry)]
            (if-not blob-oid
              {:evidence/path path
               :evidence/commit-oid commit-oid
               :evidence/heading-path heading
               :evidence/failure {:failure/reason "path-not-found"
                                  :failure/message (str "Path not found in commit " commit-oid ": " path)}
               :evidence/unavailable true}
              ;; Read the blob
              (let [blob (git-fn nil blob-oid)]
                (if (:blob/failure blob)
                  {:evidence/path path
                   :evidence/commit-oid commit-oid
                   :evidence/heading-path heading
                   :evidence/failure (:blob/failure blob)
                   :evidence/unavailable true}
                  ;; Find the section in content
                  (let [content (:blob/content blob)
                        section (when (seq heading)
                                   (find-section-in-content content heading))]
                    (if section
                      {:evidence/path path
                       :evidence/commit-oid commit-oid
                       :evidence/heading-path heading
                       :evidence/source (:content section)
                       :evidence/start-line (:start-line section)
                       :evidence/end-line (:end-line section)
                       :evidence/blob-size (:blob/size blob)
                       :evidence/failure nil
                       :evidence/unavailable false}
                      ;; Heading not found — return full content
                      {:evidence/path path
                       :evidence/commit-oid commit-oid
                       :evidence/heading-path heading
                       :evidence/source content
                       :evidence/start-line 1
                       :evidence/end-line (inc (count (str/split-lines content)))
                       :evidence/blob-size (:blob/size blob)
                       :evidence/failure {:failure/reason "heading-not-found"
                                          :failure/message (str "Heading not found: " (str/join " > " heading))}
                       :evidence/unavailable false}))))))
          (catch Exception e
            {:evidence/path path
             :evidence/commit-oid commit-oid
             :evidence/heading-path heading
             :evidence/failure {:failure/reason "retrieval-error"
                                :failure/message (.getMessage e)}
             :evidence/unavailable true}))))))

;; ---------------------------------------------------------------------------
;; Formatting

(defn format-evidence-text
  "Format evidence as human-readable text with provenance."
  [evidence]
  (if (:evidence/failure evidence)
    (str "UNAVAILABLE: " (get-in evidence [:evidence/failure :failure/message]))
    (let [{:keys [evidence/path evidence/commit-oid evidence/heading-path
                  evidence/source evidence/start-line evidence/end-line
                  evidence/blob-size]} evidence]
      (str "--- Source: " path
           (when (seq heading-path)
             (str " # " (str/join " > " heading-path)))
           " @ " commit-oid " ---"
           "\nLines " start-line "-" (dec end-line)
           (when blob-size
             (str " (" blob-size " bytes)"))
           "\n"
           source))))

(defn format-evidence-edn
  "Format evidence as EDN."
  [evidence]
  (pr-str evidence))
