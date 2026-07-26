(ns epiphany.domain.export-test
  (:require [clojure.test :refer [deftest is testing]]
            [epiphany.domain.export :as export]))

;; ---------------------------------------------------------------------------
;; make-packet tests

(deftest make-packet-defaults
  (testing "make-packet creates empty packet with defaults"
    (let [pkt (export/make-packet)]
      (is (some? (:packet/id pkt)))
      (is (= "Evidence Packet" (:packet/label pkt)))
      (is (= "export-v1" (:packet/generator-version pkt)))
      (is (some? (:packet/created-at pkt)))
      (is (= [] (:packet/observed-facts pkt)))
      (is (= [] (:packet/inferred-candidates pkt)))
      (is (= [] (:packet/accepted-interpretations pkt)))
      (is (= [] (:packet/open-questions pkt))))))

(deftest make-packet-custom
  (testing "make-packet with custom options"
    (let [rid #uuid "00000000-0000-0000-0000-000000000001"
          pkt (export/make-packet :resource-id rid
                                  :label "Test Packet"
                                  :generator-version "test-v1")]
      (is (= rid (:packet/resource-id pkt)))
      (is (= "Test Packet" (:packet/label pkt)))
      (is (= "test-v1" (:packet/generator-version pkt))))))

;; ---------------------------------------------------------------------------
;; make-evidence-ref tests

(deftest make-evidence-ref-basic
  (testing "make-evidence-ref creates evidence reference"
    (let [src (export/make-evidence-ref {:section-id :s1
                                         :path-raw "doc.md"
                                         :heading-path ["H1"]
                                         :text "hello"})]
      (is (= :s1 (:evidence/section-id src)))
      (is (= "doc.md" (:evidence/path-raw src)))
      (is (= ["H1"] (:evidence/heading-path src)))
      (is (= "hello" (:evidence/text src))))))

(deftest make-evidence-ref-with-context
  (testing "make-evidence-ref with context"
    (let [src (export/make-evidence-ref {:section-id :s1
                                         :path-raw "doc.md"
                                         :heading-path []
                                         :text "world"}
                                        :context "test context")]
      (is (= "test context" (:evidence/context src))))))

;; ---------------------------------------------------------------------------
;; make-no-source tests

(deftest make-no-source-creates-label
  (testing "make-no-source creates explicit no-source label"
    (let [src (export/make-no-source "No evidence available")]
      (is (= :no-direct-source (:source/type src)))
      (is (= "No evidence available" (:source/reason src))))))

;; ---------------------------------------------------------------------------
;; make-interpretation-source tests

(deftest make-interpretation-source-creates-label
  (testing "make-interpretation-source creates interpretation label"
    (let [src (export/make-interpretation-source "User interpretation" "analyst")]
      (is (= :interpretation (:source/type src)))
      (is (= "User interpretation" (:source/interpretation src)))
      (is (= "analyst" (:source/interpreter src))))))

;; ---------------------------------------------------------------------------
;; make-claim tests

(deftest make-claim-basic
  (testing "make-claim creates claim map"
    (let [src (export/make-no-source "test")
          c (export/make-claim :observed "Statement" src)]
      (is (= :observed (:claim/type c)))
      (is (= "Statement" (:claim/statement c)))
      (is (= src (:claim/source c)))
      (is (= 0.0 (:claim/confidence c)))
      (is (= "" (:claim/rationale c))))))

(deftest make-claim-with-options
  (testing "make-claim with options"
    (let [src (export/make-no-source "test")
          c (export/make-claim :inferred "Inferred" src
                               :confidence 0.8
                               :rationale "because"
                               :identifiers {:id 1})]
      (is (= :inferred (:claim/type c)))
      (is (= 0.8 (:claim/confidence c)))
      (is (= "because" (:claim/rationale c)))
      (is (= {:id 1} (:claim/identifiers c))))))

;; ---------------------------------------------------------------------------
;; add claims tests

(deftest add-observed-fact-works
  (testing "add-observed-fact adds to packet"
    (let [pkt (export/make-packet)
          claim (export/make-claim :observed "fact" (export/make-no-source "test"))
          pkt2 (export/add-observed-fact pkt claim)]
      (is (= 1 (count (:packet/observed-facts pkt2)))))))

