(ns epiphany.domain.boundary-test
  (:require [clojure.test :refer [deftest testing is]]
            [epiphany.domain.boundary :as b]))

;; ---------------------------------------------------------------------------
;; classify-continuity

(deftest classify-continuity-strong-test
  (testing "score 0.95 → :strong-continuity"
    (is (= :strong-continuity (b/classify-continuity 0.95))))
  (testing "score 0.90 → :strong-continuity"
    (is (= :strong-continuity (b/classify-continuity 0.90)))))

(deftest classify-continuity-substantial-test
  (testing "score 0.70 → :substantial-continuity"
    (is (= :substantial-continuity (b/classify-continuity 0.70))))
  (testing "score 0.85 → :substantial-continuity"
    (is (= :substantial-continuity (b/classify-continuity 0.85)))))

(deftest classify-continuity-partial-test
  (testing "score 0.40 → :partial-transforming"
    (is (= :partial-transforming (b/classify-continuity 0.40))))
  (testing "score 0.55 → :partial-transforming"
    (is (= :partial-transforming (b/classify-continuity 0.55)))))

(deftest classify-continuity-weak-test
  (testing "score 0.15 → :weak-continuity"
    (is (= :weak-continuity (b/classify-continuity 0.15))))
  (testing "score 0.30 → :weak-continuity"
    (is (= :weak-continuity (b/classify-continuity 0.30)))))

(deftest classify-continuity-discontinuity-test
  (testing "score 0.05 → :apparent-discontinuity"
    (is (= :apparent-discontinuity (b/classify-continuity 0.05)))))

;; ---------------------------------------------------------------------------
;; propose-boundary

(defn- make-continuity-result
  "Build a minimal continuity result map for testing."
  [score features]
  {:continuity/score score
   :continuity/policy-version "continuity-v1"
   :continuity/features features})

(def default-features
  {:text-similarity 0.8
   :frontmatter-changed false
   :frontmatter-keys-added []
   :frontmatter-keys-removed []
   :link-overlap-ratio 0.7
   :link-common []
   :link-added []
   :link-removed []
   :time-gap-seconds 3600.0
   :name-token-overlap 0.8
   :name-token-common []})

(deftest propose-boundary-above-threshold-test
  (testing "high score → no proposal"
    (let [result (make-continuity-result 0.85 default-features)
          proposal (b/propose-boundary result)]
      (is (false? (:proposed? proposal)))
      (is (nil? (:proposal proposal))))))

(deftest propose-boundary-content-change-test
  (testing "low score with content change → proposal"
    (let [features (assoc default-features
                          :text-similarity 0.2
                          :frontmatter-changed true)
          result (make-continuity-result 0.15 features)
          proposal (b/propose-boundary result)]
      (is (true? (:proposed? proposal)))
      (is (false? (:drift-only? proposal)))
      (is (= :weak-continuity (:classification proposal)))
      (is (contains? (:proposal proposal) :boundary/score))
      (is (= "boundary-v1" (get-in proposal [:proposal :boundary/policy-version]))))))

(deftest propose-boundary-time-gap-only-test
  (testing "low score from time gap alone → drift, no proposal"
    (let [features (assoc default-features
                          :time-gap-seconds (* 2 365 24 3600)) ;; 2 years
          result (make-continuity-result 0.25 features)
          proposal (b/propose-boundary result)]
      (is (false? (:proposed? proposal)))
      (is (true? (:drift-only? proposal))))))

(deftest propose-boundary-low-continuity-no-corroboration-test
  (testing "low continuity without corroborating signals → drift"
    (let [features (assoc default-features
                          :text-similarity 0.8    ;; above 0.7 threshold
                          :link-overlap-ratio 0.8 ;; above 0.5 threshold
                          :name-token-overlap 0.8  ;; above 0.5 threshold
                          :frontmatter-changed false
                          :time-gap-seconds (* 2 365 24 3600)) ;; 2 years
          result (make-continuity-result 0.20 features)
          proposal (b/propose-boundary result)]
      (is (false? (:proposed? proposal)))
      (is (true? (:drift-only? proposal))))))

