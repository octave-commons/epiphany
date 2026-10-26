(ns epiphany.domain.resource-extraction
  "Typed historical resources share the existing append-only extraction port.")

(defn make-record
  "Retain structured/conversation payloads; lexical sections are rebuildable views."
  [representation revision extractor-version]
  (let [span (:resource/source-span representation)
        sections (if (= :parsed (:resource/status representation))
                   [{:section/heading-path [(name (:resource/format representation))]
                     :section/level 0 :section/ordinal 0
                     :section/heading-span-start-byte 0 :section/heading-span-end-byte 0
                     :section/body-span-start-byte (:span/start-byte span)
                     :section/body-span-end-byte (:span/end-byte span)
                     :section/body-span-start-line (:span/start-line span)
                     :section/body-span-end-line (:span/end-line span)}] [])]
    {:extraction/revision-at-path-id (:revision-at-path/id revision)
     :extraction/commit-oid (:revision/commit-oid revision)
     :extraction/path-raw (:revision/path-raw revision)
     :extraction/blob-oid (:revision/blob-oid revision)
     :extraction/extractor-version extractor-version
     :extraction/section-count (count sections)
     :extraction/content-sha256 (:resource/source-sha256 representation)
     :extraction/sections sections
     :extraction/resource (assoc representation :resource/provenance
                                 {:resource-id (:resource-id revision)
                                  :source/commit-oid (:revision/commit-oid revision)
                                  :source/path-raw (:revision/path-raw revision)
                                  :source/blob-oid (:revision/blob-oid revision)})}))
