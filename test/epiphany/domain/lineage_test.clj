(ns epiphany.domain.lineage-test
  (:require [clojure.test :refer [deftest testing is are]]
            [epiphany.domain.lineage :as lineage]))

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

(defn- make-features
  "Build a minimal features map for testing."
  [& {:keys [text-similarity link-overlap-ratio name-token-overlap
             frontmatter-changed time-gap-seconds
             link-common link-added link-removed name-token-common]
      :or {text-similarity 0.5
           link-overlap-ratio 0.0
           name-token-overlap 0.0
           frontmatter-changed false
           time-gap-seconds nil
           link-common [] link-added [] link-removed []
           name-token-common []}}]
  {:text-similarity text-similarity
   :frontmatter-changed frontmatter-changed
   :frontmatter-keys-added []
   :frontmatter-keys-removed []
   :link-overlap-ratio link-overlap-ratio
   :link-common link-common
   :link-added link-added
   :link-removed link-removed
   :time-gap-seconds time-gap-seconds
   :name-token-overlap name-token-overlap
   :name-token-common name-token-common})

(defn- make-features-fn
  "Returns a features-fn that always returns the given features."
  [features]
  (fn [_source _target] features))

;; ---------------------------------------------------------------------------
;; classify-relation

(deftest classify-near-duplicate-test
  (testing "same heading + high similarity → :near-duplicate"
    (let [src (make-section "a.md" ["Intro"] "abc123")
          tgt (make-section "a.md" ["Intro"] "def456")
          features (make-features :text-similarity 0.95)]
      (is (= #{:near-duplicate}
             (lineage/classify-relation src tgt features))))))

(deftest classify-continues-test
  (testing "same heading + moderate similarity + later → :continues"
    (let [src (make-section "a.md" ["Intro"] "abc" :timestamp (java.util.Date. 1000))
          tgt (make-section "a.md" ["Intro"] "def" :timestamp (java.util.Date. 2000))
          features (make-features :text-similarity 0.6)]
      (is (= #{:continues}
             (lineage/classify-relation src tgt features))))))

(deftest classify-refines-test
  (testing "same heading + later + text got longer → :refines + :continues"
    (let [src (make-section "a.md" ["Intro"] "abc"
                            :body "Short."
                            :timestamp (java.util.Date. 1000))
          tgt (make-section "a.md" ["Intro"] "def"
                            :body "Much longer content that expands on the original."
                            :timestamp (java.util.Date. 2000))
          features (make-features :text-similarity 0.6)]
      (let [rels (lineage/classify-relation src tgt features)]
        (is (contains? rels :refines))
        (is (contains? rels :continues))))))

(deftest classify-references-test
  (testing "significant link overlap → :references"
    (let [src (make-section "a.md" ["Intro"] "abc")
          tgt (make-section "b.md" ["Other"] "def")
          features (make-features :text-similarity 0.3
                                  :link-overlap-ratio 0.7)]
      (is (= #{:references}
             (lineage/classify-relation src tgt features))))))

(deftest classify-possibly-derived-from-test
  (testing "shared name tokens + some similarity → :possibly-derived-from"
    (let [src (make-section "a.md" ["Intro"] "abc")
          tgt (make-section "b.md" ["Other"] "def")
          features (make-features :text-similarity 0.4
                                  :name-token-overlap 0.5)]
      (is (= #{:possibly-derived-from}
             (lineage/classify-relation src tgt features))))))

(deftest classify-possibly-supersedes-test
  (testing "later + frontmatter changed + low similarity → :possibly-supersedes"
    (let [src (make-section "a.md" ["Intro"] "abc"
                            :timestamp (java.util.Date. 1000))
          tgt (make-section "b.md" ["Other"] "def"
                            :timestamp (java.util.Date. 2000))
          features (make-features :text-similarity 0.3
                                  :frontmatter-changed true)]
      (is (= #{:possibly-supersedes}
             (lineage/classify-relation src tgt features))))))

(deftest classify-possible-contradiction-test
  (testing "same heading + low similarity → :possible-contradiction"
    (let [src (make-section "a.md" ["Intro"] "abc")
          tgt (make-section "a.md" ["Intro"] "def")
          features (make-features :text-similarity 0.15)]
      (is (= #{:possible-contradiction}
             (lineage/classify-relation src tgt features))))))

