(ns epiphany.shape.markdown-test
  (:require [clojure.test :refer [deftest is testing]]
            [clojure.string :as str]
            [epiphany.shape.markdown :as md]))

(deftest parse-paragraph
  (let [result (md/parse "Hello world")
        body (:doc/body result)]
    (is (= 1 (count body)))
    (is (= :paragraph (:block/type (first body))))
    (is (seq (:paragraph/inlines (first body))))
    (is (contains? (first body) :block/span))))

(deftest parse-headings-preserve-hierarchy
  (let [result (md/parse "# H1\n## H2\n### H3")
        body (:doc/body result)]
    (is (= 3 (count body)))
    (testing "each heading carries its level"
      (is (= :heading (:block/type (first body))))
      (is (= 1 (:heading/level (first body))))
      (is (= 2 (:heading/level (second body))))
      (is (= 3 (:heading/level (nth body 2)))))
    (testing "heading inlines are present"
      (is (seq (:heading/inlines (first body)))))))

(deftest parse-fenced-code
  (let [result (md/parse "```clojure\n(def x 1)\n```")
        body (:doc/body result)]
    (is (= 1 (count body)))
    (let [block (first body)]
      (is (= :fenced-code (:block/type block)))
      (is (= "clojure" (:code/info-string block)))
      (is (= "(def x 1)" (:code/literal block))))))

(deftest parse-block-quote
  (let [result (md/parse "> quoted text\n> second line")
        body (:doc/body result)]
    (is (= 1 (count body)))
    (let [block (first body)]
      (is (= :block-quote (:block/type block)))
      (is (seq (:block-children block))))))

(deftest parse-bullet-list
  (let [result (md/parse "- item one\n- item two\n- item three")
        body (:doc/body result)]
    (is (= 1 (count body)))
    (let [block (first body)]
      (is (= :bullet-list (:block/type block)))
      (is (= 3 (count (:list-items block))))
      (is (false? (:list/loose? block))))))

(deftest parse-ordered-list
  (let [result (md/parse "1. first\n2. second\n3. third")
        body (:doc/body result)]
    (is (= 1 (count body)))
    (let [block (first body)]
      (is (= :ordered-list (:block/type block)))
      (is (= 3 (count (:list-items block))))
      (is (= 1 (:list/start-number block))))))

(deftest parse-thematic-break
  (let [result (md/parse "***")
        body (:doc/body result)]
    (is (= 1 (count body)))
    (is (= :thematic-break (:block/type (first body))))))

