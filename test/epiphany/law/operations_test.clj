(ns epiphany.law.operations-test
  (:require [clojure.test :refer [deftest is testing]]
            [epiphany.law.operations :as operations]
            [epiphany.law.registry :as registry]))

;; ---------------------------------------------------------------------------
;; Completeness: every port write is registered

(deftest every-port-write-is-registered
  (testing "every observations-port write operation has a registry entry"
    (let [port-writes operations/port-write-operations
          registered  (operations/registered-operations)
          missing     (clojure.set/difference port-writes registered)]
      (is (empty? missing)
          (str "Port write operations missing from registry: " missing)))))

(deftest every-registered-operation-is-a-port-write
  (testing "every registry entry is a port write operation"
    (let [registered  (operations/registered-operations)
          port-writes operations/port-write-operations
          extra       (clojure.set/difference registered port-writes)]
      (is (empty? extra)
          (str "Registry entries not in port writes: " extra)))))

;; ---------------------------------------------------------------------------
;; Schema resolution

(deftest every-registered-schema-resolves
  (testing "every :input-schema in the registry resolves via law.registry"
    (doseq [[op entry] operations/operations]
      (when-let [schema-name (:input-schema entry)]
        (is (some? (registry/schema schema-name))
            (str "Schema " schema-name " (op " op ") does not resolve"))))))

;; ---------------------------------------------------------------------------
;; Unknown operation

(deftest unregistered-operation-is-explicit
  (testing "schema-for-operation returns stable error for unknown op"
    (let [result (operations/schema-for-operation :nonexistent-operation)]
      (is (= :unregistered-write-operation (:code result)))
      (is (= :nonexistent-operation (:operation result)))
      (is (not (contains? result :record))))))

;; ---------------------------------------------------------------------------
;; Version mismatch

(deftest schema-version-mismatch-detected
  (testing "validate-version detects version mismatch as data"
    (let [record {:observation/schema-version 99}
          result (operations/validate-version :record-repository-location! record)]
      (is (= :schema-version-mismatch (:code result)))
      (is (= :record-repository-location! (:operation result)))
      (is (= "observation/repository-location-v1" (:schema/name result)))
      (is (= 1 (:expected-version result)))
      (is (= 99 (:actual-version result))))))

(deftest valid-version-returns-nil
  (testing "validate-version returns nil for correct version"
    (let [record {:observation/schema-version 1}
          result (operations/validate-version :record-repository-location! record)]
      (is (nil? result)))))

(deftest unknown-operation-version-returns-error
  (testing "validate-version returns error for unregistered operation"
    (let [record {:observation/schema-version 1}
          result (operations/validate-version :nonexistent-op record)]
      (is (= :unregistered-write-operation (:code result))))))

;; ---------------------------------------------------------------------------
;; Error data excludes record content

(deftest error-data-excludes-record-content
  (testing "error datums never contain the offending record's content"
    (let [unreg (operations/schema-for-operation :sensitive-data-op)
          mismatch (operations/validate-version
                    :record-repository-location!
                    {:observation/schema-version 99
                     :repository/path {:path/raw "/secret/docs.md"
                                       :path/source :git-tree-entry
                                       :path/comparison :exact}})]
      ;; unregistered operation error
      (is (not (contains? unreg :record)))
      (is (not (contains? unreg :value)))
      ;; version mismatch error
      (is (not (contains? mismatch :record)))
      (is (not (contains? mismatch :value)))
      ;; neither contains the path or any user data
      (is (not (some #(re-find #"secret" (str (val %))) unreg)))
      (is (not (some #(re-find #"secret" (str (val %))) mismatch))))))

;; ---------------------------------------------------------------------------
;; Lookup helpers

(deftest registered-operations-returns-set
  (testing "registered-operations returns a set of keywords"
    (let [ops (operations/registered-operations)]
      (is (set? ops))
      (is (contains? ops :record-repository-location!))
      (is (contains? ops :record-section-extraction!))
      (is (contains? ops :import-all)))))

(deftest registered-write-operations-excludes-import-all
  (testing "registered-write-operations excludes :import-all"
    (let [ops (operations/registered-write-operations)]
      (is (not (contains? ops :import-all)))
      (is (contains? ops :record-repository-location!)))))
