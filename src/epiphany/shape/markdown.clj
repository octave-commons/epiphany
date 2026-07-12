(ns epiphany.shape.markdown
  "Parse a Markdown string into a typed block tree with source spans.

  Uses flexmark-java. Headings preserve hierarchy; paragraphs, lists,
  tables, quotes, fenced code, thematic breaks, and HTML blocks are
  typed. Unsupported constructs become diagnostics, not crashes. Front
  matter is parsed separately from the body. Every node carries byte
  and line spans; the exact raw source slice is recoverable from
  blob + span.

  Chose flexmark over commonmark-java because:
  - Native source span tracking via Node.getStartOffset/getEndOffset
  - Built-in YAML front matter extension
  - Table extension with source positions
  - Visitor API with AST node source spans
  - Well-maintained, 0.64.x stable"
  (:require [clojure.string :as str])
  (:import [com.vladsch.flexmark.ext.tables TablesExtension TableHead TableBody]
           [com.vladsch.flexmark.ext.yaml.front.matter YamlFrontMatterBlock
            YamlFrontMatterExtension]
           [com.vladsch.flexmark.parser Parser]
           [com.vladsch.flexmark.util.ast Block Document Node]
           [com.vladsch.flexmark.util.sequence BasedSequence]
           [java.util Arrays]))

(defn- ext-seq [exts]
  (Arrays/asList (to-array exts)))

(defn make-parser
  "Create a flexmark Parser with extensions."
  []
  (let [extensions (ext-seq [(YamlFrontMatterExtension/create)
                             (TablesExtension/create)])]
    (-> (Parser/builder)
        (.extensions extensions)
        (.build))))

(defn- char-offset->byte-offset
  "Convert a UTF-16 char offset to a UTF-8 byte offset."
  [^String s char-offset]
  (let [truncated (subs s 0 (min char-offset (count s)))]
    (count (.getBytes truncated "UTF-8"))))

