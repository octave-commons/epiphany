(ns epiphany.law-suite.generators
  "test.check generators for observation records (ENG-017I).

  Hand-curated over malli.generator defaults for the Git-evidence types
  (OIDs, insts, enums) per the card's own risk note: structural fields
  vary freely; identity/evidence fields vary within their legal
  alphabets. Every generator produces records that PASS the registered
  schema — validity is checked by the mutation properties (a mutation
  must flip acceptance to rejection)."
  (:require [clojure.string :as string]
            [clojure.test.check.generators :as gen]))

;; ---------------------------------------------------------------------------
;; Atoms

(def gen-uuid
  "A UUID derived from test.check's PRNG (never the global RNG) so seeds
   replay exactly."
  (gen/fmap (fn [[msb lsb]] (java.util.UUID. ^long msb ^long lsb))
            (gen/tuple (gen/choose -9223372036854775808 9223372036854775807)
                       (gen/choose -9223372036854775808 9223372036854775807))))

(def gen-oid
  (gen/fmap (fn [chars] (apply str chars))
            (gen/vector (gen/elements "0123456789abcdef") 40)))

(def gen-inst
  (gen/fmap (fn [ms] (java.util.Date. ^long ms))
            (gen/choose 1577836800000 1798761600000)))

(def gen-path
  (gen/fmap (fn [segments] (str "/" (string/join "/" segments)))
            (gen/vector (gen/elements ["docs" "notes" "src" ".ημ" "research"
                                       "a.md" "b.md" "x.md" "Ελληνικά.md"])
                        1 4)))

(def gen-observed-path
  (gen/fmap (fn [p] {:path/raw p
                     :path/source :filesystem-argument
                     :path/comparison :exact})
            gen-path))

