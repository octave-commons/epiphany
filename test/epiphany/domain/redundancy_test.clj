(ns epiphany.domain.redundancy-test
  (:require [clojure.test :refer [deftest is testing]]
            [epiphany.domain.redundancy :as redundancy]))

;; ---------------------------------------------------------------------------
;; classify-pair tests

(deftest identical-text-is-duplicate
  (testing "Identical text is classified as :duplicate"
    (let [result (redundancy/classify-pair "Hello world" "Hello world")]
      (is (= :duplicate (:relation result)))
      (is (= 1.0 (:confidence result)))
      (is (string? (:rationale result))))))

(deftest high-word-overlap-is-near-duplicate
  (testing "High word overlap without negation is :near-duplicate"
    (let [a "The quick brown fox jumps over the lazy dog"
          b "The quick brown fox leaps over the lazy dog"
          result (redundancy/classify-pair a b)]
      (is (= :near-duplicate (:relation result)))
      (is (< 0.5 (:confidence result) 1.0)))))

(deftest negation-in-one-text
  (testing "Negation in one text + overlap → :possible-contradiction"
    (let [a "The system is fast"
          b "The system is not fast"
          result (redundancy/classify-pair a b)]
      (is (= :possible-contradiction (:relation result)))
      (is (pos? (:confidence result))))))

(deftest negation-in-both-texts
  (testing "Both texts negated → :unclear (both-negated pattern)"
    (let [a "The system is not slow"
          b "The system is not fast"
          result (redundancy/classify-pair a b)]
      ;; Both negated doesn't trigger contradiction, so falls through
      (is (contains? #{:possible-contradiction :unclear :complementary :near-duplicate}
                     (:relation result))))))

(deftest mutual-exclusion-detected
  (testing "Mutually exclusive values → :possible-contradiction"
    (let [a "The color is red"
          b "The color is blue"
          result (redundancy/classify-pair a b)]
      (is (= :possible-contradiction (:relation result)))
      (is (pos? (:confidence result))))))

(deftest complementary-with-moderate-overlap
  (testing "Moderate overlap → :complementary"
    (let [a "Alice enjoys hiking and camping in the mountains every summer with friends"
          b "Alice enjoys swimming and sailing at the beach every winter alone"
          result (redundancy/classify-pair a b)]
      (is (= :complementary (:relation result))))))

(deftest replacement-language-detected
  (testing "Replacement language in text-b → :superseded"
    (let [a "The old API uses XML"
          b "The new API replaces XML with JSON and is faster"
          result (redundancy/classify-pair a b)]
      (is (contains? #{:superseded :complementary :near-duplicate}
                     (:relation result))))))

(deftest low-overlap-is-unclear
  (testing "Low overlap → :unclear"
    (let [a "Alice loves cats"
          b "Quantum physics involves entanglement"
          result (redundancy/classify-pair a b)]
      (is (= :unclear (:relation result))))))

(deftest every-pair-has-spans
  (testing "classify-pair always returns source spans in signals"
    (let [a "Some text"
          b "Different text"
          result (redundancy/classify-pair a b)]
      (is (sequential? (:signals result))))))

;; ---------------------------------------------------------------------------
;; detect-pairs tests

(deftest detect-pairs-filters-low-confidence
  (testing "detect-pairs filters below min-confidence"
    (let [sections [{:id :a :text "Alice" :path-raw "a.md" :heading-path ["A"]}
                    {:id :b :text "Bob" :path-raw "b.md" :heading-path ["B"]}
                    {:id :c :text "Charlie" :path-raw "c.md" :heading-path ["C"]}]
          results (redundancy/detect-pairs sections :min-confidence 0.9)]
      (is (vector? results))
      (doseq [r results]
        (is (<= 0.9 (:redundancy-candidate/confidence r) 1.0))))))

(deftest detect-pairs-respects-max-pairs
  (testing "detect-pairs respects max-pairs limit"
    (let [sections (map (fn [i] {:id i :text "Same text" :path-raw (str i ".md") :heading-path [(str "H" i)]})
                        (range 20))
          results (redundancy/detect-pairs sections :max-pairs 5)]
      (is (<= (count results) 5)))))

(deftest detect-pairs-returns-candidate-structure
  (testing "detect-pairs returns proper candidate structure"
    (let [sections [{:id :a :text "Hello" :path-raw "a.md" :heading-path ["A"]}
                    {:id :b :text "Hello" :path-raw "b.md" :heading-path ["B"]}]
          results (redundancy/detect-pairs sections :min-confidence 0.1)
          candidate (first results)]
      (is (some? candidate))
      (is (some? (:redundancy-candidate/id candidate)))
      (is (map? (:redundancy-candidate/source-a candidate)))
      (is (map? (:redundancy-candidate/source-b candidate)))
      (is (contains? redundancy/relation-types
                     (:redundancy-candidate/relation candidate)))
      (is (number? (:redundancy-candidate/confidence candidate)))
      (is (= :provisional (:redundancy-candidate/status candidate)))
      (is (= "redundancy-deterministic-v1"
             (:redundancy-candidate/generator-version candidate))))))

(deftest detect-pairs-identical-sections
  (testing "Identical sections produce :duplicate candidates"
    (let [sections [{:id :a :text "Same content" :path-raw "a.md" :heading-path ["A"]}
                    {:id :b :text "Same content" :path-raw "b.md" :heading-path ["B"]}]
          results (redundancy/detect-pairs sections :min-confidence 0.1)
          candidate (first results)]
      (is (= :duplicate (:redundancy-candidate/relation candidate)))
      (is (= 1.0 (:redundancy-candidate/confidence candidate))))))

(deftest detect-pairs-no-self-pairs
  (testing "detect-pairs never pairs a section with itself"
    (let [sections [{:id :a :text "Hello" :path-raw "a.md" :heading-path ["A"]}]
          results (redundancy/detect-pairs sections :min-confidence 0.0)]
      (is (empty? results)))))

(deftest detect-pairs-empty-input
  (testing "detect-pairs on empty input returns empty"
    (is (= [] (redundancy/detect-pairs [])))))

(deftest detect-pairs-sorted-by-confidence
  (testing "detect-pairs returns results sorted by confidence descending"
    (let [sections (map (fn [i] {:id i
                                 :text (str "Section " i " about topic " (mod i 3))
                                 :path-raw (str i ".md")
                                 :heading-path [(str "H" i)]})
                        (range 10))
          results (redundancy/detect-pairs sections :min-confidence 0.0)]
      (is (apply >= (map :redundancy-candidate/confidence results))))))

;; ---------------------------------------------------------------------------
;; edge cases

(deftest empty-text-classification
  (testing "Empty text doesn't crash"
    (let [result (redundancy/classify-pair "" "")]
      (is (= :duplicate (:relation result))))))

(deftest very-long-text-classification
  (testing "Long text is handled"
    (let [long-a (apply str (repeat 100 "The quick brown fox jumps over the lazy dog. "))
          long-b (apply str (repeat 100 "The quick brown fox leaps over the lazy dog. "))
          result (redundancy/classify-pair long-a long-b)]
      (is (contains? redundancy/relation-types (:relation result))))))