(defn- span-of
  "Extract byte/line span from a flexmark Node."
  [^Node node]
  (let [node-start (.getStartOffset node)
        node-end (.getEndOffset node)
        doc (.getDocument node)
        ^BasedSequence doc-text (.getBaseSequence doc)
        full-text (str doc-text)
        before (subs full-text 0 (min node-start (count full-text)))
        node-text (subs full-text
                       (min node-start (count full-text))
                       (min node-end (count full-text)))
        start-line (count (re-seq #"\n" before))
        end-line (+ start-line (count (re-seq #"\n" node-text)))]
    {:span/start-byte (char-offset->byte-offset full-text node-start)
     :span/end-byte (char-offset->byte-offset full-text node-end)
     :span/start-line start-line
     :span/end-line end-line}))

(defn- text-content
  "Extract the plain text content of a node."
  [^Node node]
  (str (.getChars node)))

(defn- inline->map
  "Convert an inline node to a simple map."
  [^Node node]
  {:inline/type (keyword (.getSimpleName (.getClass node)))
   :inline/text (text-content node)
   :inline/span (span-of node)})

(defn- inlines-of
  "Collect non-Block children as inlines."
  [^Node node]
  (mapv inline->map
        (remove #(instance? Block %) (iterator-seq (.getChildIterator node)))))

(declare block->map)
(declare children->blocks)

(defn- heading->map [^com.vladsch.flexmark.ast.Heading node]
  {:block/type :heading
   :heading/level (.getLevel node)
   :heading/inlines (inlines-of node)
   :block/span (span-of node)})

(defn- paragraph->map [^Node node]
  {:block/type :paragraph
   :paragraph/inlines (inlines-of node)
   :block/span (span-of node)})

(defn- fenced-code->map [^Node node]
  (let [raw (text-content node)
        first-line (first (str/split-lines raw))
        fence-length (or (when-let [m (re-matches #"^(`{3,}).*$" first-line)]
                           (count (second m)))
                         (when-let [m (re-matches #"^(~{3,}).*$" first-line)]
                           (count (second m)))
                         0)
        info-str (if (pos? fence-length)
                   (subs first-line fence-length)
                   "")]
    {:block/type :fenced-code
     :code/info-string (str/trim info-str)
     :code/literal (let [lines (str/split-lines raw)
                         inner (drop 1 (butlast lines))]
                     (str/join "\n" inner))
     :block/span (span-of node)}))

(defn- block-quote->map [^Node node]
  {:block/type :block-quote
   :block-children (children->blocks node)
   :block/span (span-of node)})

(defn- bullet-list->map [^Node node]
  (let [items (filter #(instance? com.vladsch.flexmark.ast.BulletListItem %)
                      (iterator-seq (.getChildIterator node)))]
    {:block/type :bullet-list
     :list/loose? (.isLoose node)
     :list-items (mapv (fn [item]
                         {:block/type :list-item
                          :block-children (children->blocks item)
                          :block/span (span-of item)})
                       items)
     :block/span (span-of node)}))

(defn- ordered-list->map [^Node node]
  (let [items (filter #(instance? com.vladsch.flexmark.ast.OrderedListItem %)
                      (iterator-seq (.getChildIterator node)))
        parse-marker (fn [^com.vladsch.flexmark.ast.OrderedListItem item]
                       (let [marker (str (.getOpeningMarker item))
                             cleaned (clojure.string/replace marker #"[^0-9]" "")]
                         (when (seq cleaned)
                           (Integer/parseInt cleaned))))]
    {:block/type :ordered-list
     :list/loose? (.isLoose node)
     :list/start-number (when-let [first-item (first items)]
                           (parse-marker first-item))
     :list-items (mapv (fn [^com.vladsch.flexmark.ast.OrderedListItem item]
                         {:block/type :list-item
                          :item/number (parse-marker item)
                          :block-children (children->blocks item)
                          :block/span (span-of item)})
                       items)
     :block/span (span-of node)}))

(defn- table->map [^Node node]
  (let [children (iterator-seq (.getChildIterator node))
        head-children (filter #(instance? TableHead %) children)
        body-children (filter #(instance? TableBody %) children)
        row->cells (fn [row]
                     (mapv (fn [cell] (inline->map cell))
                           (iterator-seq (.getChildIterator row))))]
    {:block/type :table
     :table/header-rows (mapv row->cells head-children)
     :table/body-rows (mapv row->cells body-children)
     :block/span (span-of node)}))

(defn- html-block->map [^Node node]
  {:block/type :html-block
   :html/literal (text-content node)
   :block/span (span-of node)})

(defn- thematic-break->map [^Node node]
  {:block/type :thematic-break
   :block/span (span-of node)})

(defn- unsupported->diagnostic [^Node node]
  {:block/type :diagnostic
   :diagnostic/unsupported-type (.getSimpleName (.getClass node))
   :diagnostic/raw (text-content node)
   :block/span (span-of node)})

(defn- block->map
  "Convert a single block node to a typed map."
  [^Node node]
  (condp instance? node
    com.vladsch.flexmark.ext.yaml.front.matter.YamlFrontMatterBlock
    {:block/type :front-matter
     :block/raw (text-content node)
     :block/span (span-of node)}

    com.vladsch.flexmark.ast.Heading
    (heading->map node)

    com.vladsch.flexmark.ast.Paragraph
    (paragraph->map node)

    com.vladsch.flexmark.ast.FencedCodeBlock
    (fenced-code->map node)

    com.vladsch.flexmark.ast.BlockQuote
    (block-quote->map node)

    com.vladsch.flexmark.ast.BulletList
    (bullet-list->map node)

    com.vladsch.flexmark.ast.OrderedList
    (ordered-list->map node)

    com.vladsch.flexmark.ext.tables.TableBlock
    (table->map node)

    com.vladsch.flexmark.ast.ThematicBreak
    (thematic-break->map node)

    com.vladsch.flexmark.ast.HtmlBlock
    (html-block->map node)

    ;; Unsupported → diagnostic
    (unsupported->diagnostic node)))

(defn- children->blocks
  "Convert all child block nodes of a parent to typed maps."
  [^Node parent]
  (vec (keep (fn [^Node child]
               (when (instance? Block child)
                 (block->map child)))
             (iterator-seq (.getChildIterator parent)))))

(defn- find-front-matter
  "Find the YamlFrontMatterBlock among the document's children, if any."
  [^Document doc]
  (some (fn [^Node child]
          (when (instance? YamlFrontMatterBlock child)
            child))
        (iterator-seq (.getChildIterator doc))))

(defn parse
  "Parse a Markdown string into a typed block tree.

  Returns a map:
    :doc/front-matter  — optional front matter block map
    :doc/body          — vector of typed block maps
    :doc/source-length — byte length of the input string"
  ([source] (parse source (make-parser)))
  ([^String source ^Parser parser]
   (let [doc (.parse parser source)
         all-blocks (children->blocks doc)
         front-matter-node (find-front-matter doc)
         body-blocks (vec (remove #(= :front-matter (:block/type %)) all-blocks))]
     {:doc/front-matter (when front-matter-node
                          {:block/type :front-matter
                           :block/raw (text-content front-matter-node)
                           :block/span (span-of front-matter-node)})
      :doc/body body-blocks
      :doc/source-length (count (.getBytes ^String source "UTF-8"))})))

(defn- byte-offset->char-offset
  "Convert a UTF-8 byte offset to a UTF-16 char offset.
  4-byte UTF-8 sequences (astral plane) map to surrogate pairs (2 chars)."
  [^String s byte-offset]
  (let [bytes (.getBytes s "UTF-8")]
    (loop [i 0 char-count 0]
      (if (or (>= i byte-offset) (>= i (alength bytes)))
        char-count
        (let [b (aget bytes i)
              byte-width (cond
                           (zero? (bit-and b 0x80)) 1   ; 1-byte char
                           (zero? (bit-and b 0x20)) 2   ; 2-byte char
                           (zero? (bit-and b 0x10)) 3   ; 3-byte char
                           :else 4)                     ; 4-byte char
              char-width (if (= byte-width 4) 2 1)]     ; astral = surrogate pair
          (recur (+ i byte-width) (+ char-count char-width)))))))

(defn slice
  "Recover the exact raw source text for a node given the original blob."
  [^String blob span]
  (let [start-char (byte-offset->char-offset blob (:span/start-byte span))
        end-char (byte-offset->char-offset blob (:span/end-byte span))]
    (subs blob start-char end-char)))
