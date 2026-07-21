(ns epiphany.domain.candidates-test
  (:require [clojure.test :refer [deftest testing is are]]
            [epiphany.domain.candidates :as candidates]
            [epiphany.domain.lineage :as lineage]
            [epiphany.domain.review :as review]
            [epiphany.law.registry :as registry]))

;; ---------------------------------------------------------------------------
;; Fixtures

(def ^:private oid-a "1111111111111111111111111111111111111111")
(def ^:private oid-b "2222222222222222222222222222222222222222")

(def ^:private obs-ctx
  {:resource-id #uuid "aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeeee"
   :adapter-version "test"})

(defn- span-a []
  (candidates/make-span {:path-raw "docs/a.md" :heading-path ["A" "One"] :commit-oid oid-a}))

(defn- span-b []
  (candidates/make-span {:path-raw "docs/b.md" :heading-path ["B" "Two"] :commit-oid oid-b}))

;; ---------------------------------------------------------------------------
;; make-candidate

(deftest make-candidate-is-provisional-test
  (testing "candidates are always minted at the PROVISIONAL tier"
    (let [c (candidates/make-candidate :continues (span-a) (span-b)
                                       :confidence 0.7
                                       :generator-version "gen-v1")]
      (is (uuid? (:lineage-candidate/id c)))
      (is (uuid? (:lineage-candidate/request-id c)))
      (is (= :continues (:lineage-candidate/relation c)))
      (is (= :provisional (:lineage-candidate/tier c)))
      (is (= 0.7 (:lineage-candidate/confidence c)))
      (is (inst? (:lineage-candidate/generated-at c))))))

(deftest make-candidate-invalid-relation-test
  (testing "rejects a relation outside the shared lineage vocabulary"
    (is (thrown? AssertionError
                 (candidates/make-candidate :not-a-relation (span-a) (span-b)
                                            :confidence 0.5 :generator-version "g")))))

(deftest make-candidate-custom-request-id-test
  (testing "an explicit request-id is preserved for idempotent retries"
    (let [rid (java.util.UUID/randomUUID)
          c (candidates/make-candidate :refines (span-a) (span-b)
                                       :confidence 0.5 :generator-version "g"
                                       :request-id rid)]
      (is (= rid (:lineage-candidate/request-id c))))))

(deftest relation-vocab-matches-lineage-test
  (testing "the candidate relation vocabulary is the lineage vocabulary, not a fork"
    (is (= lineage/relation-types candidates/relation-types))))

;; ---------------------------------------------------------------------------
;; candidate->observation (durable, schema-valid wrapping)

(deftest candidate->observation-is-schema-valid-test
  (testing "wraps candidates of every relation into a valid observation/lineage-candidate-v1"
    (are [relation] (registry/valid?
                     "observation/lineage-candidate-v1"
                     (candidates/candidate->observation
                      (candidates/make-candidate relation (span-a) (span-b)
                                                 :confidence 0.42 :generator-version "gen-v1")
                      obs-ctx))
      :near-duplicate :continues :refines :references
      :possibly-derived-from :possibly-supersedes :possible-contradiction)))

(deftest candidate->observation-carries-request-id-as-idempotency-key-test
  (testing "the candidate's request-id becomes the observation request-id"
    (let [rid (java.util.UUID/randomUUID)
          c   (candidates/make-candidate :continues (span-a) (span-b)
                                         :confidence 0.5 :generator-version "gen-v1"
                                         :request-id rid)
          obs (candidates/candidate->observation c obs-ctx)]
      (is (= rid (:observation/request-id obs)))
      (is (= :lineage/candidate-generated (:observation/type obs)))
      (is (= 1 (:observation/schema-version obs)))
      (is (= :provisional (:lineage-candidate/tier obs)))
      (is (= (:resource-id obs-ctx) (:resource-id obs))))))

(deftest candidate->observation-requires-provenance-test
  (testing "resource-id and adapter-version are required"
    (let [c (candidates/make-candidate :continues (span-a) (span-b)
                                       :confidence 0.5 :generator-version "g")]
      (is (thrown? AssertionError (candidates/candidate->observation c {:adapter-version "x"})))
      (is (thrown? AssertionError (candidates/candidate->observation c {:resource-id (java.util.UUID/randomUUID)}))))))

;; ---------------------------------------------------------------------------
;; Generation seam: from-lineage-candidate

(deftest from-lineage-candidate-seam-test
  (testing "a domain/lineage candidate maps onto a schema-valid store candidate"
    (let [lc {:lineage-candidate/id (java.util.UUID/randomUUID)
              :lineage-candidate/relation :continues
              :lineage-candidate/confidence 0.66
              :lineage-candidate/generator-version lineage/lineage-generator-version
              :lineage-candidate/source {:section/path-raw "docs/a.md"
                                         :section/heading-path ["A"]
                                         :section/commit-oid oid-a}
              :lineage-candidate/target {:section/path-raw "docs/b.md"
                                         :section/heading-path ["B"]
                                         :section/commit-oid oid-b}
              :lineage-candidate/status :provisional}
          c   (candidates/from-lineage-candidate lc)
          obs (candidates/candidate->observation c obs-ctx)]
      (is (= (:lineage-candidate/id lc) (:lineage-candidate/id c))
          "candidate id is preserved so decisions join correctly")
      (is (= :continues (:lineage-candidate/relation c)))
      (is (= "docs/a.md" (get-in c [:lineage-candidate/source :span/path-raw])))
      (is (= oid-b (get-in c [:lineage-candidate/target :span/commit-oid])))
      (is (registry/valid? "observation/lineage-candidate-v1" obs)))))

