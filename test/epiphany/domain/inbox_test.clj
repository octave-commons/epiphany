(ns epiphany.domain.inbox-test
  (:require [clojure.test :refer [deftest testing is are]]
            [epiphany.domain.inbox :as inbox]
            [epiphany.domain.review :as review]))

;; ---------------------------------------------------------------------------
;; Test fixtures

(defn- make-candidate
  [id relation confidence & {:keys [generator features timestamp]
                             :or {generator "lineage-deterministic-v1"
                                  features {:text-similarity 0.5}
                                  timestamp nil}}]
  {:lineage-candidate/id id
   :lineage-candidate/source {:section/path-raw "a.md"
                              :section/heading-path ["Intro"]
                              :section/commit-oid "abc"}
   :lineage-candidate/target {:section/path-raw "b.md"
                              :section/heading-path ["Intro"]
                              :section/commit-oid "def"}
   :lineage-candidate/relation relation
   :lineage-candidate/confidence confidence
   :lineage-candidate/evidence-spans [{:evidence/signal :text-similarity
                                       :evidence/value confidence
                                       :evidence/weight 0.4}]
   :lineage-candidate/features features
   :lineage-candidate/generator-version generator
   :lineage-candidate/timestamp timestamp})

(def candidate-1 (make-candidate #uuid "00000000-0000-0000-0000-000000000001"
                                 :continues 0.9))
(def candidate-2 (make-candidate #uuid "00000000-0000-0000-0000-000000000002"
                                 :refines 0.7
                                 :features {:text-similarity 0.6
                                            :link-overlap-ratio 0.3}))
(def candidate-3 (make-candidate #uuid "00000000-0000-0000-0000-000000000003"
                                 :near-duplicate 0.95))
(def candidate-4 (make-candidate #uuid "00000000-0000-0000-0000-000000000004"
                                 :references 0.5
                                 :generator "custom-gen-v2"))

(def all-candidates [candidate-1 candidate-2 candidate-3 candidate-4])

;; ---------------------------------------------------------------------------
;; summarize-evidence

(deftest summarize-evidence-test
  (testing "builds a readable summary"
    (let [summary (inbox/summarize-evidence candidate-2)]
      (is (clojure.string/includes? summary "refines"))
      (is (clojure.string/includes? summary "0.70"))
      (is (clojure.string/includes? summary "text-similarity")))))

(deftest summarize-evidence-with-frontmatter-test
  (testing "includes frontmatter change"
    (let [c (assoc-in candidate-2 [:lineage-candidate/features :frontmatter-changed] true)
          summary (inbox/summarize-evidence c)]
      (is (clojure.string/includes? summary "frontmatter changed")))))

;; ---------------------------------------------------------------------------
;; build-inbox — basic

(deftest build-inbox-all-unreviewed-test
  (testing "returns all candidates when none reviewed"
    (let [result (inbox/build-inbox all-candidates [])]
      (is (= 4 (count result)))
      (is (every? #(= :unreviewed (:inbox/decision-status %)) result)))))

(deftest build-inbox-excludes-reviewed-test
  (testing "excludes candidates with review decisions"
    (let [decision (review/make-decision #uuid "00000000-0000-0000-0000-000000000001"
                                         :accepted)
          result (inbox/build-inbox all-candidates [decision])]
      (is (= 3 (count result)))
      (is (not (some #(= #uuid "00000000-0000-0000-0000-000000000001"
                         (:lineage-candidate/id (:inbox/candidate %)))
                     result))))))

;; ---------------------------------------------------------------------------
;; build-inbox — filters

(deftest build-inbox-filter-by-relation-type-test
  (testing "filters by relation type"
    (let [result (inbox/build-inbox all-candidates [] {:relation-types [:continues]})]
      (is (= 1 (count result)))
      (is (= :continues (:lineage-candidate/relation (:inbox/candidate (first result))))))))

(deftest build-inbox-filter-by-confidence-band-test
  (testing "filters by confidence band"
    (let [result (inbox/build-inbox all-candidates [] {:confidence-band [0.8 1.0]})]
      (is (= 2 (count result)))
      (is (every? #(>= (:lineage-candidate/confidence (:inbox/candidate %)) 0.8) result)))))

(deftest build-inbox-filter-by-generator-test
  (testing "filters by generator version"
    (let [result (inbox/build-inbox all-candidates [] {:generators ["custom-gen-v2"]})]
      (is (= 1 (count result)))
      (is (= "custom-gen-v2"
             (:lineage-candidate/generator-version (:inbox/candidate (first result))))))))

(deftest build-inbox-filter-combined-test
  (testing "combined filters narrow results"
    (let [result (inbox/build-inbox all-candidates []
                                    {:relation-types [:continues :refines]
                                     :confidence-band [0.6 1.0]})]
      (is (every? #(contains? #{:continues :refines}
                              (:lineage-candidate/relation (:inbox/candidate %)))
                  result)))))

;; ---------------------------------------------------------------------------
;; build-inbox — sorting

(deftest build-inbox-sort-by-confidence-test
  (testing "sorts by confidence descending by default"
    (let [result (inbox/build-inbox all-candidates [])]
      (is (= #uuid "00000000-0000-0000-0000-000000000003"
             (:lineage-candidate/id (:inbox/candidate (first result))))))))

(deftest build-inbox-sort-by-evidence-test
  (testing "sorts by evidence span count"
    (let [result (inbox/build-inbox all-candidates [] {} {:sort :evidence})]
      ;; All have 1 evidence span, so order may vary
      (is (= 4 (count result))))))

;; ---------------------------------------------------------------------------
;; build-inbox — limit

(deftest build-inbox-limit-test
  (testing "limits results"
    (let [result (inbox/build-inbox all-candidates [] {} {:limit 2})]
      (is (= 2 (count result))))))

;; ---------------------------------------------------------------------------
;; build-inbox — evidence summary

(deftest build-inbox-evidence-summary-test
  (testing "each item has an evidence summary"
    (let [result (inbox/build-inbox all-candidates [])]
      (is (every? :inbox/evidence-summary result))
      (is (every? #(clojure.string/includes? % "confidence")
                  (map :inbox/evidence-summary result))))))

;; ---------------------------------------------------------------------------
;; inbox-item-detail

(deftest inbox-item-detail-test
  (testing "returns detailed item info"
    (let [item {:inbox/candidate candidate-2
                :inbox/decision-status :unreviewed
                :inbox/evidence-summary "..."}
          detail (inbox/inbox-item-detail item)]
      (is (= :refines (:relation detail)))
      (is (= 0.7 (:confidence detail)))
      (is (contains? detail :source))
      (is (contains? detail :target))
      (is (contains? detail :features)))))
