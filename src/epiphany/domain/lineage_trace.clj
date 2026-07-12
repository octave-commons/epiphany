(ns epiphany.domain.lineage-trace
  "Trace a lineage chronology from a section.

  Walk the dated chain of path history, relocations, accepted edges,
  and provisional candidates to produce an ordered chronology.

  Every edge carries a visible status: :observed, :accepted, :provisional,
  or :rejected. The user can filter to observed facts only, or include
  provisional candidates. Every node resolves to the evidence reader."
  (:require [clojure.string :as str]))

;; ---------------------------------------------------------------------------
;; Node and edge construction

(defn make-node
  "Create a chronology node from a section map."
  [section]
  {:node/section section
   :node/continuity-score nil
   :node/boundary-proposal nil})

(defn make-edge
  "Create a chronology edge between two node indices."
  [from-idx to-idx relation confidence status evidence-spans]
  {:edge/from from-idx
   :edge/to to-idx
   :edge/relation relation
   :edge/confidence confidence
   :edge/status status
   :edge/evidence-spans evidence-spans})

;; ---------------------------------------------------------------------------
;; Chronology building

(defn- chronological-order
  "Sort sections chronologically by timestamp, then by commit-oid for stability."
  [sections]
  (sort-by (fn [s]
             [(.getTime ^java.util.Date (or (:timestamp s) (java.util.Date. 0)))
              (:commit-oid s)])
           sections))

(defn- build-git-history-edges
  "Build observed edges from consecutive revisions of the same section.
   These are always :observed status since they come from Git history."
  [sorted-sections]
  (map-indexed
   (fn [idx section]
     (when (pos? idx)
       (make-edge (dec idx) idx :continues 1.0 :observed
                  [{:evidence/signal :git-history
                    :evidence/value (:commit-oid section)
                    :evidence/weight 1.0}])))
   sorted-sections))

(defn- index-candidates
  "Index lineage candidates by source and target for fast lookup."
  [candidates]
  (reduce (fn [acc candidate]
            (let [src-key (vector (get-in candidate [:lineage-candidate/source :section/path-raw])
                                (get-in candidate [:lineage-candidate/source :section/heading-path])
                                (get-in candidate [:lineage-candidate/source :section/commit-oid]))
                  tgt-key (vector (get-in candidate [:lineage-candidate/target :section/path-raw])
                                (get-in candidate [:lineage-candidate/target :section/heading-path])
                                (get-in candidate [:lineage-candidate/target :section/commit-oid]))]
              (-> acc
                  (update-in [:by-source src-key] (fnil conj []) candidate)
                  (update-in [:by-target tgt-key] (fnil conj []) candidate))))
          {:by-source {} :by-target {}}
          candidates))

(defn- section-key
  "Build a lookup key from a section. Handles both plain (:path-raw)
   and namespaced (:section/path-raw) key formats."
  [section]
  [(:path-raw section (:section/path-raw section))
   (:heading-path section (:section/heading-path section))
   (:commit-oid section (:section/commit-oid section))])

(defn- find-cross-edges
  "Find cross-file lineage edges between nodes using indexed candidates.
   Returns edges with :provisional or :accepted status."
  [nodes candidates-index decisions-by-candidate-id]
  (let [nodes-by-key (into {} (map-indexed (fn [i n] [(section-key (:node/section n)) i]) nodes))
        edges (atom [])]
    (doseq [[idx node] (map-indexed vector nodes)]
      (let [key (section-key (:node/section node))
            outgoing (get-in candidates-index [:by-source key] [])]
        (doseq [c outgoing]
          (let [tgt-key (vector (get-in c [:lineage-candidate/target :section/path-raw])
                                (get-in c [:lineage-candidate/target :section/heading-path])
                                (get-in c [:lineage-candidate/target :section/commit-oid]))
                tgt-idx (get nodes-by-key tgt-key)]
            (when tgt-idx
              (let [candidate-id (:lineage-candidate/id c)
                    decision (get decisions-by-candidate-id candidate-id)
                    status (or (:boundary-decision/decision decision)
                               (:lineage-candidate/status c))
                    edge (make-edge idx tgt-idx
                                    (:lineage-candidate/relation c)
                                    (:lineage-candidate/confidence c)
                                    status
                                    (:lineage-candidate/evidence-spans c))]
                (swap! edges conj edge)))))))
    @edges))

;; ---------------------------------------------------------------------------
;; Filtering

(defn filter-by-status
  "Filter edges by status. When observed-only?, only :observed edges remain.
   When provisional-rejected is :exclude, rejected edges are removed."
  ([edges]
   edges)
  ([edges observed-only?]
   (if observed-only?
     (filter #(= :observed (:edge/status %)) edges)
     edges))
  ([edges observed-only? provisional-rejected]
   (let [filtered (filter-by-status edges observed-only?)]
     (if (= :exclude provisional-rejected)
       (filter #(not= :rejected (:edge/status %)) filtered)
       filtered))))

;; ---------------------------------------------------------------------------
;; Main entry point

(defn trace-lineage
  "Trace a lineage chronology from a source section.

   Parameters:
     source   — map with :path-raw, :heading-path, :commit-oid, :body, :timestamp
     sections — seq of section maps (all revisions to include in the trace)
     candidates — seq of lineage candidate maps (from lineage.clj)
     options — map:
       :include-adjacent? boolean — include Git history edges (default true)
       :observed-only?   boolean — only observed facts (default false)
       :provisional-rejected keyword — :include or :exclude (default :include)

   Returns a chronology map:
     {:trace/source section
      :trace/nodes [{:node/section {...}
                     :node/continuity-score double-or-nil
                     :node/boundary-proposal map-or-nil}]
      :trace/edges [{:edge/from int
                     :edge/to int
                     :edge/relation keyword
                     :edge/confidence double
                     :edge/status keyword
                     :edge/evidence-spans [...]}]
      :trace/filter keyword}"
  ([source sections candidates]
   (trace-lineage source sections candidates {}))
  ([source sections candidates {:keys [include-adjacent?
                                       observed-only?
                                       provisional-rejected]
                                :or {include-adjacent? true
                                     observed-only? false
                                     provisional-rejected :include}}]
   (let [;; Build node list: source first, then all sections chronologically
         sorted (chronological-order
                 (vals (reduce (fn [acc s] (assoc acc (section-key s) s))
                               {}
                               (cons source sections))))
         nodes (mapv make-node sorted)

         ;; Build Git history edges
         git-edges (when include-adjacent?
                     (build-git-history-edges sorted))

         ;; Index candidates for cross-file lookup
         candidates-index (index-candidates candidates)

         ;; Find cross-file edges
         cross-edges (find-cross-edges nodes candidates-index {})

         ;; Combine all edges
         all-edges (vec (concat (remove nil? git-edges) cross-edges))

         ;; Apply filters
         filtered-edges (-> all-edges
                            (filter-by-status observed-only?)
                            (filter-by-status false provisional-rejected))]

     {:trace/source source
      :trace/nodes nodes
      :trace/edges filtered-edges
      :trace/filter (cond
                      observed-only? :observed-only
                      (= :exclude provisional-rejected) :no-rejected
                      :else :all)})))