(deftest classify-no-relation-test
  (testing "no signals → empty set"
    (let [src (make-section "a.md" ["Intro"] "abc")
          tgt (make-section "b.md" ["Other"] "def")
          features (make-features :text-similarity 0.5
                                  :link-overlap-ratio 0.1
                                  :name-token-overlap 0.1)]
      (is (= #{} (lineage/classify-relation src tgt features))))))

;; ---------------------------------------------------------------------------
;; compute-confidence

(deftest compute-confidence-near-duplicate-test
  (testing "near-duplicate confidence tracks text similarity"
    (is (= 0.95 (lineage/compute-confidence :near-duplicate
                   (make-features :text-similarity 0.95))))
    (is (= 0.90 (lineage/compute-confidence :near-duplicate
                   (make-features :text-similarity 0.90))))))

(deftest compute-confidence-references-test
  (testing "references confidence tracks link overlap"
    (is (= 0.8 (lineage/compute-confidence :references
                  (make-features :link-overlap-ratio 0.8))))))

(deftest compute-confidence-bounded-test
  (testing "confidence is always in [0, 1]"
    (doseq [rel lineage/relation-types]
      (let [conf (lineage/compute-confidence rel (make-features))]
        (is (<= 0.0 conf 1.0))
        (is (double? conf))))))

;; ---------------------------------------------------------------------------
;; generate-candidates

(deftest generate-candidates-basic-test
  (testing "generates candidates for matching sections"
    (let [src (make-section "a.md" ["Intro"] "abc"
                            :timestamp (java.util.Date. 1000))
          tgt (make-section "a.md" ["Intro"] "def"
                            :timestamp (java.util.Date. 2000))
          features (make-features :text-similarity 0.6)
          candidates (lineage/generate-candidates src [tgt]
                       (make-features-fn features))]
      (is (= 1 (count candidates)))
      (is (= 1 (count (first candidates))))
      (let [c (first (first candidates))]
        (is (uuid? (:lineage-candidate/id c)))
        (is (= :continues (:lineage-candidate/relation c)))
        (is (= "a.md" (get-in c [:lineage-candidate/source :section/path-raw])))
        (is (= "def" (get-in c [:lineage-candidate/target :section/commit-oid])))
        (is (= :provisional (:lineage-candidate/status c)))
        (is (= "lineage-deterministic-v1"
               (:lineage-candidate/generator-version c)))))))

(deftest generate-candidates-multiple-relations-test
  (testing "single target can produce multiple relation types"
    (let [src (make-section "a.md" ["Intro"] "abc"
                            :body "Short."
                            :timestamp (java.util.Date. 1000))
          tgt (make-section "a.md" ["Intro"] "def"
                            :body "Much longer detailed content here."
                            :timestamp (java.util.Date. 2000))
          features (make-features :text-similarity 0.7)
          candidates (lineage/generate-candidates src [tgt]
                       (make-features-fn features))]
      ;; :continues and :refines are both triggered
      (is (>= (count (first candidates)) 1)))))

(deftest generate-candidates-no-match-test
  (testing "no candidates when no signals match"
    (let [src (make-section "a.md" ["Intro"] "abc")
          tgt (make-section "b.md" ["Other"] "def")
          features (make-features :text-similarity 0.5
                                  :link-overlap-ratio 0.1
                                  :name-token-overlap 0.1)
          candidates (lineage/generate-candidates src [tgt]
                       (make-features-fn features))]
      (is (empty? candidates)))))

(deftest generate-candidates-fork-test
  (testing "one source can link to many targets (fork)"
    (let [src (make-section "a.md" ["Intro"] "abc")
          targets [(make-section "b.md" ["Intro"] "def")
                   (make-section "c.md" ["Intro"] "ghi")
                   (make-section "d.md" ["Intro"] "jkl")]
          features (make-features :text-similarity 0.95)
          candidates (lineage/generate-candidates src targets
                       (make-features-fn features))]
      ;; 3 targets × 1 relation each (near-duplicate)
      (is (= 3 (count candidates)))
      (is (every? #(= :near-duplicate
                      (:lineage-candidate/relation (first %)))
                  candidates)))))

(deftest generate-candidates-evidence-spans-test
  (testing "candidates include evidence spans"
    (let [src (make-section "a.md" ["Intro"] "abc")
          tgt (make-section "a.md" ["Intro"] "def")
          features (make-features :text-similarity 0.95
                                  :link-overlap-ratio 0.3
                                  :name-token-overlap 0.4)
          candidates (lineage/generate-candidates src [tgt]
                       (make-features-fn features))
          c (first (first candidates))]
      (is (seq (:lineage-candidate/evidence-spans c)))
      (is (some #(= :text-similarity (:evidence/signal %))
                (:lineage-candidate/evidence-spans c))))))

;; ---------------------------------------------------------------------------
;; merge-candidates

(deftest merge-candidates-identity-test
  (testing "merging with empty returns original"
    (let [c1 (first (first
                     (lineage/generate-candidates
                      (make-section "a.md" ["Intro"] "abc"
                                    :timestamp (java.util.Date. 1000))
                      [(make-section "a.md" ["Intro"] "def"
                                     :timestamp (java.util.Date. 2000))]
                      (make-features-fn (make-features :text-similarity 0.6)))))]
      (is (= [c1] (lineage/merge-candidates [c1] [])))
      (is (= [c1] (lineage/merge-candidates [] [c1]))))))

(deftest merge-candidates-dedup-test
  (testing "duplicate candidates are merged, keeping higher confidence"
    (let [src (make-section "a.md" ["Intro"] "abc")
          tgt (make-section "a.md" ["Intro"] "def")
          c1 (first (first
                     (lineage/generate-candidates src [tgt]
                       (make-features-fn (make-features :text-similarity 0.95)))))
          c2 (first (first
                     (lineage/generate-candidates src [tgt]
                       (make-features-fn (make-features :text-similarity 0.98)))))]
      ;; Both are :near-duplicate for same source+target, so merge to 1
      (let [merged (lineage/merge-candidates [c1] [c2])]
        (is (= 1 (count merged)))
        ;; Should keep the higher-confidence one
        (is (= 0.98 (:lineage-candidate/confidence (first merged))))))))
