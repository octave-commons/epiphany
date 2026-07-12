(ns epiphany.domain.section-extraction
  "Pure domain logic for extracting heading-delimited sections from a
  parsed Markdown block tree.

  A section is a heading block plus its immediate sibling blocks
  (everything between this heading and the next heading at the same
  or higher level). The heading path is the chain of ancestor heading
  texts from root to this section, encoding the document hierarchy.

  No I/O. No Git. No storage. Pure data transformation."
  (:require [epiphany.shape.markdown :as md]))

(defn- heading-text
  "Extract a plain-text string from heading inlines."
  [inlines]
  (->> inlines
       (map :inline/text)
       (apply str)
       clojure.string/trim))

(defn- make-section
  "Build a section map from a heading block, its body blocks, the
  heading path, and its ordinal among siblings."
  [heading-block body-blocks heading-path ordinal]
  (let [h-span (:block/span heading-block)
        body-start (if (seq body-blocks)
                     (:span/start-byte (:block/span (first body-blocks)))
                     (:span/end-byte h-span))
        body-end   (if (seq body-blocks)
                     (:span/end-byte (:block/span (last body-blocks)))
                     (:span/end-byte h-span))
        body-start-line (if (seq body-blocks)
                          (:span/start-line (:block/span (first body-blocks)))
                          (:span/end-line h-span))
        body-end-line   (if (seq body-blocks)
                          (:span/end-line (:block/span (last body-blocks)))
                          (:span/end-line h-span))]
    {:section/heading-path heading-path
     :section/level        (:heading/level heading-block)
     :section/ordinal      ordinal
     :section/heading-span h-span
     :section/body-span    {:span/start-byte body-start
                            :span/end-byte   body-end
                            :span/start-line body-start-line
                            :span/end-line   body-end-line}
     :section/body-blocks  body-blocks}))

(defn- classify-blocks
  "Walk the flat body vector, splitting into sections at each heading.
  Each heading becomes its own section; a section's body contains only
  the non-heading blocks between it and the next heading at any level.
  Returns a vector of {:heading block, :body [blocks]} maps."
  [blocks]
  (let [preamble (vec (take-while #(not= :heading (:block/type %)) blocks))
        sections (loop [remaining (drop-while #(not= :heading (:block/type %)) blocks)
                        acc []]
                   (if (empty? remaining)
                     acc
                     (let [heading (first remaining)
                           rest-after (rest remaining)
                           body (vec (take-while
                                      (fn [b]
                                        (not= :heading (:block/type b)))
                                      rest-after))
                           next-remaining (drop (count body) rest-after)]
                       (recur next-remaining
                              (conj acc {:heading heading :body body})))))]
    (if (seq preamble)
      (vec (cons {:heading nil :body preamble :preamble? true} sections))
      (vec sections))))

(defn- build-heading-paths
  "Given classified sections, assign heading paths based on heading hierarchy.
  The path-stack is kept root-first (oldest ancestor at index 0). Returns a
  vector of [heading-path section] pairs."
  [classified]
  (loop [remaining classified
         path-stack []    ;; root-first: [[level text] ...]
         acc []]
    (if (empty? remaining)
      acc
      (let [{:keys [heading preamble?]} (first remaining)
            rest-secs (rest remaining)]
        (if preamble?
          (recur rest-secs path-stack
                 (conj acc [[] (assoc (first remaining) :heading-path [])]))
          (let [level (:heading/level heading)
                text  (heading-text (:heading/inlines heading))
                ;; Pop younger siblings / descendants (level >= current) from
                ;; the end of the stack, keeping older ancestors.
                trimmed (loop [stack path-stack]
                          (if (and (seq stack) (>= (first (peek stack)) level))
                            (recur (pop stack))
                            stack))
                new-stack (conj trimmed [level text])
                full-path (vec (map second new-stack))
                sibling-count (count (filter (fn [[l _]] (= l level))
                                             path-stack))]
            (recur rest-secs new-stack
                   (conj acc [full-path
                              (assoc (first remaining)
                                     :heading-path full-path
                                     :ordinal sibling-count)]))))))))
(defn extract-sections
  "Extract heading-delimited sections from a parsed Markdown document."
  [parsed-doc]
  (let [blocks (:doc/body parsed-doc)
        classified (classify-blocks blocks)
        with-paths (build-heading-paths classified)]
    (mapv (fn [[heading-path {:keys [heading body _ordinal]}]]
            (make-section (or heading
                              {:block/type :heading
                               :heading/level 0
                               :heading/inlines []
                               :block/span {:span/start-byte 0
                                            :span/end-byte 0
                                            :span/start-line 1
                                            :span/end-line 1}})
                          body
                          heading-path
                          (or _ordinal 0)))
          with-paths)))

(defn section-content-hash
  "Compute a SHA-256 of the section body's raw content bytes."
  [blob section]
  (let [body-span (:section/body-span section)
        start (:span/start-byte body-span)
        end   (:span/end-byte body-span)
        raw   (subs blob start end)]
    (let [digest (.digest (java.security.MessageDigest/getInstance "SHA-256")
                          (.getBytes raw "UTF-8"))]
      (.encodeToString (java.util.Base64/getEncoder) digest))))

(defn make-extraction-record
  "Pure: construct an extraction observation map from extracted sections.
  Does not assign observation envelope — caller adds that."
  [sections revision-at-path-id commit-oid path-raw blob-oid blob extractor-version]
  (let [section-maps (mapv (fn [s]
                             {:section/heading-path            (:section/heading-path s)
                              :section/level                   (:section/level s)
                              :section/ordinal                 (:section/ordinal s)
                              :section/heading-span-start-byte (get-in s [:section/heading-span :span/start-byte])
                              :section/heading-span-end-byte   (get-in s [:section/heading-span :span/end-byte])
                              :section/body-span-start-byte    (get-in s [:section/body-span :span/start-byte])
                              :section/body-span-end-byte      (get-in s [:section/body-span :span/end-byte])
                              :section/body-span-start-line    (get-in s [:section/body-span :span/start-line])
                              :section/body-span-end-line      (get-in s [:section/body-span :span/end-line])})
                           sections)]
    {:extraction/revision-at-path-id revision-at-path-id
     :extraction/commit-oid          commit-oid
     :extraction/path-raw            path-raw
     :extraction/blob-oid            blob-oid
     :extraction/extractor-version   extractor-version
     :extraction/section-count       (count sections)
     :extraction/content-sha256      (section-content-hash blob (first sections))
     :extraction/sections            section-maps}))
