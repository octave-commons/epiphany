(ns epiphany.domain.continuity-test
  (:require [clojure.test :refer [deftest testing is are]]
            [epiphany.domain.continuity :as c]))

;; ---------------------------------------------------------------------------
;; text-similarity

(deftest text-similarity-identical-test
  (testing "identical blobs return 1.0"
    (is (= 1.0 (c/text-similarity "hello world" "hello world")))))

(deftest text-similarity-empty-test
  (testing "empty blob returns 0.0"
    (is (zero? (c/text-similarity "" "hello"))))
  (testing "both empty returns 0.0"
    (is (zero? (c/text-similarity "" ""))))
  (testing "nil returns 0.0"
    (is (zero? (c/text-similarity nil "hello")))))

(deftest text-similarity-partial-overlap-test
  (testing "partial overlap returns value between 0 and 1"
    (let [score (c/text-similarity "abcdef" "abcxyz")]
      (is (< 0.0 score 1.0)))))

(deftest text-similarity-completely-different-test
  (testing "completely different blobs return low score"
    (let [score (c/text-similarity "aaaa" "bbbb")]
      (is (< score 0.1)))))

;; ---------------------------------------------------------------------------
;; frontmatter-delta

(deftest frontmatter-delta-identical-test
  (testing "identical frontmatter returns :changed false, empty diffs"
    (let [blob "---\ntitle: Foo\nauthor: Bar\n---\nBody"
          result (c/frontmatter-delta blob blob)]
      (is (false? (:changed result)))
      (is (= [] (:keys-added result)))
      (is (= [] (:keys-removed result)))
      (is (= [] (:keys-modified result))))))

(deftest frontmatter-delta-modified-value-test
  (testing "modified value appears in :keys-modified"
    (let [a "---\ntitle: Old\n---\n"
          b "---\ntitle: New\n---\n"
          result (c/frontmatter-delta a b)]
      (is (true? (:changed result)))
      (is (= ["title"] (:keys-modified result))))))

(deftest frontmatter-delta-added-key-test
  (testing "new key appears in :keys-added"
    (let [a "---\ntitle: X\n---\n"
          b "---\ntitle: X\nauthor: Y\n---\n"
          result (c/frontmatter-delta a b)]
      (is (= ["author"] (:keys-added result))))))

(deftest frontmatter-delta-removed-key-test
  (testing "removed key appears in :keys-removed"
    (let [a "---\ntitle: X\nauthor: Y\n---\n"
          b "---\ntitle: X\n---\n"
          result (c/frontmatter-delta a b)]
      (is (= ["author"] (:keys-removed result))))))

(deftest frontmatter-delta-no-frontmatter-test
  (testing "no frontmatter returns empty deltas"
    (let [result (c/frontmatter-delta "plain text" "other text")]
      (is (false? (:changed result)))
      (is (= [] (:keys-added result))))))

(deftest frontmatter-delta-one-missing-test
  (testing "one blob missing frontmatter returns empty deltas"
    (let [result (c/frontmatter-delta "---\ntitle: X\n---" "no frontmatter")]
      (is (false? (:changed result))))))

;; ---------------------------------------------------------------------------
;; explicit-link-overlap

(deftest explicit-link-overlap-identical-test
  (testing "identical links return full overlap"
    (let [blob "[Go](http://a.com) [Doc](http://b.com)"
          result (c/explicit-link-overlap blob blob)]
      (is (= 1.0 (:overlap-ratio result)))
      (is (= [] (:added result)))
      (is (= [] (:removed result))))))

(deftest explicit-link-overlap-mixed-test
  (testing "added and removed links computed correctly"
    (let [a "[A](http://a.com) [B](http://b.com)"
          b "[B](http://b.com) [C](http://c.com)"
          result (c/explicit-link-overlap a b)]
      (is (= ["[B](http://b.com)"] (:common result)))
      (is (= 1 (count (:added result))))
      (is (= 1 (count (:removed result)))))))

