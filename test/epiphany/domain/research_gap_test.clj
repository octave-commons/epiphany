(ns epiphany.domain.research-gap-test
  (:require [clojure.test :refer [deftest is testing]]
            [epiphany.domain.research-gap :as rg]))

;; ---------------------------------------------------------------------------
;; detect-todo-markers tests

(deftest detect-todo-markers-finds-markers
  (testing "Sections with TODO markers are detected"
    (let [sections [{:id :a :text "This needs more research" :path-raw "a.md" :heading-path ["A"]}
                    {:id :b :text "Normal text" :path-raw "b.md" :heading-path ["B"]}
                    {:id :c :text "FIXME: unclear result" :path-raw "c.md" :heading-path ["C"]}]
          gaps (rg/detect-todo-markers sections)]
      (is (= 2 (count gaps)))
      (is (every? #(= :todo-marker (:gap/type %)) gaps))
      (doseq [g gaps]
        (is (= :create-research-question (:gap/suggested-action g)))
        (is (seq (:gap/evidence g)))
        (is (number? (:gap/confidence g)))))))

(deftest detect-todo-markers-empty-for-clean-text
  (testing "Clean text produces no gaps"
    (let [sections [{:id :a :text "Normal text" :path-raw "a.md" :heading-path ["A"]}
                    {:id :b :text "More normal text" :path-raw "b.md" :heading-path ["B"]}]
          gaps (rg/detect-todo-markers sections)]
      (is (empty? gaps)))))

(deftest detect-todo-markers-empty-input
  (testing "Empty input returns empty"
    (is (empty? (rg/detect-todo-markers [])))))

;; ---------------------------------------------------------------------------
;; detect-low-continuity-transitions tests

(deftest detect-low-continuity-transitions-finds-gaps
  (testing "Sections with low continuity are detected"
    (let [sections [{:id :a :text "A" :path-raw "a.md" :heading-path ["A"]}
                    {:id :b :text "B" :path-raw "b.md" :heading-path ["B"]}
                    {:id :c :text "C" :path-raw "c.md" :heading-path ["C"]}]
          scores {:a 0.5 :b 0.1 :c 0.8}
          gaps (rg/detect-low-continuity-transitions sections scores)]
      (is (= 1 (count gaps)))
      (is (= :low-continuity-transition (:gap/type (first gaps))))
      (is (= 0.9 (:gap/confidence (first gaps)))))))

(deftest detect-low-continuity-transitions-respects-threshold
  (testing "Threshold filters low-continuity gaps"
    (let [sections [{:id :a :text "A" :path-raw "a.md" :heading-path ["A"]}
                    {:id :b :text "B" :path-raw "b.md" :heading-path ["B"]}]
          scores {:a 0.5 :b 0.3}
          gaps (rg/detect-low-continuity-transitions sections scores :threshold 0.2)]
      (is (empty? gaps)))))

(deftest detect-low-continuity-transitions-empty-input
  (testing "Empty input returns empty"
    (is (empty? (rg/detect-low-continuity-transitions [] {})))))

;; ---------------------------------------------------------------------------
;; detect-contradictions tests

(deftest detect-contradictions-finds-unresolved
  (testing "Unresolved contradictions are detected"
    (let [decisions [{:review/relation :possible-contradiction
                      :review/confidence 0.8
                      :review/rationale "conflicting claims"
                      :review/evidence [{:evidence/section-id :x}]}
                     {:review/relation :accepted
                      :review/confidence 0.9
                      :review/rationale "good claim"
                      :review/evidence []}]
          gaps (rg/detect-contradictions decisions)]
      (is (= 1 (count gaps)))
      (is (= :unresolved-contradiction (:gap/type (first gaps))))
      (is (= :resolve-contradiction (:gap/suggested-action (first gaps)))))))

(deftest detect-contradictions-empty-for-no-contradictions
  (testing "No contradictions produces no gaps"
    (let [decisions [{:review/relation :accepted :review/confidence 0.9}]
          gaps (rg/detect-contradictions decisions)]
      (is (empty? gaps)))))

(deftest detect-contradictions-empty-input
  (testing "Empty input returns empty"
    (is (empty? (rg/detect-contradictions [])))))

;; ---------------------------------------------------------------------------
;; detect-isolated-claims tests

(deftest detect-isolated-claims-finds-isolated
  (testing "Late claims with few connections are detected"
    (let [sections [{:id :a :text "A" :path-raw "a.md" :heading-path ["A"] :position 0}
                    {:id :b :text "B" :path-raw "b.md" :heading-path ["B"] :position 1}
                    {:id :c :text "C" :path-raw "c.md" :heading-path ["C"] :position 2}]
          lineage [{:lineage/source :a :lineage/target :b}]
          gaps (rg/detect-isolated-claims sections lineage :min-position 0.5)]
      ;; :c is at position 2/3 = 0.66, with 0 connections
      (is (some #(= :isolated-claim (:gap/type %)) gaps)))))

(deftest detect-isolated-claims-empty-for-connected
  (testing "Connected claims produce no gaps"
    (let [sections [{:id :a :text "A" :path-raw "a.md" :heading-path ["A"] :position 0}
                    {:id :b :text "B" :path-raw "b.md" :heading-path ["B"] :position 1}]
          lineage [{:lineage/source :a :lineage/target :b}]
          gaps (rg/detect-isolated-claims sections lineage)]
      (is (empty? gaps)))))

(deftest detect-isolated-claims-empty-input
  (testing "Empty input returns empty"
    (is (empty? (rg/detect-isolated-claims [] [])))))

;; ---------------------------------------------------------------------------
;; detect-near-duplicates tests

(deftest detect-near-duplicates-finds-duplicates
  (testing "Near-duplicates are detected"
    (let [candidates [{:redundancy-candidate/relation :near-duplicate
                       :redundancy-candidate/confidence 0.9
                       :redundancy-candidate/source-a {:section/id :a :section/path-raw "a.md"
                                                       :section/heading-path ["A"] :section/text "a"}
                       :redundancy-candidate/source-b {:section/id :b :section/path-raw "b.md"
                                                       :section/heading-path ["B"] :section/text "b"}}
                      {:redundancy-candidate/relation :duplicate
                       :redundancy-candidate/confidence 1.0}]
          gaps (rg/detect-near-duplicates candidates)]
      (is (= 1 (count gaps)))
      (is (= :repeated-near-duplicate (:gap/type (first gaps))))
      (is (= 2 (count (:gap/evidence (first gaps))))))))

(deftest detect-near-duplicates-empty-for-no-near-dupes
  (testing "No near-duplicates produces no gaps"
    (let [candidates [{:redundancy-candidate/relation :duplicate}]
          gaps (rg/detect-near-duplicates candidates)]
      (is (empty? gaps)))))

(deftest detect-near-duplicates-empty-input
  (testing "Empty input returns empty"
    (is (empty? (rg/detect-near-duplicates [])))))

;; ---------------------------------------------------------------------------
;; surface-gaps tests

(deftest surface-gaps-aggregates-all
  (testing "surface-gaps aggregates all gap types"
    (let [data {:sections [{:id :a :text "TODO: investigate" :path-raw "a.md" :heading-path ["A"]}
                           {:id :b :text "Normal" :path-raw "b.md" :heading-path ["B"]}]
                :continuity-scores {:a 0.9 :b 0.1}
                :decisions [{:review/relation :possible-contradiction
                             :review/confidence 0.8
                             :review/evidence []}]
                :lineage-links []
                :redundancy-candidates [{:redundancy-candidate/relation :near-duplicate
                                         :redundancy-candidate/confidence 0.85
                                         :redundancy-candidate/source-a {:section/id :x :section/path-raw "x.md"
                                                                         :section/heading-path ["X"] :section/text "x"}
                                         :redundancy-candidate/source-b {:section/id :y :section/path-raw "y.md"
                                                                         :section/heading-path ["Y"] :section/text "y"}}]}
          gaps (rg/surface-gaps data)]
      (is (seq gaps))
      (is (vector? gaps))
      (is (apply >= (map :gap/confidence gaps)))
      (is (some #(= :todo-marker (:gap/type %)) gaps))
      (is (some #(= :low-continuity-transition (:gap/type %)) gaps))
      (is (some #(= :unresolved-contradiction (:gap/type %)) gaps))
      (is (some #(= :repeated-near-duplicate (:gap/type %)) gaps)))))

(deftest surface-gaps-empty-data
  (testing "surface-gaps with empty data returns empty"
    (is (empty? (rg/surface-gaps {})))))

(deftest surface-gaps-sorted-by-confidence
  (testing "Results sorted by confidence descending"
    (let [data {:sections [{:id :a :text "TODO: investigate" :path-raw "a.md" :heading-path ["A"]}
                           {:id :b :text "FIXME: unclear" :path-raw "b.md" :heading-path ["B"]}
                           {:id :c :text "TODO: more research needed" :path-raw "c.md" :heading-path ["C"]}
                           {:id :d :text "Hack: workaround" :path-raw "d.md" :heading-path ["D"]}
                           {:id :e :text "???" :path-raw "e.md" :heading-path ["E"]}
                           {:id :f :text "normal" :path-raw "f.md" :heading-path ["F"]}
                           {:id :g :text "also normal" :path-raw "g.md" :heading-path ["G"]}
                           {:id :h :text "research question" :path-raw "h.md" :heading-path ["H"]}
                           {:id :i :text "needs investigation" :path-raw "i.md" :heading-path ["I"]}
                           {:id :j :text "unclear" :path-raw "j.md" :heading-path ["J"]}
                           {:id :k :text "unresolved" :path-raw "k.md" :heading-path ["K"]}
                           {:id :l :text "needs more research" :path-raw "l.md" :heading-path ["L"]}
                           {:id :m :text "TODO: finish" :path-raw "m.md" :heading-path ["M"]}
                           {:id :n :text "FIXME: broken" :path-raw "n.md" :heading-path ["N"]}
                           {:id :o :text "research question again" :path-raw "o.md" :heading-path ["O"]}
                           {:id :p :text "needs further research" :path-raw "p.md" :heading-path ["P"]}
                           {:id :q :text "open question" :path-raw "q.md" :heading-path ["Q"]}
                           {:id :r :text "needs more investigation" :path-raw "r.md" :heading-path ["R"]}
                           {:id :s :text "HACK: temporary" :path-raw "s.md" :heading-path ["S"]}
                           {:id :t :text "TODO: cleanup" :path-raw "t.md" :heading-path ["T"]}
                           {:id :u :text "FIXME: edge case" :path-raw "u.md" :heading-path ["U"]}
                           {:id :v :text "needs further investigation" :path-raw "v.md" :heading-path ["V"]}
                           {:id :w :text "open question" :path-raw "w.md" :heading-path ["W"]}
                           {:id :x :text "research question" :path-raw "x.md" :heading-path ["X"]}
                           {:id :y :text "needs more research" :path-raw "y.md" :heading-path ["Y"]}
                           {:id :z :text "TODO: fix" :path-raw "z.md" :heading-path ["Z"]}
                           {:id :aa :text "FIXME: bug" :path-raw "aa.md" :heading-path ["AA"]}
                           {:id :ab :text "needs investigation" :path-raw "ab.md" :heading-path ["AB"]}
                           {:id :ac :text "research question" :path-raw "ac.md" :heading-path ["AC"]}
                           {:id :ad :text "FIXME: issue" :path-raw "ad.md" :heading-path ["AD"]}
                           {:id :ae :text "TODO: verify" :path-raw "ae.md" :heading-path ["AE"]}
                           {:id :af :text "FIXME: edge" :path-raw "af.md" :heading-path ["AF"]}
                           {:id :ag :text "TODO: review" :path-raw "ag.md" :heading-path ["AG"]}
                           {:id :ah :text "FIXME: check" :path-raw "ah.md" :heading-path ["AH"]}
                           {:id :ai :text "TODO: verify" :path-raw "ai.md" :heading-path ["AI"]}
                           {:id :aj :text "FIXME: fix" :path-raw "aj.md" :heading-path ["AJ"]}
                           {:id :ak :text "TODO: check" :path-raw "ak.md" :heading-path ["AK"]}
                           {:id :al :text "FIXME: review" :path-raw "al.md" :heading-path ["AL"]}
                           {:id :am :text "TODO: fix" :path-raw "am.md" :heading-path ["AM"]}
                           {:id :an :text "FIXME: verify" :path-raw "an.md" :heading-path ["AN"]}
                           {:id :ao :text "TODO: check" :path-raw "ao.md" :heading-path ["AO"]}
                           {:id :ap :text "FIXME: review" :path-raw "ap.md" :heading-path ["AP"]}
                           {:id :aq :text "TODO: fix" :path-raw "aq.md" :heading-path ["AQ"]}
                           {:id :ar :text "FIXME: verify" :path-raw "ar.md" :heading-path ["AR"]}
                           {:id :as :text "TODO: check" :path-raw "as.md" :heading-path ["AS"]}
                           {:id :at :text "FIXME: review" :path-raw "at.md" :heading-path ["AT"]}
                           {:id :au :text "TODO: fix" :path-raw "au.md" :heading-path ["AU"]}
                           {:id :av :text "FIXME: verify" :path-raw "av.md" :heading-path ["AV"]}
                           {:id :aw :text "TODO: check" :path-raw "aw.md" :heading-path ["AW"]}
                           {:id :ax :text "FIXME: review" :path-raw "ax.md" :heading-path ["AX"]}
                           {:id :ay :text "TODO: fix" :path-raw "ay.md" :heading-path ["AY"]}
                           {:id :az :text "FIXME: verify" :path-raw "az.md" :heading-path ["AZ"]}]}
          gaps (rg/surface-gaps data)]
      (is (apply >= (map :gap/confidence gaps))))))

;; ---------------------------------------------------------------------------
;; gap->research-question tests

(deftest gap-to-research-question
  (testing "gap->research-question converts gap to RQ"
    (let [gap {:gap/type :unresolved-contradiction
               :gap/description "Contradiction found"
               :gap/confidence 0.8
               :gap/evidence [{:evidence/section-id :x :evidence/path-raw "x.md"
                               :evidence/heading-path ["X"] :evidence/text "text"}]
               :gap/suggested-action :resolve-contradiction}
          rq (rg/gap->research-question gap)]
      (is (some? (:research-question/id rq)))
      (is (= "Contradiction found" (:research-question/question rq)))
      (is (= :high (:research-question/priority rq)))
      (is (= :open (:research-question/status rq)))
      (is (= "research-gap-detector" (:research-question/created-by rq)))
      (is (= 1 (count (:research-question/evidence-links rq)))))))

(deftest gap-to-research-question-preserves-evidence
  (testing "Evidence links are preserved in conversion"
    (let [gap {:gap/type :todo-marker
               :gap/description "TODO found"
               :gap/evidence [{:evidence/section-id :a :evidence/path-raw "a.md"
                               :evidence/heading-path ["A"] :evidence/text "hello"}
                              {:evidence/section-id :b :evidence/path-raw "b.md"
                               :evidence/heading-path ["B"] :evidence/text "world"}]}
          rq (rg/gap->research-question gap)]
      (is (= 2 (count (:research-question/evidence-links rq))))
      (is (= :medium (:research-question/priority rq))))))

(deftest gap-types-are-valid
  (testing "All gap types are defined"
    (is (= #{:unresolved-contradiction :todo-marker :low-continuity-transition
             :isolated-claim :repeated-near-duplicate}
           rg/gap-types))))
