(ns epiphany.domain.diff-test
  (:require [clojure.test :refer [deftest testing is]]
            [epiphany.domain.diff :as diff]
            [epiphany.domain.evidence :as evidence]))

;; ---------------------------------------------------------------------------
;; Mock port

(defn- make-mock-git-port
  [blobs commits]
  {:read-blob (fn [_ oid]
                (if-let [content (get blobs oid)]
                  {:blob/oid oid
                   :blob/content content
                   :blob/size (count content)
                   :blob/failure nil}
                  {:blob/oid oid
                   :blob/content nil
                   :blob/size 0
                   :blob/failure {:failure/oid oid
                                  :failure/reason "blob-not-found"
                                  :failure/message (str "Not found: " oid)}}))
   :commit-tree-entries (fn [_ commit-oid]
                          {:commit-oid commit-oid
                           :entries (get commits commit-oid [])
                           :failure nil})})

;; ---------------------------------------------------------------------------
;; compute-diff

(deftest compute-diff-identical
  (testing "identical content produces only equal entries"
    (let [content "# Title\n\nSome text.\n"
          result (diff/compute-diff content content)]
      (is (every? #(= :equal (:type %)) result))
      (is (= 3 (count result))))))

(deftest compute-diff-empty
  (testing "empty strings produce single equal entry (empty line)"
    (is (= 1 (count (diff/compute-diff "" ""))))))

(deftest compute-diff-insertions
  (testing "new lines appear as inserts"
    (let [left "line1\nline2\n"
          right "line1\nline2\nline3\n"
          result (diff/compute-diff left right)]
      (is (some #(= :insert (:type %)) result))
      (is (every? #(not= :delete (:type %)) result)))))

(deftest compute-diff-deletions
  (testing "removed lines appear as deletes"
    (let [left "line1\nline2\nline3\n"
          right "line1\nline3\n"
          result (diff/compute-diff left right)]
      (is (some #(= :equal (:type %)) result))
      (is (some #(= :delete (:type %)) result))
      (is (= "line1" (:content (first (filter #(= :equal (:type %)) result))))))))

(deftest compute-diff-mixed
  (testing "mixed changes produce correct diff"
    (let [left "line1\nline2\nline3\n"
          right "line1\nline2a\nline3\nline4\n"
          result (diff/compute-diff left right)]
      (is (some #(= :equal (:type %)) result))
      (is (some #(= :delete (:type %)) result))
      (is (some #(= :insert (:type %)) result)))))

;; ---------------------------------------------------------------------------
;; format-diff-text

(deftest format-diff-text-basic
  (testing "formats diff with headers"
    (let [diff [{:type :equal :line-a 1 :line-b 1 :content "hello"}
                {:type :insert :line-a nil :line-b 2 :content "world"}]
          result (diff/format-diff-text diff "=== left ===" "=== right ===")]
      (is (.contains result "=== left ==="))
      (is (.contains result "=== right ==="))
      (is (.contains result "  hello"))
      (is (.contains result "+ world")))))

;; ---------------------------------------------------------------------------
;; compare-evidence

(deftest compare-evidence-identical
  (testing "comparing identical content produces empty diff"
    (let [content "# Title\n\nSame text.\n"
          port (make-mock-git-port {"blob1" content}
                                   {"commit1" [{:git/path "doc.md" :git/blob-oid "blob1"}]
                                    "commit2" [{:git/path "doc.md" :git/blob-oid "blob1"}]})
          ports {:git port}
          result (diff/compare-evidence ports
                                        {:left {:path "doc.md" :heading ["Title"] :commit-oid "commit1"}
                                         :right {:path "doc.md" :heading ["Title"] :commit-oid "commit2"}})]
      (is (nil? (:diff/failure result)))
      (is (= 0 (get-in result [:diff/summary :insert])))
      (is (= 0 (get-in result [:diff/summary :delete])))
      (is (true? (get-in result [:diff/continuity :same-path])))
      (is (true? (get-in result [:diff/continuity :same-heading]))))))

(deftest compare-evidence-different-content
  (testing "comparing different content produces diff lines"
    (let [port (make-mock-git-port {"blob1" "# Title\n\nOld text.\n"
                                    "blob2" "# Title\n\nNew text.\n"}
                                   {"commit1" [{:git/path "doc.md" :git/blob-oid "blob1"}]
                                    "commit2" [{:git/path "doc.md" :git/blob-oid "blob2"}]})
          ports {:git port}
          result (diff/compare-evidence ports
                                        {:left {:path "doc.md" :heading ["Title"] :commit-oid "commit1"}
                                         :right {:path "doc.md" :heading ["Title"] :commit-oid "commit2"}})]
      (is (nil? (:diff/failure result)))
      (is (pos? (+ (get-in result [:diff/summary :delete])
                   (get-in result [:diff/summary :insert])))))))

(deftest compare-evidence-unavailable-side
  (testing "unavailable side produces failure"
    (let [port (make-mock-git-port {} {})
          ports {:git port}
          result (diff/compare-evidence ports
                                        {:left {:path "doc.md" :heading [] :commit-oid "commit1"}
                                         :right {:path "doc.md" :heading [] :commit-oid "commit2"}})]
      (is (some? (:diff/failure result)))
      (is (= [] (:diff/lines result))))))
