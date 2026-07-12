(ns epiphany.domain.review-test
  (:require [clojure.test :refer [deftest testing is are]]
            [epiphany.domain.review :as review]))

;; ---------------------------------------------------------------------------
;; make-decision

(deftest make-decision-accepted-test
  (testing "creates an accepted decision"
    (let [cid (java.util.UUID/randomUUID)
          d (review/make-decision cid :accepted)]
      (is (uuid? (:review-decision/id d)))
      (is (= cid (:review-decision/candidate-id d)))
      (is (= :accepted (:review-decision/decision d)))
      (is (uuid? (:review-decision/request-id d)))
      (is (inst? (:review-decision/decided-at d))))))

(deftest make-decision-rejected-test
  (testing "creates a rejected decision with reason"
    (let [d (review/make-decision (java.util.UUID/randomUUID) :rejected
                                  :reason "Outdated information")]
      (is (= :rejected (:review-decision/decision d)))
      (is (= "Outdated information" (:review-decision/reason d))))))

(deftest make-decision-relabel-test
  (testing "creates a relabel decision"
    (let [d (review/make-decision (java.util.UUID/randomUUID) :relabel
                                  :relabel-to :refines)]
      (is (= :relabel (:review-decision/decision d)))
      (is (= :refines (:review-decision/relabel-to d))))))

(deftest make-decision-annotated-test
  (testing "creates an annotated decision"
    (let [d (review/make-decision (java.util.UUID/randomUUID) :annotated
                                  :annotation "Needs further review")]
      (is (= :annotated (:review-decision/decision d)))
      (is (= "Needs further review" (:review-decision/annotation d))))))

(deftest make-decision-do-not-suggest-test
  (testing "creates a do-not-suggest decision with suppressed flag"
    (let [d (review/make-decision (java.util.UUID/randomUUID) :do-not-suggest
                                  :suppressed true)]
      (is (= :do-not-suggest (:review-decision/decision d)))
      (is (true? (:review-decision/suppressed d))))))

(deftest make-decision-deferred-test
  (testing "creates a deferred decision"
    (let [d (review/make-decision (java.util.UUID/randomUUID) :deferred)]
      (is (= :deferred (:review-decision/decision d))))))

(deftest make-decision-custom-request-id-test
  (testing "uses provided request-id for idempotency"
    (let [rid (java.util.UUID/randomUUID)
          d (review/make-decision (java.util.UUID/randomUUID) :accepted
                                  :request-id rid)]
      (is (= rid (:review-decision/request-id d))))))

(deftest make-decision-invalid-type-test
  (testing "throws on invalid decision type"
    (is (thrown? AssertionError
                (review/make-decision (java.util.UUID/randomUUID) :invalid)))))

;; ---------------------------------------------------------------------------
;; Query helpers

(def now (java.util.Date.))
(def t-minus-1h (java.util.Date. (- (.getTime now) 3600000)))
(def t-minus-2h (java.util.Date. (- (.getTime now) 7200000)))

(def sample-decisions
  [(review/make-decision #uuid "00000000-0000-0000-0000-000000000001" :accepted)
   (review/make-decision #uuid "00000000-0000-0000-0000-000000000001" :rejected
                         :reason "test")
   (review/make-decision #uuid "00000000-0000-0000-0000-000000000002" :accepted)
   (review/make-decision #uuid "00000000-0000-0000-0000-000000000003" :do-not-suggest
                         :suppressed true)])

(deftest by-candidate-test
  (testing "filters decisions by candidate ID"
    (let [cid #uuid "00000000-0000-0000-0000-000000000001"
          result (review/by-candidate sample-decisions cid)]
      (is (= 2 (count result)))
      (is (every? #(= cid (:review-decision/candidate-id %)) result)))))

(deftest by-decision-type-test
  (testing "filters decisions by type"
    (let [result (review/by-decision-type sample-decisions :accepted)]
      (is (= 2 (count result))))))

(deftest by-time-range-test
  (testing "filters decisions by time range"
    (let [d1 (assoc (review/make-decision (java.util.UUID/randomUUID) :accepted)
                    :review-decision/decided-at t-minus-2h)
          d2 (assoc (review/make-decision (java.util.UUID/randomUUID) :rejected)
                    :review-decision/decided-at t-minus-1h)
          d3 (assoc (review/make-decision (java.util.UUID/randomUUID) :accepted)
                    :review-decision/decided-at now)
          decisions [d1 d2 d3]]
      ;; Only d2 in range [t-minus-1h, now)
      (let [result (review/by-time-range decisions t-minus-1h now)]
        (is (= 1 (count result)))
        (is (= (:review-decision/id d2) (:review-decision/id (first result))))))))

(deftest by-request-id-test
  (testing "finds decision by request ID"
    (let [rid (java.util.UUID/randomUUID)
          d (review/make-decision (java.util.UUID/randomUUID) :accepted :request-id rid)
          result (review/by-request-id [d] rid)]
      (is (some? result))
      (is (= rid (:review-decision/request-id result))))))

(deftest by-request-id-not-found-test
  (testing "returns nil for unknown request ID"
    (is (nil? (review/by-request-id [] (java.util.UUID/randomUUID))))))

(deftest rejected-candidates-test
  (testing "returns set of rejected/suppressed candidate IDs"
    (let [result (review/rejected-candidates sample-decisions)]
      (is (set? result))
      (is (contains? result #uuid "00000000-0000-0000-0000-000000000001"))
      (is (contains? result #uuid "00000000-0000-0000-0000-000000000003"))
      (is (not (contains? result #uuid "00000000-0000-0000-0000-000000000002"))))))

(deftest visible-decisions-default-test
  (testing "default view excludes suppressed do-not-suggest"
    (let [result (review/visible-decisions sample-decisions)]
      (is (= 3 (count result)))
      (is (not (some #(and (= :do-not-suggest (:review-decision/decision %))
                           (:review-decision/suppressed %))
                     result))))))

(deftest visible-decisions-include-suppressed-test
  (testing "include-suppressed? true shows all"
    (let [result (review/visible-decisions sample-decisions true)]
      (is (= 4 (count result))))))