(deftest explicit-link-overlap-no-links-test
  (testing "no links returns empty and ratio 0.0"
    (let [result (c/explicit-link-overlap "plain text" "more text")]
      (is (zero? (:overlap-ratio result)))
      (is (= [] (:common result))))))

(deftest explicit-link-overlap-added-only-test
  (testing "all new links return overlap 0"
    (let [result (c/explicit-link-overlap "no links" "[X](http://x.com)")]
      (is (zero? (:overlap-ratio result)))
      (is (= 1 (count (:added result)))))))

;; ---------------------------------------------------------------------------
;; time-gap

(deftest time-gap-basic-test
  (testing "computes gap in seconds"
    (let [a (java.util.Date. 1000000000000)
          b (java.util.Date. 1000003600000)]
      (is (= 3600.0 (c/time-gap a b))))))

(deftest time-gap-nil-test
  (testing "nil timestamp returns nil"
    (is (nil? (c/time-gap nil (java.util.Date.))))
    (is (nil? (c/time-gap (java.util.Date.) nil)))))

(deftest time-gap-symmetric-test
  (testing "always returns non-negative value"
    (let [a (java.util.Date. 1000000000000)
          b (java.util.Date. 1000003600000)]
      (is (= (c/time-gap a b) (c/time-gap b a))))))

;; ---------------------------------------------------------------------------
;; shared-name-tokens

(deftest shared-name-tokens-identical-test
  (testing "same file name returns overlap 1.0"
    (let [result (c/shared-name-tokens "docs/foo.md" "src/foo.md")]
      (is (= 1.0 (:overlap-ratio result)))
      (is (= ["foo"] (:common result))))))

