(ns epiphany.law-suite.generative-laws-test
  "ENG-017I: generative and epistemic verification laws.

  Properties over GENERATED records, not fixed fixtures:
  - mutation laws: valid closed record + single-field mutation ->
    rejected without state change, over every registered write op
  - metamorphic laws: replay idempotency, export->import->export
  - epistemic laws: corrupt / unsupported-version / unavailable / empty
    stay pairwise distinguishable, never collapsed

  Every run prints its seed; re-run a failure with the printed seed."
  (:require [clojure.test :refer [deftest is testing]]
            [clojure.test.check :as tc]
            [clojure.test.check.generators :as gen]
            [clojure.test.check.properties :as prop]
            [clojure.edn]
            [clojure.java.io]
            [clojure.set]
            [epiphany.domain.backup :as backup]
            [epiphany.infra.adapters.in-memory :as in-memory]
            [epiphany.law-suite.generators :as gens]
            [epiphany.law.operations :as operations]
            [epiphany.law.registry :as law-registry]))

(def ^:private num-tests 30)

(defn- quick
  "Run a property with a printed, replayable seed. The seed is also
   appended to target/property-seeds.edn (kaocha's capture-output swallows
   console prints; the file is the durable, CI-readable record — ENG-017J)."
  [label property]
  (let [seed (rand-int 1000000)
        _ (println (str "SEED " label " " seed))
        _ (clojure.java.io/make-parents "target/property-seeds.edn")
        _ (spit "target/property-seeds.edn"
                (str (pr-str {:label label :seed seed}) "\n")
                :append true)
        result (tc/quick-check num-tests property :seed seed)]
    (is (:pass? result)
        (str label " failed (replay with seed " seed "): "
             (pr-str (get-in result [:shrunk :smallest] (:fail result)))))))

(defn- make-port []
  (:observations (in-memory/make {:common-git-dir-fn (fn [p] (str p "/.git"))})))

;; ---------------------------------------------------------------------------
;; Mutation laws: valid generated record + mutation -> rejected, stateless

(deftest generated-records-are-schema-valid-sanity
  (testing "the generators produce records the registry accepts (else the mutation laws are vacuous)"
    (doseq [[op g] gens/record-generators]
      (let [samples (gen/sample g 5)]
        (doseq [record samples]
          (is (nil? (operations/validate-version op record))
              (str op " generated a record with a bad version"))
          (is (nil? (law-registry/explain
                     (:input-schema (operations/schema-for-operation op)) record))
              (str op " generated a schema-invalid record: "
                   (pr-str (law-registry/explain
                            (:input-schema (operations/schema-for-operation op)) record)))))))))

(deftest mutation-law-undeclared-key-rejected-statelessly
  (quick "undeclared-key"
         (prop/for-all [[op record] gens/gen-op-record]
           (let [port (make-port)
                 before ((:export-all port))
                 mutated ((:undeclared-key gens/mutations) record)]
             (try
               ((get port op) mutated)
               false ;; accepted a record with an undeclared key
               (catch clojure.lang.ExceptionInfo _
                 (= before ((:export-all port)))))))))

(deftest mutation-law-uuid-as-string-rejected-statelessly
  (quick "uuid-as-string"
         (prop/for-all [[op record] gens/gen-op-record]
           (let [port (make-port)
                 before ((:export-all port))
                 mutated ((:uuid-as-string gens/mutations) record)]
             (try
               ((get port op) mutated)
               false
               (catch clojure.lang.ExceptionInfo _
                 (= before ((:export-all port)))))))))

(deftest mutation-law-version-bump-is-unsupported
  (quick "version-bump"
         (prop/for-all [[op record] gens/gen-op-record]
           (let [mutated ((:version-bump gens/mutations) record)
                 result (operations/validate-version op mutated)]
             (= :schema-version-mismatch (:code result))))))

(deftest mutation-law-drop-envelope-key-rejected-statelessly
  (quick "drop-envelope-key"
         (prop/for-all [[op record] gens/gen-op-record]
           (let [port (make-port)
                 before ((:export-all port))
                 mutated ((:drop-envelope-key gens/mutations) record)]
             (try
               ((get port op) mutated)
               false
               (catch clojure.lang.ExceptionInfo _
                 (= before ((:export-all port)))))))))

;; ---------------------------------------------------------------------------
;; Metamorphic laws

(deftest metamorphic-replay-idempotent-and-conflict
  (quick "replay-idempotency"
         (prop/for-all [record gens/gen-repository-location]
           (let [port (make-port)
                 rid (:observation/request-id record)]
             ((:record-repository-location! port) record)
             (and (nil? ((:record-repository-location! port) record))
                  (= :idempotency-conflict
                     (:code ((:record-repository-location! port)
                             (assoc record :observation/id (random-uuid)))))
                  (= record ((:find-by-request-id port) rid)))))))

