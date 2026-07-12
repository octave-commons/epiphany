(ns epiphany.domain.concept-test
  (:require [clojure.test :refer [deftest testing is]]
            [epiphany.domain.concept :as concept]))

;; ---------------------------------------------------------------------------
;; Test fixtures

(def sample-link {:path-raw "docs/arch.md"
                  :heading-path ["Architecture" "Events"]
                  :commit-oid "abc123"})

(def sample-link2 {:path-raw "docs/design.md"
                   :heading-path ["Design" "Overview"]
                   :commit-oid "def456"})

;; ---------------------------------------------------------------------------
;; make-concept

(deftest make-concept-basic-test
  (testing "creates a concept with required fields"
    (let [c (concept/make-concept "Event Sourcing")]
      (is (uuid? (:concept/id c)))
      (is (= "Event Sourcing" (:concept/name c)))
      (is (nil? (:concept/description c)))
      (is (= [] (:concept/evidence-links c)))
      (is (inst? (:concept/created-at c)))
      (is (= "user" (:concept/created-by c)))
      (is (= [] (:concept/tags c)))
      (is (= :concept (:concept/type c))))))

(deftest make-concept-with-options-test
  (testing "creates a concept with optional fields"
    (let [c (concept/make-concept "CQRS"
                                  :description "Command Query Responsibility Segregation"
                                  :evidence-links [sample-link]
                                  :created-by "analyst"
                                  :tags ["architecture" "patterns"])]
      (is (= "CQRS" (:concept/name c)))
      (is (= "Command Query Responsibility Segregation" (:concept/description c)))
      (is (= 1 (count (:concept/evidence-links c))))
      (is (= "analyst" (:concept/created-by c)))
      (is (= ["architecture" "patterns"] (:concept/tags c))))))

(deftest make-concept-no-name-test
  (testing "throws on empty name"
    (is (thrown? AssertionError (concept/make-concept "")))))

;; ---------------------------------------------------------------------------
;; add-evidence / remove-evidence

(deftest add-evidence-test
  (testing "adds evidence link to concept"
    (let [c (concept/make-concept "Test")
          c2 (concept/add-evidence c sample-link)]
      (is (= 1 (count (:concept/evidence-links c2))))
      (is (= sample-link (first (:concept/evidence-links c2)))))))

(deftest add-evidence-multiple-test
  (testing "can add multiple evidence links"
    (let [c (concept/make-concept "Test")
          c2 (-> c
                 (concept/add-evidence sample-link)
                 (concept/add-evidence sample-link2))]
      (is (= 2 (count (:concept/evidence-links c2)))))))

(deftest remove-evidence-test
  (testing "removes evidence link by path+heading+commit"
    (let [c (-> (concept/make-concept "Test")
                (concept/add-evidence sample-link)
                (concept/add-evidence sample-link2))
          c2 (concept/remove-evidence c "docs/arch.md" ["Architecture" "Events"] "abc123")]
      (is (= 1 (count (:concept/evidence-links c2))))
      (is (= sample-link2 (first (:concept/evidence-links c2)))))))

(deftest remove-evidence-no-match-test
  (testing "no-op when link not found"
    (let [c (-> (concept/make-concept "Test")
                (concept/add-evidence sample-link))
          c2 (concept/remove-evidence c "other.md" ["Other"] "xyz")]
      (is (= 1 (count (:concept/evidence-links c2)))))))

;; ---------------------------------------------------------------------------
;; add-tag / remove-tag

(deftest add-tag-test
  (testing "adds a tag"
    (let [c (concept/make-concept "Test")
          c2 (concept/add-tag c "important")]
      (is (= ["important"] (:concept/tags c2))))))

(deftest remove-tag-test
  (testing "removes a tag"
    (let [c (-> (concept/make-concept "Test")
                (concept/add-tag "important")
                (concept/add-tag "draft"))
          c2 (concept/remove-tag c "important")]
      (is (= ["draft"] (:concept/tags c2))))))

(deftest remove-tag-no-match-test
  (testing "no-op when tag not found"
    (let [c (concept/make-concept "Test")
          c2 (concept/remove-tag c "nonexistent")]
      (is (= [] (:concept/tags c2))))))

;; ---------------------------------------------------------------------------
;; search-entry-point

(deftest search-entry-point-test
  (testing "returns a search-ready map"
    (let [c (concept/make-concept "Test" :tags ["arch"])
          entry (concept/search-entry-point c)]
      (is (= :concept (:entry/type entry)))
      (is (= (:concept/id c) (:entry/id entry)))
      (is (= "Test" (:entry/name entry)))
      (is (= ["arch"] (:entry/tags entry))))))

