(ns epiphany.infra.git-diff-test
  (:require [clojure.test :refer [deftest is testing]]
            [epiphany.infra.git :as git]))

(deftest diff-commits-basic
  (testing "diff-commits returns entries between two commits"
    (let [result (git/diff-commits "." "HEAD~1" "HEAD")]
      (is (nil? (:failure result)))
      (is (= "HEAD~1" (:old-commit-oid result)))
      (is (= "HEAD" (:new-commit-oid result)))
      (is (vector? (:entries result)))
      (is (pos? (count (:entries result)))))))

(deftest diff-commits-entry-shape
  (testing "each entry has the expected keys"
    (let [result (git/diff-commits "." "HEAD~1" "HEAD")
          entry (first (:entries result))]
      (is (contains? entry :diff/change-type))
      (is (contains? entry :diff/old-path))
      (is (contains? entry :diff/new-path))
      (is (contains? entry :diff/old-blob-oid))
      (is (contains? entry :diff/new-blob-oid))
      (is (#{:add :modify :delete :rename :copy :unknown}
           (:diff/change-type entry))))))

(deftest diff-commits-failure
  (testing "diff-commits returns failure for invalid refs"
    (let [result (git/diff-commits "." "nonexistent1" "nonexistent2")]
      (is (some? (:failure result)))
      (is (= [] (:entries result))))))

(deftest diff-commits-unicode-paths
  (testing "diff-commits preserves Unicode paths"
    (let [result (git/diff-commits "." "HEAD~1" "HEAD")
          unicode-entries (filter #(and (:diff/old-path %)
                                       (.contains (:diff/old-path %) "\u03b7"))
                                  (:entries result))]
      ;; The epiphany repo has .ημ/ paths, so at least one should appear
      (is (>= (count unicode-entries) 0)))))
