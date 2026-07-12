(ns epiphany.domain.evidence-test
  (:require [clojure.test :refer [deftest testing is]]
            [epiphany.domain.evidence :as ev]))

;; ---------------------------------------------------------------------------
;; parse-section-expression

(deftest parse-section-expression-path-only
  (testing "simple path"
    (is (= {:path "docs/notes/foo.md"
            :heading []
            :commit-oid nil}
           (ev/parse-section-expression "docs/notes/foo.md")))))

(deftest parse-section-expression-with-heading
  (testing "path with heading"
    (is (= {:path "docs/notes/foo.md"
            :heading ["Architecture"]
            :commit-oid nil}
           (ev/parse-section-expression "docs/notes/foo.md#Architecture")))))

(deftest parse-section-expression-with-nested-heading
  (testing "path with nested heading"
    (is (= {:path "docs/notes/foo.md"
            :heading ["Architecture" "Design"]
            :commit-oid nil}
           (ev/parse-section-expression "docs/notes/foo.md#Architecture>Design")))))

(deftest parse-section-expression-with-commit
  (testing "path with commit"
    (is (= {:path "docs/notes/foo.md"
            :heading []
            :commit-oid "abc123"}
           (ev/parse-section-expression "docs/notes/foo.md@abc123")))))

(deftest parse-section-expression-full
  (testing "path with heading and commit"
    (is (= {:path "docs/notes/foo.md"
            :heading ["Architecture" "Design"]
            :commit-oid "abc123"}
           (ev/parse-section-expression "docs/notes/foo.md#Architecture>Design@abc123")))))

(deftest parse-section-expression-no-heading
  (testing "path with commit but no heading"
    (is (= {:path "docs/notes/foo.md"
            :heading []
            :commit-oid "abc123"}
           (ev/parse-section-expression "docs/notes/foo.md#@abc123")))))

;; ---------------------------------------------------------------------------
;; find-section-in-content (via retrieve-evidence with in-memory port)

(defn- make-mock-git-port
  "Create a mock git port for testing."
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

(deftest retrieve-evidence-simple
  (testing "retrieve evidence from blob"
    (let [content "# Title\n\nSome text here.\n\n## Section\n\nSection content.\n"
          port (make-mock-git-port {"blob123" content}
                                   {"commit1" [{:git/path "docs/notes/foo.md"
                                                :git/blob-oid "blob123"}]})
          ports {:git port}]
      (let [result (ev/retrieve-evidence ports {:path "docs/notes/foo.md"
                                                 :heading ["Title"]
                                                 :commit-oid "commit1"})]
        (is (= "docs/notes/foo.md" (:evidence/path result)))
        (is (= "commit1" (:evidence/commit-oid result)))
        (is (= ["Title"] (:evidence/heading-path result)))
        (is (.contains (:evidence/source result) "# Title"))
        (is (= 1 (:evidence/start-line result)))
        (is (pos-int? (:evidence/end-line result)))
        (is (= false (:evidence/unavailable result)))
        (is (nil? (:evidence/failure result)))))))

(deftest retrieve-evidence-with-subsection
  (testing "retrieve evidence for a subsection"
    (let [content "# Title\n\nSome text.\n\n## Section\n\nSection content.\n"
          port (make-mock-git-port {"blob123" content}
                                   {"commit1" [{:git/path "doc.md"
                                                :git/blob-oid "blob123"}]})
          ports {:git port}]
      (let [result (ev/retrieve-evidence ports {:path "doc.md"
                                                 :heading ["Section"]
                                                 :commit-oid "commit1"})]
        (is (= "doc.md" (:evidence/path result)))
        (is (= "commit1" (:evidence/commit-oid result)))
        (is (some? (:evidence/source result)))
        (is (= false (:evidence/unavailable result)))))))

(deftest retrieve-evidence-missing-path
  (testing "path not found in commit"
    (let [port (make-mock-git-port {}
                                   {"commit1" [{:git/path "other.md"
                                                :git/blob-oid "blob1"}]})
          ports {:git port}]
      (let [result (ev/retrieve-evidence ports {:path "missing.md"
                                                 :heading []
                                                 :commit-oid "commit1"})]
        (is (= true (:evidence/unavailable result)))
        (is (= "path-not-found" (get-in result [:evidence/failure :failure/reason])))))))

(deftest retrieve-evidence-missing-commit
  (testing "commit not reachable"
    (let [port (make-mock-git-port {} {})
          ports {:git port}]
      (let [result (ev/retrieve-evidence ports {:path "doc.md"
                                                 :heading []
                                                 :commit-oid "nonexistent"})]
        (is (= true (:evidence/unavailable result)))
        (is (= "path-not-found" (get-in result [:evidence/failure :failure/reason])))))))

(deftest retrieve-evidence-no-commit-oid
  (testing "missing commit-oid"
    (let [port (make-mock-git-port {} {})
          ports {:git port}]
      (let [result (ev/retrieve-evidence ports {:path "doc.md"
                                                 :heading []
                                                 :commit-oid nil})]
        (is (= true (:evidence/unavailable result)))
        (is (= "commit-required" (get-in result [:evidence/failure :failure/reason])))))))

(deftest retrieve-evidence-no-git-port
  (testing "git port missing"
    (let [result (ev/retrieve-evidence {:git {}} {:path "doc.md"
                                                    :heading []
                                                    :commit-oid "abc"})]
      (is (= true (:evidence/unavailable result)))
      (is (= "port-missing" (get-in result [:evidence/failure :failure/reason]))))))

(deftest retrieve-evidence-heading-not-found
  (testing "heading not found in blob content"
    (let [content "# Title\n\nSome text.\n"
          port (make-mock-git-port {"blob123" content}
                                   {"commit1" [{:git/path "doc.md"
                                                :git/blob-oid "blob123"}]})
          ports {:git port}]
      (let [result (ev/retrieve-evidence ports {:path "doc.md"
                                                 :heading ["Nonexistent"]
                                                 :commit-oid "commit1"})]
        (is (= false (:evidence/unavailable result)))
        (is (= "heading-not-found" (get-in result [:evidence/failure :failure/reason])))
        (is (= content (:evidence/source result)))))))

;; ---------------------------------------------------------------------------
;; format-evidence-text

(deftest format-evidence-text-available
  (testing "format available evidence"
    (let [result (ev/format-evidence-text {:evidence/path "doc.md"
                                            :evidence/commit-oid "abc123"
                                            :evidence/heading-path ["Title"]
                                            :evidence/source "# Title"
                                            :evidence/start-line 1
                                            :evidence/end-line 2
                                            :evidence/blob-size 42
                                            :evidence/failure nil
                                            :evidence/unavailable false})]
      (is (.contains result "doc.md"))
      (is (.contains result "abc123"))
      (is (.contains result "Title"))
      (is (.contains result "# Title")))))

(deftest format-evidence-text-unavailable
  (testing "format unavailable evidence"
    (let [result (ev/format-evidence-text {:evidence/path "doc.md"
                                            :evidence/commit-oid "abc123"
                                            :evidence/heading-path []
                                            :evidence/source nil
                                            :evidence/failure {:failure/reason "path-not-found"
                                                              :failure/message "Not found"}
                                            :evidence/unavailable true})]
      (is (.contains result "UNAVAILABLE"))
      (is (.contains result "Not found")))))
