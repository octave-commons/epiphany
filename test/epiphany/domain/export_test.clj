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