;; ---------------------------------------------------------------------------
;; make-research-question

(deftest make-research-question-basic-test
  (testing "creates a research question"
    (let [rq (concept/make-research-question "How does event sourcing scale?")]
      (is (uuid? (:research-question/id rq)))
      (is (= "How does event sourcing scale?" (:research-question/question rq)))
      (is (= :open (:research-question/status rq)))
      (is (= :medium (:research-question/priority rq)))
      (is (= :research-question (:research-question/type rq))))))

(deftest make-research-question-with-options-test
  (testing "creates with options"
    (let [rq (concept/make-research-question "What about consistency?"
                                              :interpretation "Need to understand eventual consistency"
                                              :evidence-links [sample-link]
                                              :priority :high
                                              :status :investigating)]
      (is (= "Need to understand eventual consistency"
             (:research-question/interpretation rq)))
      (is (= 1 (count (:research-question/evidence-links rq))))
      (is (= :high (:research-question/priority rq)))
      (is (= :investigating (:research-question/status rq))))))

(deftest make-research-question-no-question-test
  (testing "throws on empty question"
    (is (thrown? AssertionError (concept/make-research-question "")))))

;; ---------------------------------------------------------------------------
;; answer-question / dismiss-question

(deftest answer-question-test
  (testing "marks question as answered"
    (let [rq (concept/make-research-question "Test?")
          rq2 (concept/answer-question rq "It scales well with partitioning")]
      (is (= :answered (:research-question/status rq2)))
      (is (= "It scales well with partitioning" (:research-question/answer rq2)))
      (is (inst? (:research-question/answered-at rq2))))))

(deftest dismiss-question-test
  (testing "dismisses question with reason"
    (let [rq (concept/make-research-question "Test?")
          rq2 (concept/dismiss-question rq "Out of scope")]
      (is (= :dismissed (:research-question/status rq2)))
      (is (= "Out of scope" (:research-question/dismiss-reason rq2)))
      (is (inst? (:research-question/dismissed-at rq2))))))

;; ---------------------------------------------------------------------------
;; rq-add-evidence / rq-remove-evidence

(deftest rq-add-evidence-test
  (testing "adds evidence to research question"
    (let [rq (concept/make-research-question "Test?")
          rq2 (concept/rq-add-evidence rq sample-link)]
      (is (= 1 (count (:research-question/evidence-links rq2)))))))

(deftest rq-remove-evidence-test
  (testing "removes evidence from research question"
    (let [rq (-> (concept/make-research-question "Test?")
                 (concept/rq-add-evidence sample-link)
                 (concept/rq-add-evidence sample-link2))
          rq2 (concept/rq-remove-evidence rq "docs/arch.md" ["Architecture" "Events"] "abc123")]
      (is (= 1 (count (:research-question/evidence-links rq2)))))))

;; ---------------------------------------------------------------------------
;; rq-search-entry-point

(deftest rq-search-entry-point-test
  (testing "returns a search-ready map"
    (let [rq (concept/make-research-question "Test?" :priority :high)
          entry (concept/rq-search-entry-point rq)]
      (is (= :research-question (:entry/type entry)))
      (is (= "Test?" (:entry/name entry)))
      (is (= :high (:entry/priority entry))))))

;; ---------------------------------------------------------------------------
;; list-concepts / list-research-questions

(deftest list-concepts-by-tag-test
  (testing "filters concepts by tag"
    (let [c1 (concept/make-concept "A" :tags ["arch"])
          c2 (concept/make-concept "B" :tags ["design"])
          result (concept/list-concepts [c1 c2] :tag "arch")]
      (is (= 1 (count result)))
      (is (= "A" (:concept/name (first result)))))))

(deftest list-concepts-by-name-test
  (testing "filters concepts by name substring"
    (let [c1 (concept/make-concept "Event Sourcing")
          c2 (concept/make-research-question "Command Query")  ;; not a concept
          result (concept/list-concepts [c1] :name-substring "event")]
      (is (= 1 (count result))))))

(deftest list-research-questions-by-status-test
  (testing "filters by status"
    (let [rq1 (concept/make-research-question "Q1" :status :open)
          rq2 (concept/make-research-question "Q2" :status :answered)
          result (concept/list-research-questions [rq1 rq2] :status :open)]
      (is (= 1 (count result)))
      (is (= "Q1" (:research-question/question (first result)))))))