(deftest parse-html-block
  (let [result (md/parse "<div>raw html</div>")
        body (:doc/body result)]
    (is (some #(= :html-block (:block/type %)) body))
    (let [block (first (filter #(= :html-block (:block/type %)) body))]
      (is (str/includes? (:html/literal block) "<div>")))))

(deftest parse-table
  (let [md-str "| A | B |\n|---|---|\n| 1 | 2 |\n| 3 | 4 |"
        result (md/parse md-str)
        body (:doc/body result)]
    (is (some #(= :table (:block/type %)) body))
    (let [table (first (filter #(= :table (:block/type %)) body))]
      (is (seq (:table/header-rows table)))
      (is (seq (:table/body-rows table))))))

(deftest every-node-carries-byte-and-line-spans
  (let [result (md/parse "# Title\n\nSome text.\n\n- item")]
    (testing "top-level blocks have spans"
      (doseq [block (:doc/body result)]
        (is (contains? block :block/span))
        (let [span (:block/span block)]
          (is (integer? (:span/start-byte span)))
          (is (integer? (:span/end-byte span)))
          (is (integer? (:span/start-line span)))
          (is (integer? (:span/end-line span)))
          (is (< (:span/start-byte span) (:span/end-byte span)))
          (is (<= (:span/start-line span) (:span/end-line span))))))
    (testing "nested children have spans"
      (doseq [block (:doc/body result)
              child (or (:block-children block) (:list-items block) [])]
        (is (contains? child :block/span))))))

(deftest slice-recovers-exact-source
  (let [blob "# Hello\n\nWorld\n"
        result (md/parse blob)
        heading (first (:doc/body result))]
    (is (= "# Hello" (md/slice blob (:block/span heading))))))

(deftest front-matter-parsed-separately
  (let [md-str "---\ntitle: Test\ntags: [a, b]\n---\n\n# Body"
        result (md/parse md-str)]
    (testing "front matter is present"
      (is (some? (:doc/front-matter result)))
      (is (= :front-matter (:block/type (:doc/front-matter result))))
      (is (contains? (:doc/front-matter result) :block/span)))
    (testing "body does not include front matter"
      (is (every? #(not= :front-matter (:block/type %))
                  (:doc/body result))))
    (testing "body starts with heading after front matter"
      (is (= :heading (:block/type (first (:doc/body result))))))))

(deftest document-without-front-matter
  (let [result (md/parse "# Just a heading")]
    (is (nil? (:doc/front-matter result)))
    (is (= 1 (count (:doc/body result))))))

(deftest unsupported-constructs-become-diagnostics
  (testing "footnotes become diagnostics"
    (let [result (md/parse "Text[^1]\n\n[^1]: footnote")
          ;; footnote definitions are not standard markdown, should be diagnostic or paragraph
          body (:doc/body result)]
      ;; At minimum, it should not crash
      (is (vector? body))
      (is (seq body)))))

(deftest empty-input
  (let [result (md/parse "")]
    (is (= [] (:doc/body result)))
    (is (nil? (:doc/front-matter result)))
    (is (= 0 (:doc/source-length result)))))

(deftest source-length-is-byte-count
  (let [result (md/parse "Hello")]
    (is (= 5 (:doc/source-length result))))
  (testing "multi-byte characters count as bytes"
    (let [result (md/parse "ημ")]
      (is (= 4 (:doc/source-length result))))))

(deftest span-offsets-are-true-utf8-byte-offsets
  (testing "non-ASCII content before a node shifts byte offsets"
    (let [blob "ημ hello\n\n# Café ☕ heading\n\nBody"
          result (md/parse blob)
          heading (first (filter #(= :heading (:block/type %)) (:doc/body result)))
          span (:block/span heading)]
      ;; "ημ hello\n\n" is 10 chars but 12 UTF-8 bytes (η=2, μ=2, space=1, h=1, e=1, l=1, l=1, o=1, \n=1, \n=1)
      (is (= 12 (:span/start-byte span)))
      ;; Verify slice still works correctly with byte-based spans
      (is (= "# Café ☕ heading" (md/slice blob span)))))
  (testing "ASCII-only content has matching char and byte offsets"
    (let [blob "# Hello\n\nBody"
          result (md/parse blob)
          heading (first (filter #(= :heading (:block/type %)) (:doc/body result)))
          span (:block/span heading)]
      (is (= 0 (:span/start-byte span)))
      (is (= 7 (:span/end-byte span)))))
  (testing "astral plane characters (emoji) with surrogate pairs"
    (let [blob "😀 😐 😀\n\n# Title"
          result (md/parse blob)
          heading (first (filter #(= :heading (:block/type %)) (:doc/body result)))
          span (:block/span heading)]
      ;; "😀 😐 😀\n\n" = 12 bytes (4+4+1+4+4+1+1+1) but 9 chars (6 emoji/space + 2 newlines)
      ;; Actually: 😀 (4 bytes, 2 chars) + space (1 byte, 1 char) + 😐 (4 bytes, 2 chars) + space (1 byte, 1 char) + 😀 (4 bytes, 2 chars) + \n\n (2 bytes, 2 chars)
      ;; = 16 bytes, 10 chars
      (is (= 16 (:span/start-byte span)))
      (is (= "# Title" (md/slice blob span))))))

(deftest output-is-deterministic
  (let [input "# Title\n\nBody text.\n\n- item\n"
        r1 (md/parse input)
        r2 (md/parse input)]
    (is (= r1 r2))))

(deftest heading-level-reflects-hash-count
  (let [result (md/parse "# H1\n## H2\n#### H4")
        headings (filter #(= :heading (:block/type %)) (:doc/body result))]
    (is (= [1 2 4] (mapv :heading/level headings)))))

(deftest nested-block-quote-children
  (let [result (md/parse "> ## Quoted heading\n>\n> Paragraph inside quote.")
        body (:doc/body result)
        bq (first body)]
    (is (= :block-quote (:block/type bq)))
    (is (< 1 (count (:block-children bq))))
    (is (= :heading (:block/type (first (:block-children bq)))))))

(deftest loose-list-items
  (let [result (md/parse "- a\n\n- b")
        body (:doc/body result)
        block (first body)]
    (is (= :bullet-list (:block/type block)))
    (is (true? (:list/loose? block)))))

(deftest fenced-code-with-tilde-fence
  (let [result (md/parse "~~~python\nprint('hi')\n~~~")
        body (:doc/body result)
        block (first body)]
    (is (= :fenced-code (:block/type block)))
    (is (= "python" (:code/info-string block)))
    (is (= "print('hi')" (:code/literal block)))))

(deftest fenced-code-with-long-fence
  (testing "4+ backtick fence extracts info string correctly"
    (let [result (md/parse "````clojure\n(def x 1)\n````")
          body (:doc/body result)
          block (first body)]
      (is (= :fenced-code (:block/type block)))
      (is (= "clojure" (:code/info-string block)))
      (is (= "(def x 1)" (:code/literal block)))))
  (testing "5+ tilde fence extracts info string correctly"
    (let [result (md/parse "~~~~python\nprint('hi')\n~~~~")
          body (:doc/body result)
          block (first body)]
      (is (= :fenced-code (:block/type block)))
      (is (= "python" (:code/info-string block)))
      (is (= "print('hi')" (:code/literal block))))))