;; ---------------------------------------------------------------------------
;; Query filters — one test per queryable dimension (AC3)

(def now (java.util.Date.))
(def t-minus-1h (java.util.Date. (- (.getTime now) 3600000)))
(def t-minus-2h (java.util.Date. (- (.getTime now) 7200000)))

(defn- cand [relation gen conf at]
  (candidates/make-candidate relation (span-a) (span-b)
                             :confidence conf :generator-version gen :generated-at at))

(def ^:private cid-1 #uuid "00000000-0000-0000-0000-000000000001")

(def sample
  [(assoc (cand :continues "gen-v1" 0.9 now) :lineage-candidate/id cid-1)
   (cand :refines "gen-v1" 0.5 t-minus-1h)
   (cand :continues "gen-v2" 0.2 t-minus-2h)])

(deftest query-by-candidate-id-test
  (testing "candidates are queryable by candidate id"
    (let [r (candidates/by-candidate-id sample cid-1)]
      (is (= 1 (count r)))
      (is (= cid-1 (:lineage-candidate/id (first r)))))))

(deftest query-by-relation-test
  (testing "candidates are queryable by relation type"
    (is (= 2 (count (candidates/by-relation sample :continues))))
    (is (= 1 (count (candidates/by-relation sample :refines))))))

(deftest query-by-generator-version-test
  (testing "candidates are queryable by generator version"
    (is (= 2 (count (candidates/by-generator-version sample "gen-v1"))))
    (is (= 1 (count (candidates/by-generator-version sample "gen-v2"))))))

(deftest query-by-confidence-band-test
  (testing "candidates are queryable by confidence band"
    (is (= 1 (count (candidates/by-confidence-band sample 0.8 1.0))))
    (is (= 2 (count (candidates/by-confidence-band sample 0.4 1.0))))
    (is (= 3 (count (candidates/by-confidence-band sample nil nil))))))

(deftest query-by-time-range-test
  (testing "candidates are queryable by generation time [from, to)"
    (let [r (candidates/by-time-range sample t-minus-1h now)]
      (is (= 1 (count r)))
      (is (= t-minus-1h (:lineage-candidate/generated-at (first r)))))))

;; ---------------------------------------------------------------------------
;; Disposition join (AC4)

(defn- decision [cid type & opts]
  (apply review/make-decision cid type opts))

(deftest disposition-provisional-by-default-test
  (testing "with no decisions a candidate is provisional"
    (let [c (first sample)]
      (is (= :provisional (candidates/disposition c [])))
      (is (not (candidates/established? c [])))
      (is (candidates/surfaced? c [])))))

(deftest disposition-accepted-test
  (testing "an accepted candidate resolves to :accepted and is established"
    (let [c (first sample)
          ds [(decision cid-1 :accepted)]]
      (is (= :accepted (candidates/disposition c ds)))
      (is (candidates/established? c ds))
      (is (candidates/surfaced? c ds)))))

(deftest disposition-rejected-never-established-test
  (testing "a rejected candidate is never established nor surfaced"
    (let [c (first sample)
          ds [(decision cid-1 :rejected :reason "stale")]]
      (is (= :rejected (candidates/disposition c ds)))
      (is (not (candidates/established? c ds)))
      (is (not (candidates/surfaced? c ds))))))

(deftest disposition-do-not-suggest-never-established-test
  (testing "a do-not-suggest candidate is never established nor surfaced"
    (let [c (first sample)
          ds [(decision cid-1 :do-not-suggest :suppressed true)]]
      (is (= :do-not-suggest (candidates/disposition c ds)))
      (is (not (candidates/established? c ds)))
      (is (not (candidates/surfaced? c ds))))))

(deftest disposition-latest-terminal-wins-test
  (testing "the latest terminal decision wins; neutral decisions do not settle"
    (let [c (first sample)
          older (assoc (decision cid-1 :accepted)
                       :review-decision/decided-at t-minus-2h)
          neutral (assoc (decision cid-1 :annotated :annotation "note")
                         :review-decision/decided-at t-minus-1h)
          newer (assoc (decision cid-1 :rejected :reason "changed my mind")
                       :review-decision/decided-at now)]
      ;; newest terminal (rejected) wins over older accept; the annotate is neutral
      (is (= :rejected (candidates/disposition c [older neutral newer])))
      ;; annotate alone leaves it at the prior terminal (accepted)
      (is (= :accepted (candidates/disposition c [older neutral]))))))

(deftest with-disposition-annotates-test
  (testing "with-disposition tags each candidate; surfaced-candidates drops rejected"
    (let [ds [(decision cid-1 :rejected :reason "x")]
          tagged (candidates/with-disposition sample ds)]
      (is (= :rejected (:lineage-candidate/disposition (first tagged))))
      (is (every? #(= :provisional (:lineage-candidate/disposition %)) (rest tagged)))
      (is (= 2 (count (candidates/surfaced-candidates sample ds))))
      (is (empty? (candidates/established-candidates sample ds))))))