(deftest add-inferred-candidate-works
  (testing "add-inferred-candidate adds to packet"
    (let [pkt (export/make-packet)
          claim (export/make-claim :inferred "candidate" (export/make-no-source "test"))
          pkt2 (export/add-inferred-candidate pkt claim)]
      (is (= 1 (count (:packet/inferred-candidates pkt2)))))))

(deftest add-accepted-interpretation-works
  (testing "add-accepted-interpretation adds to packet"
    (let [pkt (export/make-packet)
          claim (export/make-claim :accepted "interpretation" (export/make-no-source "test"))
          pkt2 (export/add-accepted-interpretation pkt claim)]
      (is (= 1 (count (:packet/accepted-interpretations pkt2)))))))

(deftest add-open-question-works
  (testing "add-open-question adds to packet"
    (let [pkt (export/make-packet)
          claim (export/make-claim :question "question?" (export/make-no-source "test"))
          pkt2 (export/add-open-question pkt claim)]
      (is (= 1 (count (:packet/open-questions pkt2)))))))

;; ---------------------------------------------------------------------------
;; populate-from-lineage tests

(deftest populate-from-lineage-adds-candidates
  (testing "populate-from-lineage adds inferred candidates"
    (let [pkt (export/make-packet)
          candidates [{:lineage/relation :related
                       :lineage/confidence 0.7
                       :lineage/rationale "shared topic"
                       :lineage/source {:section/id :a :section/path-raw "a.md"
                                        :section/heading-path ["A"] :section/text "text a"}
                       :lineage/target {:section/id :b :section/path-raw "b.md"
                                        :section/heading-path ["B"] :section/text "text b"}}]
          pkt2 (export/populate-from-lineage pkt candidates)]
      (is (= 1 (count (:packet/inferred-candidates pkt2))))
      (is (= :inferred (:claim/type (first (:packet/inferred-candidates pkt2))))))))

(deftest populate-from-lineage-empty
  (testing "populate-from-lineage with empty candidates"
    (let [pkt (export/make-packet)
          pkt2 (export/populate-from-lineage pkt [])]
      (is (= [] (:packet/inferred-candidates pkt2))))))

;; ---------------------------------------------------------------------------
;; populate-from-redundancy tests

(deftest populate-from-redundancy-adds-candidates
  (testing "populate-from-redundancy adds inferred candidates"
    (let [pkt (export/make-packet)
          candidates [{:redundancy-candidate/relation :near-duplicate
                       :redundancy-candidate/confidence 0.9
                       :redundancy-candidate/rationale "similar text"
                       :redundancy-candidate/source-a {:section/id :x :section/path-raw "x.md"
                                                       :section/heading-path ["X"] :section/text "x"}
                       :redundancy-candidate/source-b {:section/id :y :section/path-raw "y.md"
                                                       :section/heading-path ["Y"] :section/text "y"}}]
          pkt2 (export/populate-from-redundancy pkt candidates)]
      (is (= 1 (count (:packet/inferred-candidates pkt2)))))))

;; ---------------------------------------------------------------------------
;; populate-from-decisions tests

(deftest populate-from-decisions-adds-accepted
  (testing "populate-from-decisions adds accepted interpretations"
    (let [pkt (export/make-packet)
          decisions [{:review/decision :accepted
                      :review/confidence 0.9
                      :review/rationale "good claim"
                      :review/id #uuid "00000000-0000-0000-0000-000000000001"
                      :review/request-id "req-1"
                      :review/evidence [{:evidence/section-id :s1
                                         :evidence/path-raw "doc.md"
                                         :evidence/heading-path ["H1"]
                                         :evidence/text "hello"}]}]
          pkt2 (export/populate-from-decisions pkt decisions)]
      (is (= 1 (count (:packet/accepted-interpretations pkt2)))))))

(deftest populate-from-decisions-ignores-rejected
  (testing "populate-from-decisions ignores rejected decisions"
    (let [pkt (export/make-packet)
          decisions [{:review/decision :rejected
                      :review/rationale "bad claim"
                      :review/evidence []}]
          pkt2 (export/populate-from-decisions pkt decisions)]
      (is (= [] (:packet/accepted-interpretations pkt2))))))

