(ns epiphany.domain.file-lineage-test
  (:require [clojure.test :refer [deftest is testing]]
            [epiphany.domain.file-lineage :as fl]
            [epiphany.infra.git :as git]))

(deftest diff-entry->candidate-modify
  (testing "modify entry produces continues candidate"
    (let [entry {:diff/change-type :modify
                 :diff/old-path "src/foo.md"
                 :diff/new-path "src/foo.md"
                 :diff/old-blob-oid "aaa"
                 :diff/new-blob-oid "bbb"}
          candidates (fl/diff-entry->candidate entry "c1" "c2")]
      (is (= 1 (count candidates)))
      (let [c (first candidates)]
        (is (= :continues (:lineage-candidate/relation c)))
        (is (= :provisional (:lineage-candidate/status c)))
        (is (= "c1" (get-in c [:lineage-candidate/source :section/commit-oid])))
        (is (= "c2" (get-in c [:lineage-candidate/target :section/commit-oid])))
        (is (= "src/foo.md" (get-in c [:lineage-candidate/source :section/path-raw])))
        (is (= "src/foo.md" (get-in c [:lineage-candidate/target :section/path-raw])))
        (is (uuid? (:lineage-candidate/id c)))))))

(deftest diff-entry->candidate-add
  (testing "add entry produces possibly-derived-from candidate"
    (let [entry {:diff/change-type :add
                 :diff/old-path nil
                 :diff/new-path "new.md"
                 :diff/old-blob-oid nil
                 :diff/new-blob-oid "ccc"}
          candidates (fl/diff-entry->candidate entry "c1" "c2")]
      (is (= 1 (count candidates)))
      (is (= :possibly-derived-from (:lineage-candidate/relation (first candidates)))))))

(deftest diff-entry->candidate-delete
  (testing "delete entry produces no candidates"
    (let [entry {:diff/change-type :delete
                 :diff/old-path "gone.md"
                 :diff/new-path nil
                 :diff/old-blob-oid "ddd"
                 :diff/new-blob-oid nil}
          candidates (fl/diff-entry->candidate entry "c1" "c2")]
      (is (nil? candidates)))))

(deftest diff-entry->candidate-rename
  (testing "rename entry produces possibly-derived-from candidate"
    (let [entry {:diff/change-type :rename
                 :diff/old-path "old.md"
                 :diff/new-path "new.md"
                 :diff/old-blob-oid "eee"
                 :diff/new-blob-oid "eee"}
          candidates (fl/diff-entry->candidate entry "c1" "c2")]
      (is (= 1 (count candidates)))
      (let [c (first candidates)]
        (is (= :possibly-derived-from (:lineage-candidate/relation c)))
        (is (= "old.md" (get-in c [:lineage-candidate/source :section/path-raw])))
        (is (= "new.md" (get-in c [:lineage-candidate/target :section/path-raw])))))))

(deftest diff-entry->candidate-same-blob
  (testing "rename with same blob gets high confidence"
    (let [entry {:diff/change-type :rename
                 :diff/old-path "a.md"
                 :diff/new-path "b.md"
                 :diff/old-blob-oid "fff"
                 :diff/new-blob-oid "fff"}
          candidates (fl/diff-entry->candidate entry "c1" "c2")
          c (first candidates)]
      (is (>= (:lineage-candidate/confidence c) 0.9)))))

(deftest diff->file-candidates
  (testing "converts multiple entries"
    (let [entries [{:diff/change-type :modify :diff/old-path "a.md" :diff/new-path "a.md"
                   :diff/old-blob-oid "1" :diff/new-blob-oid "2"}
                  {:diff/change-type :delete :diff/old-path "b.md" :diff/new-path nil
                   :diff/old-blob-oid "3" :diff/new-blob-oid nil}
                  {:diff/change-type :add :diff/old-path nil :diff/new-path "c.md"
                   :diff/old-blob-oid nil :diff/new-blob-oid "4"}]
          candidates (fl/diff->file-candidates entries "c1" "c2")]
      ;; delete is skipped
      (is (= 2 (count candidates)))
      (is (every? #(= :provisional (:lineage-candidate/status %)) candidates)))))

(deftest generate-file-lineage-integration
  (testing "end-to-end: diff repo, generate candidates"
    (let [diff-result (git/diff-commits "." "HEAD~1" "HEAD")
          candidates (fl/generate-file-lineage diff-result)]
      (is (vector? candidates))
      (is (pos? (count candidates)))
      (is (every? #(= :provisional (:lineage-candidate/status %)) candidates))
      (is (every? #(= fl/file-lineage-version (:lineage-candidate/generator-version %)) candidates)))))

(deftest generate-file-lineage-failure
  (testing "returns nil on diff failure"
    (let [result {:failure {:failure/reason "test"} :entries []}
          candidates (fl/generate-file-lineage result)]
      (is (nil? candidates)))))