;; ---------------------------------------------------------------------------
;; Records (one generator per registered write op's record kind)

(defn- envelope
  [m]
  (merge m {:observation/observed-at #inst "2026-01-01T00:00:00.000Z"
            :observation/adapter-version "gen-v1"
            :observation/schema-version 1
            :resource-id #uuid "aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeeee"}))

(def gen-repository-location
  (gen/fmap (fn [[rid oid p]]
              (envelope {:observation/id oid
                         :observation/request-id rid
                         :observation/type :repository/location-observed
                         :repository/path {:path/raw p
                                           :path/source :filesystem-argument
                                           :path/comparison :exact}
                         :repository/common-git-dir {:path/raw (str p "/.git")
                                                     :path/source :filesystem-argument
                                                     :path/comparison :exact}}))
            (gen/tuple gen-uuid gen-uuid gen-path)))

(def gen-revision-at-path
  (gen/fmap (fn [[oid1 oid2 p evidence id1 id2]]
              (envelope {:observation/id id1
                         :observation/type :revision/at-path-observed
                         :revision-at-path/id id2
                         :revision/commit-oid oid1
                         :revision/tree-oid oid1
                         :revision/path-raw p
                         :revision/blob-oid oid2
                         :revision/mode 33188
                         :revision/evidence evidence}))
            (gen/tuple gen-oid gen-oid gen-path
                       (gen/elements [:initial :add :modify :continuity])
                       gen-uuid gen-uuid)))

(def gen-ingestion-run
  (gen/fmap (fn [[rid p refs n id1]]
              (envelope {:observation/id id1
                         :observation/request-id rid
                         :observation/type :ingestion/run-completed
                         :ingestion/repo-path {:path/raw p
                                               :path/source :filesystem-argument
                                               :path/comparison :exact}
                         :ingestion/selected-refs refs
                         :ingestion/commit-count n
                         :ingestion/failure-count 0
                         :ingestion/failures []}))
            (gen/tuple gen-uuid gen-path
                       (gen/vector (gen/elements ["HEAD" "refs/heads/main"]) 1 2)
                       (gen/choose 0 500)
                       gen-uuid)))

(def gen-checkpoint
  (gen/fmap (fn [[n status id1 id2]]
              (envelope {:observation/id id1
                         :observation/type :projection/checkpoint-recorded
                         :checkpoint/projection-name "gen-projection"
                         :checkpoint/projection-version 1
                         :checkpoint/ingestion-run-id id2
                         :checkpoint/status status
                         :checkpoint/processed-count n}))
            (gen/tuple (gen/choose 0 1000)
                       (gen/elements [:running :completed])
                       gen-uuid gen-uuid)))

(def gen-section-extraction
  (gen/fmap (fn [[oid1 oid2 p id1 id2]]
              (envelope {:observation/id id1
                         :observation/type :section/extraction-completed
                         :extraction/revision-at-path-id id2
                         :extraction/commit-oid oid1
                         :extraction/path-raw p
                         :extraction/blob-oid oid2
                         :extraction/extractor-version "gen-extractor-v1"
                         :extraction/section-count 1
                         :extraction/content-sha256 "genhash"
                         :extraction/sections [{:section/heading-path ["Gen"]
                                                :section/level 1
                                                :section/ordinal 0
                                                :section/heading-span-start-byte 0
                                                :section/heading-span-end-byte 5
                                                :section/body-span-start-byte 6
                                                :section/body-span-end-byte 20
                                                :section/body-span-start-line 2
                                                :section/body-span-end-line 3}]}))
            (gen/tuple gen-oid gen-oid gen-path gen-uuid gen-uuid)))

(def gen-review-decision
  (gen/fmap (fn [[rid decision id1 id2 id3]]
              (envelope {:observation/id id1
                         :observation/request-id rid
                         :observation/type :review/decision-recorded
                         :review-decision/id id2
                         :review-decision/candidate-id id3
                         :review-decision/decision decision
                         :review-decision/decided-at #inst "2026-01-01T00:00:00.000Z"}))
            (gen/tuple gen-uuid
                       (gen/elements [:accepted :rejected :relabel :deferred
                                      :annotated :do-not-suggest])
                       gen-uuid gen-uuid gen-uuid)))

(def gen-lineage-candidate
  (gen/fmap (fn [[rid relation oid1 oid2 id1 id2]]
              (envelope {:observation/id id1
                         :observation/request-id rid
                         :observation/type :lineage/candidate-generated
                         :lineage-candidate/id id2
                         :lineage-candidate/relation relation
                         :lineage-candidate/generator-version "gen-v1"
                         :lineage-candidate/confidence (double 0.5)
                         :lineage-candidate/source {:span/path-raw "a.md"
                                                    :span/heading-path ["A"]
                                                    :span/commit-oid oid1}
                         :lineage-candidate/target {:span/path-raw "b.md"
                                                    :span/heading-path ["B"]
                                                    :span/commit-oid oid2}
                         :lineage-candidate/tier :provisional
                         :lineage-candidate/generated-at #inst "2026-01-01T00:00:00.000Z"}))
            (gen/tuple gen-uuid
                       (gen/elements [:near-duplicate :continues :refines :references
                                      :possibly-derived-from :possibly-supersedes
                                      :possible-contradiction])
                       gen-oid gen-oid gen-uuid gen-uuid)))

(def record-generators
  "Write op -> generator of schema-valid records for that op."
  {:record-repository-location! gen-repository-location
   :record-revision-at-path! gen-revision-at-path
   :record-ingestion-run! gen-ingestion-run
   :record-checkpoint! gen-checkpoint
   :record-section-extraction! gen-section-extraction
   :record-review-decision! gen-review-decision
   :record-lineage-candidate! gen-lineage-candidate})

(def gen-op-record
  "Generator of [op record] pairs: a uniformly-chosen write op plus one
   schema-valid record for it."
  (gen/bind (gen/elements (vec (keys record-generators)))
            (fn [op] (gen/fmap (fn [record] [op record])
                               (get record-generators op)))))

;; ---------------------------------------------------------------------------
;; Mutations — each MUST flip a valid record to invalid

(def mutations
  "Named single-field mutations over a generated valid record. Every one
   must be rejected by the schema (closed map, types, required fields)."
  {:undeclared-key    (fn [r] (assoc r :not-a-real-key true))
   :drop-envelope-key (fn [r] (dissoc r :observation/id))
   :uuid-as-string    (fn [r] (assoc r :resource-id (str (:resource-id r))))
   :version-bump      (fn [r] (assoc r :observation/schema-version 99))
   :type-as-string    (fn [r] (assoc r :observation/type (name (:observation/type r))))})