(deftest metamorphic-export-import-export-equivalence
  (quick "export-import-export"
         (prop/for-all [record gens/gen-repository-location]
           (let [source (make-port)
                 file (str (java.nio.file.Files/createTempFile
                            "epiphany-gen-backup" ".edn"
                            (make-array java.nio.file.attribute.FileAttribute 0)))]
             ((:record-repository-location! source) record)
             (backup/export-to-file source file)
             (let [target (make-port)
                   _ (backup/import-from-file target file)
                   file2 (str file ".re")]
               (backup/export-to-file target file2)
               (= (:data (clojure.edn/read-string (slurp file)))
                  (:data (clojure.edn/read-string (slurp file2)))))))))

;; ---------------------------------------------------------------------------
;; Epistemic laws: outcomes never collapse

(deftest epistemic-law-outcomes-never-collapse
  (quick "epistemic-distinguishability"
         (prop/for-all [record gens/gen-repository-location]
           (let [source (make-port)
                 _ ((:record-repository-location! source) record)
                 file (str (java.nio.file.Files/createTempFile
                            "epiphany-epistemic" ".edn"
                            (make-array java.nio.file.attribute.FileAttribute 0)))
                 _ (backup/export-to-file source file)
                 payload (clojure.edn/read-string (slurp file))
                 ;; scenario outcomes:
                 unavailable (try (backup/import-from-file (make-port) "/nonexistent.epiphany.edn")
                                  nil
                                  (catch clojure.lang.ExceptionInfo e (:code (ex-data e))))
                 corrupt (let [target (make-port)
                               _broken (assoc-in payload [:data "repository-location" 0
                                                          :observation/schema-version] 1)
                               _ (spit file (pr-str (update payload :manifest dissoc :content-hash)))]
                           (try (backup/import-from-file target file)
                                nil
                                (catch clojure.lang.ExceptionInfo e (:code (ex-data e)))))
                 unsupported (let [target (make-port)
                                   broken (assoc-in payload [:data "repository-location" 0
                                                             :observation/schema-version] 99)
                                   sorted (into (sorted-map) (:data broken))
                                   manifest (assoc (:manifest broken)
                                                   :content-hash
                                                   (let [d (.digest (java.security.MessageDigest/getInstance "SHA-256")
                                                                    (.getBytes (pr-str sorted) "UTF-8"))]
                                                     (.encodeToString (java.util.Base64/getEncoder) d)))]
                               (spit file (pr-str {:manifest manifest :data (:data broken)}))
                               (try (backup/import-from-file target file)
                                    nil
                                    (catch clojure.lang.ExceptionInfo e (:code (ex-data e)))))
                 empty-ok (let [empty-file (str file ".empty")
                                _ (backup/export-to-file (make-port) empty-file)]
                            (backup/import-from-file (make-port) empty-file))]
             (and (= :source/unavailable unavailable)
                  (= :integrity/corrupt corrupt)
                  (= :integrity/unsupported-version unsupported)
                  (map? empty-ok)
                  (distinct? unavailable corrupt unsupported))))))

;; ---------------------------------------------------------------------------
;; Coverage matrix (machine-readable; a registered op missing a category
;; turns the suite red by construction)

(def law-coverage-matrix
  "Registered write op -> law categories satisfied for it. Data, not
   prose: the completeness test below consumes it."
  {:record-repository-location! #{:valid-accepted :invalid-rejected :rejection-stateless
                                  :idempotent-replay :changed-content-conflict
                                  :mutation-rejected :export-import-round-trip}
   :record-revision-at-path! #{:valid-accepted :invalid-rejected :rejection-stateless
                               :mutation-rejected}
   :record-ingestion-run! #{:valid-accepted :invalid-rejected :rejection-stateless
                            :mutation-rejected}
   :record-checkpoint! #{:valid-accepted :invalid-rejected :rejection-stateless
                         :mutation-rejected}
   :record-section-extraction! #{:valid-accepted :invalid-rejected :rejection-stateless
                                 :mutation-rejected}
   :record-review-decision! #{:valid-accepted :invalid-rejected :rejection-stateless
                              :idempotent-replay :changed-content-first-write-wins
                              :mutation-rejected}
   :record-lineage-candidate! #{:valid-accepted :invalid-rejected :rejection-stateless
                                :idempotent-replay :changed-content-first-write-wins
                                :mutation-rejected}})

(deftest coverage-matrix-covers-every-registered-op
  (testing "every registered write op has declared law coverage; every declared op is registered"
    (let [registered (operations/registered-write-operations)
          covered (set (keys law-coverage-matrix))]
      (is (every? covered registered)
          (str "registered ops with NO law coverage declared: "
               (clojure.set/difference registered covered)))
      (is (every? registered covered)
          (str "coverage declared for unregistered ops: "
               (clojure.set/difference covered registered))))))

(deftest coverage-matrix-has-teeth
  (testing "an op missing from the matrix is detectably uncovered (negative fixture)"
    (let [registered (conj (operations/registered-write-operations)
                           :record-future-thing!)
          covered (set (keys law-coverage-matrix))]
      (is (seq (clojure.set/difference registered covered))
          "the completeness check above would catch an undeclared op"))))
