(ns epiphany.domain.lineage-trace-test
  (:require [clojure.test :refer [deftest testing is are]]
            [epiphany.domain.lineage-trace :as lt]))

;; ---------------------------------------------------------------------------
;; Test fixtures

(defn- make-section
  "Build a minimal section map for testing."
  [path heading commit-oid & {:keys [body timestamp]
                               :or {body "" timestamp nil}}]
  {:path-raw path
   :heading-path heading
   :commit-oid commit-oid
   :body body
   :timestamp timestamp})

(defn- make-candidate
  "Build a minimal lineage candidate."
  [src-path src-heading src-commit tgt-path tgt-heading tgt-commit relation]
  {:lineage-candidate/id (java.util.UUID/randomUUID)
   :lineage-candidate/source {:section/path-raw src-path
                              :section/heading-path src-heading
                              :section/commit-oid src-commit}
   :lineage-candidate/target {:section/path-raw tgt-path
                              :section/heading-path tgt-heading
                              :section/commit-oid tgt-commit}
   :lineage-candidate/relation relation
   :lineage-candidate/confidence 0.7
   :lineage-candidate/evidence-spans [{:evidence/signal :text-similarity
                                       :evidence/value 0.6
                                       :evidence/weight 0.4}]
   :lineage-candidate/status :provisional})

;; ---------------------------------------------------------------------------
;; make-node

(deftest make-node-test
  (testing "creates a node from a section"
    (let [section (make-section "a.md" ["Intro"] "abc")
          node (lt/make-node section)]
      (is (= section (:node/section node)))
      (is (nil? (:node/continuity-score node)))
      (is (nil? (:node/boundary-proposal node))))))

;; ---------------------------------------------------------------------------
;; make-edge

(deftest make-edge-test
  (testing "creates an edge with all fields"
    (let [edge (lt/make-edge 0 1 :continues 0.8 :observed
                             [{:evidence/signal :git-history}])]
      (is (= 0 (:edge/from edge)))
      (is (= 1 (:edge/to edge)))
      (is (= :continues (:edge/relation edge)))
      (is (= 0.8 (:edge/confidence edge)))
      (is (= :observed (:edge/status edge)))
      (is (= 1 (count (:edge/evidence-spans edge)))))))

;; ---------------------------------------------------------------------------
;; filter-by-status

(deftest filter-by-status-all-test
  (testing "no filter returns all edges"
    (let [edges [{:edge/status :observed}
                 {:edge/status :provisional}
                 {:edge/status :rejected}]]
      (is (= 3 (count (lt/filter-by-status edges)))))))

(deftest filter-by-status-observed-only-test
  (testing "observed-only returns only observed edges"
    (let [edges [{:edge/status :observed}
                 {:edge/status :provisional}
                 {:edge/status :observed}]]
      (is (= 2 (count (lt/filter-by-status edges true)))))))

(deftest filter-by-status-exclude-rejected-test
  (testing "exclude-rejected removes rejected edges"
    (let [edges [{:edge/status :observed}
                 {:edge/status :rejected}
                 {:edge/status :provisional}]]
      (is (= 2 (count (lt/filter-by-status edges false :exclude)))))))

(deftest filter-by-status-observed-and-exclude-rejected-test
  (testing "observed-only + exclude-rejected"
    (let [edges [{:edge/status :observed}
                 {:edge/status :rejected}
                 {:edge/status :provisional}
                 {:edge/status :observed}]]
      (is (= 2 (count (lt/filter-by-status edges true :exclude)))))))

;; ---------------------------------------------------------------------------
;; trace-lineage

(deftest trace-lineage-basic-test
  (testing "traces Git history edges between revisions"
    (let [src (make-section "a.md" ["Intro"] "abc"
                            :timestamp (java.util.Date. 1000))
          tgt (make-section "a.md" ["Intro"] "def"
                            :timestamp (java.util.Date. 2000))
          result (lt/trace-lineage src [tgt] [])]
      (is (= src (:trace/source result)))
      (is (= 2 (count (:trace/nodes result))))
      (is (>= (count (:trace/edges result)) 1))
      ;; Git history edges are :observed
      (is (every? #(= :observed (:edge/status %))
                  (:trace/edges result))))))