(deftest propose-boundary-low-link-overlap-test
  (testing "low score with low link overlap → proposal"
    (let [features (assoc default-features
                          :text-similarity 0.5
                          :link-overlap-ratio 0.1)
          result (make-continuity-result 0.25 features)
          proposal (b/propose-boundary result)]
      (is (true? (:proposed? proposal))))))

(deftest propose-boundary-low-name-token-overlap-test
  (testing "low score with low name overlap → proposal"
    (let [features (assoc default-features
                          :text-similarity 0.4
                          :name-token-overlap 0.1)
          result (make-continuity-result 0.20 features)
          proposal (b/propose-boundary result)]
      (is (true? (:proposed? proposal))))))

(deftest propose-boundary-custom-threshold-test
  (testing "custom threshold is respected"
    (let [features (assoc default-features :text-similarity 0.3) ;; triggers content-change
          result (make-continuity-result 0.55 features)]
      ;; At default threshold (0.40), 0.55 is above → no proposal
      (is (false? (:proposed? (b/propose-boundary result))))
      ;; At high threshold (0.60), 0.55 is below AND has content change → proposal
      (is (true? (:proposed? (b/propose-boundary result 0.60)))))))

(deftest propose-boundary-includes-features-test
  (testing "proposal includes raw features"
    (let [features (assoc default-features :text-similarity 0.1 :frontmatter-changed true)
          result (make-continuity-result 0.10 features)
          proposal (b/propose-boundary result)]
      (is (some? (get-in proposal [:proposal :continuity/features])))
      (is (= 0.1 (get-in proposal [:proposal :continuity/features :text-similarity]))))))

;; ---------------------------------------------------------------------------
;; accept-proposal

(deftest accept-proposal-test
  (testing "returns a durable accept event"
    (let [proposal {:boundary/score 0.2
                    :boundary/threshold 0.4
                    :boundary/policy-version "boundary-v1"
                    :boundary/from-scores {}
                    :continuity/features {}}
          event (b/accept-proposal proposal)]
      (is (uuid? (:boundary-decision/id event)))
      (is (= :accepted (:boundary-decision/decision event)))
      (is (= proposal (:boundary-decision/proposal event)))
      (is (inst? (:boundary-decision/decided-at event))))))

;; ---------------------------------------------------------------------------
;; reject-proposal

(deftest reject-proposal-without-reason-test
  (testing "returns a durable reject event without reason"
    (let [proposal {:boundary/score 0.2}
          event (b/reject-proposal proposal)]
      (is (= :rejected (:boundary-decision/decision event)))
      (is (nil? (:boundary-decision/reason event))))))

(deftest reject-proposal-with-reason-test
  (testing "includes the rejection reason"
    (let [proposal {:boundary/score 0.2}
          event (b/reject-proposal proposal "Looks like gradual drift")]
      (is (= "Looks like gradual drift" (:boundary-decision/reason event))))))

;; ---------------------------------------------------------------------------
;; filter-visible

(deftest filter-visible-keeps-unrejected-test
  (testing "proposals without rejection are kept"
    (let [accepted {:proposal {:id 1}
                    :decisions [{:boundary-decision/decision :accepted}]}
          no-decision {:proposal {:id 2} :decisions []}
          result (b/filter-visible [accepted no-decision])]
      (is (= 2 (count result))))))

(deftest filter-visible-hides-rejected-test
  (testing "proposals with rejection are hidden"
    (let [rejected {:proposal {:id 1}
                    :decisions [{:boundary-decision/decision :rejected}]}
          accepted {:proposal {:id 2}
                    :decisions [{:boundary-decision/decision :accepted}]}
          result (b/filter-visible [rejected accepted])]
      (is (= 1 (count result)))
      (is (= 2 (get-in (first result) [:proposal :id]))))))