;; ---------------------------------------------------------------------------
;; populate-from-lineage-candidates tests (real ENG-005G durable shape)

(defn- durable-candidate
  ([candidate-id]
   (durable-candidate candidate-id :continues "a.md" "b.md"))
  ([candidate-id relation source-path target-path]
   {:lineage-candidate/id candidate-id
    :lineage-candidate/relation relation
    :lineage-candidate/confidence 0.8
    :lineage-candidate/generator-version "test-gen-v1"
    :lineage-candidate/source {:span/path-raw source-path
                               :span/heading-path ["Intro"]
                               :span/commit-oid "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa"}
    :lineage-candidate/target {:span/path-raw target-path
                               :span/heading-path ["Intro"]
                               :span/commit-oid "bbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbb"}}))

(deftest populate-from-lineage-candidates-adds-inferred
  (testing "populates from the real durable candidate shape, not the speculative one"
    (let [pkt (export/make-packet)
          candidates [(durable-candidate
                       #uuid "00000000-0000-0000-0000-000000000001")]
          pkt2 (export/populate-from-lineage-candidates pkt candidates)
          claim (first (:packet/inferred-candidates pkt2))]
      (is (= 1 (count (:packet/inferred-candidates pkt2))))
      (is (= :inferred (:claim/type claim)))
      (is (= 0.8 (:claim/confidence claim)))
      (is (= "a.md" (:evidence/path-raw (:claim/source claim))))
      (is (= #uuid "00000000-0000-0000-0000-000000000001"
             (:lineage-candidate/id (:claim/identifiers claim)))))))

(deftest populate-from-lineage-candidates-empty
  (let [pkt (export/make-packet)
        pkt2 (export/populate-from-lineage-candidates pkt [])]
    (is (= [] (:packet/inferred-candidates pkt2)))))

;; ---------------------------------------------------------------------------
;; joined candidate/review tests (real ENG-005A/005G durable shapes)

(deftest accepted-review-moves-candidate-with-evidence
  (testing "an accepted candidate is emitted once in the accepted tier with its original evidence"
    (let [candidate-id #uuid "00000000-0000-0000-0000-000000000001"
          pkt2 (export/populate-from-reviewed-lineage-candidates
                (export/make-packet)
                [(durable-candidate candidate-id)]
                [{:review-decision/id #uuid "00000000-0000-0000-0000-000000000002"
                  :review-decision/candidate-id candidate-id
                  :review-decision/decision :accepted
                  :review-decision/reason "good claim"
                  :review-decision/decided-at (java.util.Date. 1000)}])
          claim (first (:packet/accepted-interpretations pkt2))]
      (is (= 1 (count (:packet/accepted-interpretations pkt2))))
      (is (= [] (:packet/inferred-candidates pkt2)))
      (is (= "a.md" (:evidence/path-raw (:claim/source claim))))
      (is (= "b.md"
             (get-in claim [:claim/identifiers :target :evidence/path-raw])))
      (is (= "good claim" (:claim/rationale claim))))))

(deftest rejected-and-suppressed-reviews-remain-auditable
  (testing "negative dispositions are visible review outcomes, not live inferred candidates"
    (let [rejected-id (random-uuid)
          suppressed-id (random-uuid)
          pkt2 (export/populate-from-reviewed-lineage-candidates
                (export/make-packet)
                [(durable-candidate rejected-id :continues "a.md" "b.md")
                 (durable-candidate suppressed-id :duplicates "c.md" "d.md")]
                [{:review-decision/id (random-uuid)
                  :review-decision/candidate-id rejected-id
                  :review-decision/decision :rejected
                  :review-decision/reason "not supported"
                  :review-decision/decided-at (java.util.Date. 1000)}
                 {:review-decision/id (random-uuid)
                  :review-decision/candidate-id suppressed-id
                  :review-decision/decision :do-not-suggest
                  :review-decision/suppressed true
                  :review-decision/decided-at (java.util.Date. 1000)}])
          dispositions (set (map #(get-in % [:claim/identifiers :review/disposition])
                                 (:packet/observed-facts pkt2)))]
      (is (= [] (:packet/inferred-candidates pkt2)))
      (is (= [] (:packet/accepted-interpretations pkt2)))
      (is (= #{:rejected :do-not-suggest} dispositions)))))

(deftest relabel-persists-when-a-later-decision-accepts
  (testing "append-only review history folds to one accepted, relabelled interpretation"
    (let [candidate-id (random-uuid)
          pkt2 (export/populate-from-reviewed-lineage-candidates
                (export/make-packet)
                [(durable-candidate candidate-id)]
                [{:review-decision/id (random-uuid)
                  :review-decision/candidate-id candidate-id
                  :review-decision/decision :relabel
                  :review-decision/relabel-to :supersedes
                  :review-decision/decided-at (java.util.Date. 1000)}
                 {:review-decision/id (random-uuid)
                  :review-decision/candidate-id candidate-id
                  :review-decision/decision :accepted
                  :review-decision/decided-at (java.util.Date. 2000)}])
          claim (first (:packet/accepted-interpretations pkt2))]
      (is (= [] (:packet/inferred-candidates pkt2)))
      (is (= :supersedes
             (get-in claim [:claim/identifiers :lineage-candidate/relation])))
      (is (= :continues
             (get-in claim [:claim/identifiers :lineage-candidate/original-relation]))))))

;; ---------------------------------------------------------------------------
;; content-hash (tamper evidence)

(deftest add-content-hash-attaches-a-hash
  (let [pkt (export/add-content-hash (export/make-packet))]
    (is (string? (:packet/content-hash pkt)))
    (is (export/content-hash-valid? pkt))))

(deftest content-hash-detects-tampering
  (testing "editing a claim after export invalidates the hash"
    (let [pkt (-> (export/make-packet)
                  (export/add-observed-fact (export/make-claim :observed "original" (export/make-no-source "n/a")))
                  (export/add-content-hash))
          tampered (assoc-in pkt [:packet/observed-facts 0 :claim/statement] "silently edited")]
      (is (export/content-hash-valid? pkt))
      (is (not (export/content-hash-valid? tampered))))))

(deftest content-hash-valid-false-when-absent
  (is (not (export/content-hash-valid? (export/make-packet)))))

(deftest content-hash-unaffected-by-provenance-metadata
  (testing "two packets with identical claims but different :packet/id/created-at hash the same"
    (let [claim (export/make-claim :observed "same claim" (export/make-no-source "n/a"))
          pkt-a (export/add-content-hash (export/add-observed-fact (export/make-packet) claim))
          pkt-b (export/add-content-hash (export/add-observed-fact (export/make-packet) claim))]
      (is (not= (:packet/id pkt-a) (:packet/id pkt-b)))
      (is (= (:packet/content-hash pkt-a) (:packet/content-hash pkt-b))))))

;; ---------------------------------------------------------------------------
;; populate-from-concepts tests

(deftest populate-from-concepts-adds-both
  (testing "populate-from-concepts adds concepts and RQs"
    (let [pkt (export/make-packet)
          concepts [{:concept/id #uuid "00000000-0000-0000-0000-000000000001"
                     :concept/name "Test Concept"
                     :concept/description "A test"
                     :concept/tags [:test]
                     :concept/evidence-links [{:evidence/section-id :s1
                                               :evidence/path-raw "a.md"
                                               :evidence/heading-path ["A"]
                                               :evidence/text "hello"}]}]
          rqs [{:research-question/id #uuid "00000000-0000-0000-0000-000000000002"
                :research-question/question "What is this?"
                :research-question/interpretation "Investigation"
                :research-question/priority :high
                :research-question/status :open
                :research-question/evidence-links [{:evidence/section-id :s2
                                                    :evidence/path-raw "b.md"
                                                    :evidence/heading-path ["B"]
                                                    :evidence/text "world"}]}]
          pkt2 (export/populate-from-concepts pkt concepts rqs)]
      (is (= 1 (count (:packet/accepted-interpretations pkt2))))
      (is (= 1 (count (:packet/open-questions pkt2)))))))

;; ---------------------------------------------------------------------------
;; populate-from-gaps tests

(deftest populate-from-gaps-adds-questions
  (testing "populate-from-gaps adds open questions"
    (let [pkt (export/make-packet)
          gaps [{:gap/type :todo-marker
                 :gap/description "TODO found"
                 :gap/confidence 0.8
                 :gap/evidence [{:evidence/section-id :s1
                                 :evidence/path-raw "a.md"
                                 :evidence/heading-path ["A"]
                                 :evidence/text "hello"}]
                 :gap/suggested-action :create-research-question}]
          pkt2 (export/populate-from-gaps pkt gaps)]
      (is (= 1 (count (:packet/open-questions pkt2)))))))

;; ---------------------------------------------------------------------------
;; Serialization tests

(deftest packet->edn-returns-string
  (testing "packet->edn returns EDN string"
    (let [pkt (export/make-packet :label "Test")
          edn (export/packet->edn pkt)]
      (is (string? edn))
      (is (.contains edn "Test")))))

(deftest packet->json-returns-string
  (testing "packet->json returns JSON string"
    (let [pkt (export/make-packet :label "Test")
          json-str (export/packet->json pkt)]
      (is (string? json-str))
      (is (.contains json-str "Test")))))

;; ---------------------------------------------------------------------------
;; Markdown rendering tests

(deftest packet->markdown-renders-header
  (testing "packet->markdown includes header"
    (let [pkt (export/make-packet :label "My Packet")
          md (export/packet->markdown pkt)]
      (is (.contains md "# My Packet"))
      (is (.contains md "**Generator:** export-v1")))))

(deftest packet->markdown-renders-observed
  (testing "packet->markdown renders observed facts"
    (let [pkt (-> (export/make-packet)
                  (export/add-observed-fact
                   (export/make-claim :observed "fact 1"
                                      (export/make-no-source "test"))))
          md (export/packet->markdown pkt)]
      (is (.contains md "## Observed Facts"))
      (is (.contains md "fact 1")))))

(deftest packet->markdown-renders-inferred
  (testing "packet->markdown renders inferred candidates"
    (let [pkt (-> (export/make-packet)
                  (export/add-inferred-candidate
                   (export/make-claim :inferred "candidate 1"
                                      (export/make-no-source "test"))))
          md (export/packet->markdown pkt)]
      (is (.contains md "## Inferred Candidates"))
      (is (.contains md "candidate 1")))))

(deftest packet->markdown-renders-accepted
  (testing "packet->markdown renders accepted interpretations"
    (let [pkt (-> (export/make-packet)
                  (export/add-accepted-interpretation
                   (export/make-claim :accepted "interp 1"
                                      (export/make-no-source "test"))))
          md (export/packet->markdown pkt)]
      (is (.contains md "## Accepted Interpretations"))
      (is (.contains md "interp 1")))))

(deftest packet->markdown-renders-questions
  (testing "packet->markdown renders open questions"
    (let [pkt (-> (export/make-packet)
                  (export/add-open-question
                   (export/make-claim :question "What?"
                                      (export/make-no-source "test"))))
          md (export/packet->markdown pkt)]
      (is (.contains md "## Open Questions"))
      (is (.contains md "What?")))))

(deftest packet->markdown-empty-sections-omitted
  (testing "packet->markdown omits empty sections"
    (let [pkt (export/make-packet)
          md (export/packet->markdown pkt)]
      (is (not (.contains md "## Observed")))
      (is (not (.contains md "## Inferred")))
      (is (not (.contains md "## Accepted")))
      (is (not (.contains md "## Open"))))))

(deftest packet->markdown-evidence-ref
  (testing "packet->markdown renders evidence references"
    (let [pkt (-> (export/make-packet)
                  (export/add-observed-fact
                   (export/make-claim :observed "fact"
                                      (export/make-evidence-ref
                                       {:section-id :s1
                                        :path-raw "doc.md"
                                        :heading-path ["H1"]
                                        :text "content"}
                                        :context "test context"))))
          md (export/packet->markdown pkt)]
      (is (.contains md "doc.md"))
      (is (.contains md "test context")))))