(deftest shared-name-tokens-partial-test
  (testing "partially shared name returns partial overlap"
    (let [result (c/shared-name-tokens "docs/foo-bar.md" "src/foo-baz.md")]
      (is (< 0.0 (:overlap-ratio result) 1.0))
      (is (some #(= "foo" %) (:common result))))))

(deftest shared-name-tokens-none-test
  (testing "no shared tokens returns 0"
    (let [result (c/shared-name-tokens "a/alpha.md" "b/bravo.md")]
      (is (zero? (:overlap-ratio result)))
      (is (= [] (:common result))))))

(deftest shared-name-tokens-subdirs-test
  (testing "extracts basename correctly from deep paths"
    (let [result (c/shared-name-tokens "a/b/c/test.md" "x/y/test.md")]
      (is (= 1.0 (:overlap-ratio result))))))

;; ---------------------------------------------------------------------------
;; compute-continuity-score

(deftest compute-continuity-score-basic-test
  (testing "score is between 0 and 1"
    (let [result (c/compute-continuity-score
                  {:text-similarity 0.8
                   :frontmatter-delta {:changed false :keys-added [] :keys-removed [] :keys-modified []}
                   :link-overlap {:overlap-ratio 0.5 :common [] :added [] :removed []}
                   :time-gap-seconds 3600.0
                   :name-token-overlap {:overlap-ratio 1.0 :common ["foo"]}})]
      (is (<= 0.0 (:continuity/score result) 1.0))
      (is (= "continuity-v1" (:continuity/policy-version result))))))

(deftest compute-continuity-score-zero-features-test
  (testing "all zero features gives low score"
    (let [result (c/compute-continuity-score
                  {:text-similarity 0.0
                   :frontmatter-delta {:changed true :keys-added [] :keys-removed [] :keys-modified []}
                   :link-overlap {:overlap-ratio 0.0 :common [] :added [] :removed []}
                   :time-gap-seconds (* 2 365 24 3600)
                   :name-token-overlap {:overlap-ratio 0.0 :common []}})]
      (is (< (:continuity/score result) 0.1)))))

(deftest compute-continuity-score-max-features-test
  (testing "perfect features gives high score"
    (let [result (c/compute-continuity-score
                  {:text-similarity 1.0
                   :frontmatter-delta {:changed false :keys-added [] :keys-removed [] :keys-modified []}
                   :link-overlap {:overlap-ratio 1.0 :common [] :added [] :removed []}
                   :time-gap-seconds 1.0
                   :name-token-overlap {:overlap-ratio 1.0 :common ["test"]}})]
      (is (> (:continuity/score result) 0.9)))))

(deftest compute-continuity-score-nil-time-test
  (testing "nil time-gap-seconds defaults to 0.5"
    (let [result (c/compute-continuity-score
                  {:text-similarity 0.0
                   :frontmatter-delta {:changed true :keys-added [] :keys-removed [] :keys-modified []}
                   :link-overlap {:overlap-ratio 0.0 :common [] :added [] :removed []}
                   :time-gap-seconds nil
                   :name-token-overlap {:overlap-ratio 0.0 :common []}})]
      (is (<= 0.0 (:continuity/score result) 1.0)))))

(deftest compute-continuity-score-features-map-test
  (testing "features map includes all expected keys"
    (let [result (c/compute-continuity-score
                  {:text-similarity 0.5
                   :frontmatter-delta {:changed true :keys-added ["x"] :keys-removed [] :keys-modified ["y"]}
                   :link-overlap {:overlap-ratio 0.3 :common ["l1"] :added ["l2"] :removed ["l3"]}
                   :time-gap-seconds 100.0
                   :name-token-overlap {:overlap-ratio 0.7 :common ["tok"]}})]
      (is (contains? (:continuity/features result) :text-similarity))
      (is (contains? (:continuity/features result) :frontmatter-changed))
      (is (contains? (:continuity/features result) :frontmatter-keys-added))
      (is (contains? (:continuity/features result) :link-overlap-ratio))
      (is (contains? (:continuity/features result) :link-common))
      (is (contains? (:continuity/features result) :time-gap-seconds))
      (is (contains? (:continuity/features result) :name-token-overlap)))))

;; ---------------------------------------------------------------------------
;; compute-continuity (integration)

(deftest compute-continuity-identical-revisions-test
  (testing "identical blobs give high continuity"
    (let [blob "---\ntitle: Test\n---\n# Hello\nSome content."
          result (c/compute-continuity blob blob "docs/test.md" "docs/test.md"
                                       (java.util.Date.) (java.util.Date.))]
      (is (>= (:continuity/score result) 0.8))
      (is (= "continuity-v1" (:continuity/policy-version result))))))

(deftest compute-continuity-different-revisions-test
  (testing "very different blobs give lower continuity"
    (let [a "---\ntitle: A\n---\nAlpha"
          b "---\ntitle: B\n---\nBravo and completely different."
          t1 (java.util.Date. 1000000000000)
          t2 (java.util.Date. 1000100000000)
          result (c/compute-continuity a b "doc.md" "doc.md" t1 t2)]
      (is (< (:continuity/score result) 0.8)))))

(deftest compute-continuity-different-paths-test
  (testing "different paths reduce name-token overlap"
    (let [blob "Some content"
          r1 (c/compute-continuity blob blob "a/alpha.md" "b/alpha.md"
                                   (java.util.Date.) (java.util.Date.))
          r2 (c/compute-continuity blob blob "a/alpha.md" "a/alpha.md"
                                   (java.util.Date.) (java.util.Date.))]
      ;; Same-name paths should score >= different-name paths
      (is (>= (:continuity/score r2) (:continuity/score r1))))))

(deftest compute-continuity-time-decay-test
  (testing "longer time gap reduces score"
    (let [blob "Some content"
          t1 (java.util.Date. 0)
          t2-short (java.util.Date. (* 3600 1000))    ;; 1 hour
          t2-long (java.util.Date. (* 365 24 3600 1000)) ;; 1 year
          r-short (c/compute-continuity blob blob "doc.md" "doc.md" t1 t2-short)
          r-long (c/compute-continuity blob blob "doc.md" "doc.md" t1 t2-long)]
      (is (>= (:continuity/score r-short) (:continuity/score r-long))))))