(deftest trace-lineage-with-candidates-test
  (testing "includes lineage candidates as edges"
    (let [src (make-section "a.md" ["Intro"] "abc"
                            :timestamp (java.util.Date. 1000))
          tgt (make-section "a.md" ["Intro"] "def"
                            :timestamp (java.util.Date. 2000))
          candidate (make-candidate "a.md" ["Intro"] "abc"
                                    "a.md" ["Intro"] "def"
                                    :continues)
          result (lt/trace-lineage src [tgt] [candidate])]
      ;; Should have at least one :provisional edge from the candidate
      (is (some #(= :provisional (:edge/status %))
                (:trace/edges result))))))

(deftest trace-lineage-no-candidates-test
  (testing "works with empty candidates"
    (let [src (make-section "a.md" ["Intro"] "abc"
                            :timestamp (java.util.Date. 1000))
          result (lt/trace-lineage src [] [])]
      (is (= 1 (count (:trace/nodes result))))
      (is (= [] (:trace/edges result))))))

(deftest trace-lineage-observed-only-filter-test
  (testing "observed-only filter removes provisional edges"
    (let [src (make-section "a.md" ["Intro"] "abc"
                            :timestamp (java.util.Date. 1000))
          tgt (make-section "a.md" ["Intro"] "def"
                            :timestamp (java.util.Date. 2000))
          candidate (make-candidate "a.md" ["Intro"] "abc"
                                    "a.md" ["Intro"] "def"
                                    :continues)
          result (lt/trace-lineage src [tgt] [candidate]
                                   {:observed-only? true})]
      ;; Only :observed edges should remain
      (is (every? #(= :observed (:edge/status %))
                  (:trace/edges result)))
      (is (= :observed-only (:trace/filter result))))))

(deftest trace-lineage-exclude-rejected-filter-test
  (testing "exclude-rejected filter removes rejected edges"
    (let [src (make-section "a.md" ["Intro"] "abc"
                            :timestamp (java.util.Date. 1000))
          tgt (make-section "a.md" ["Intro"] "def"
                            :timestamp (java.util.Date. 2000))
          candidate (make-candidate "a.md" ["Intro"] "abc"
                                    "a.md" ["Intro"] "def"
                                    :continues)
          ;; Manually create a rejected candidate
          rejected (assoc candidate
                          :lineage-candidate/status :rejected)
          result (lt/trace-lineage src [tgt] [candidate rejected]
                                   {:provisional-rejected :exclude})]
      (is (not (some #(= :rejected (:edge/status %))
                     (:trace/edges result))))
      (is (= :no-rejected (:trace/filter result))))))

(deftest trace-lineage-no-adjacent-test
  (testing "include-adjacent? false removes Git history edges"
    (let [src (make-section "a.md" ["Intro"] "abc"
                            :timestamp (java.util.Date. 1000))
          tgt (make-section "a.md" ["Intro"] "def"
                            :timestamp (java.util.Date. 2000))
          result (lt/trace-lineage src [tgt] []
                                   {:include-adjacent? false})]
      ;; No edges when both adjacent and cross-file are disabled
      (is (= [] (:trace/edges result))))))

(deftest trace-lineage-nodes-sorted-chronologically-test
  (testing "nodes are sorted by timestamp"
    (let [src (make-section "a.md" ["Intro"] "abc"
                            :timestamp (java.util.Date. 3000))
          t1 (make-section "a.md" ["Intro"] "def"
                           :timestamp (java.util.Date. 1000))
          t2 (make-section "a.md" ["Intro"] "ghi"
                           :timestamp (java.util.Date. 2000))
          result (lt/trace-lineage src [t1 t2] [])]
      (is (= 3 (count (:trace/nodes result))))
      ;; Timestamps should be in order
      (let [timestamps (map #(get-in % [:node/section :timestamp])
                            (:trace/nodes result))]
        (is (= (sort timestamps) timestamps))))))

(deftest trace-lineage-every-edge-has-status-test
  (testing "every edge carries a visible status"
    (let [src (make-section "a.md" ["Intro"] "abc"
                            :timestamp (java.util.Date. 1000))
          tgt (make-section "a.md" ["Intro"] "def"
                            :timestamp (java.util.Date. 2000))
          candidate (make-candidate "a.md" ["Intro"] "abc"
                                    "a.md" ["Intro"] "def"
                                    :continues)
          result (lt/trace-lineage src [tgt] [candidate])]
      (is (every? :edge/status (:trace/edges result)))
      (is (every? #(contains? #{:observed :provisional :accepted :rejected}
                              (:edge/status %))
                  (:trace/edges result))))))
