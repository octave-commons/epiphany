(ns epiphany.application.validation-test
  (:require [clojure.test :refer [deftest is testing]]
            [epiphany.application.validation :as validation]
            [epiphany.law.operations :as operations]))

;; ---------------------------------------------------------------------------
;; Helpers

(defn- spy-adapter
  "Create a spy observations port that records calls.
  Returns [port calls-atom] tuple."
  []
  (let [calls (atom [])]
    [{:find-by-request-id (fn [rid] (swap! calls conj [:find-by-request-id rid]) nil)
      :record-repository-location! (fn [obs] (swap! calls conj [:record-repository-location! obs]) nil)
      :record-revision-at-path! (fn [obs] (swap! calls conj [:record-revision-at-path! obs]) nil)
      :record-ingestion-run! (fn [obs] (swap! calls conj [:record-ingestion-run! obs]) nil)
      :record-checkpoint! (fn [obs] (swap! calls conj [:record-checkpoint! obs]) nil)
      :record-section-extraction! (fn [obs] (swap! calls conj [:record-section-extraction! obs]) nil)
      :record-review-decision! (fn [obs] (swap! calls conj [:record-review-decision! obs]) nil)
      :list-ingestion-runs (fn [rid] (swap! calls conj [:list-ingestion-runs rid]) [])
      :list-checkpoints (fn [rid] (swap! calls conj [:list-checkpoints rid]) [])
      :list-revision-at-path-by-resource (fn [rid] (swap! calls conj [:list-revision-at-path-by-resource rid]) [])
      :list-section-extractions-by-revision (fn [rid] (swap! calls conj [:list-section-extractions-by-revision rid]) [])
      :list-review-decisions (fn [rid] (swap! calls conj [:list-review-decisions rid]) [])
      :list-review-decisions-by-candidate (fn [cid] (swap! calls conj [:list-review-decisions-by-candidate cid]) [])
      :export-all (fn [] (swap! calls conj [:export-all]) {})
      :import-all (fn [data] (swap! calls conj [:import-all data]) nil)}
     calls]))

(defn- valid-repository-location-observation []
  {:observation/type :repository/location-observed
   :observation/id #uuid "00000000-0000-0000-0000-000000000001"
   :observation/request-id #uuid "00000000-0000-0000-0000-000000000002"
   :observation/observed-at #inst "2026-07-13T00:00:00Z"
   :observation/adapter-version "0.1.0"
   :observation/schema-version 1
   :resource-id #uuid "00000000-0000-0000-0000-000000000003"
   :repository/path {:path/raw "/repos/notes"
                     :path/source :filesystem-argument
                     :path/comparison :exact}
   :repository/common-git-dir {:path/raw "/repos/notes/.git"
                               :path/source :filesystem-argument
                               :path/comparison :exact}})

;; ---------------------------------------------------------------------------
;; All registry ops wrapped

(deftest all-registry-write-ops-are-wrapped
  (testing "every port-write-operation is wrapped by validating-observations-port"
    (let [[spy calls] (spy-adapter)
          wrapped (validation/validating-observations-port spy)]
      (doseq [op operations/port-write-operations]
        (is (fn? (get wrapped op))
            (str "Operation " op " should be wrapped"))))))

(deftest read-ops-pass-through-unwrapped
  (testing "read operations are not wrapped"
    (let [[spy calls] (spy-adapter)
          wrapped (validation/validating-observations-port spy)]
      ;; :find-by-request-id is a read, not in port-write-operations,
      ;; so it should pass through unchanged.
      (is (= (wrapped :find-by-request-id)
             (spy :find-by-request-id))
          "find-by-request-id should pass through unchanged"))))

;; ---------------------------------------------------------------------------
;; Rejection before delegation

(deftest invalid-write-rejected-before-delegation
  (testing "invalid record does not reach the adapter"
    (let [calls (atom [])
          port {:record-repository-location!
                (fn [obs] (swap! calls conj obs) nil)
                :find-by-request-id (fn [_] nil)
                :list-ingestion-runs (fn [_] [])
                :list-checkpoints (fn [_] [])
                :list-revision-at-path-by-resource (fn [_] [])
                :list-section-extractions-by-revision (fn [_] [])
                :export-all (fn [] {})
                :import-all (fn [_] nil)}
          wrapped (validation/validating-observations-port port)]
      ;; Call with an invalid record (missing required fields)
      (is (thrown? clojure.lang.ExceptionInfo
                  ((:record-repository-location! wrapped)
                   {:not-a-valid-observation true})))
      ;; The spy should not have been called
      (is (empty? @calls)
          "Adapter should not receive invalid records"))))

(deftest invalid-write-returns-schema-validation-failed
  (testing "error datum has :code :schema-validation-failed"
    (let [port {:record-repository-location! (fn [_] nil)
                :find-by-request-id (fn [_] nil)
                :list-ingestion-runs (fn [_] [])
                :list-checkpoints (fn [_] [])
                :list-revision-at-path-by-resource (fn [_] [])
                :list-section-extractions-by-revision (fn [_] [])
                :export-all (fn [] {})
                :import-all (fn [_] nil)}
          wrapped (validation/validating-observations-port port)]
      (try
        ((:record-repository-location! wrapped)
         {:not-a-valid-observation true})
        (is false "should have thrown")
        (catch clojure.lang.ExceptionInfo e
          (let [data (ex-data e)]
            (is (= :schema-validation-failed (:code data)))
            (is (= :record-repository-location! (:operation data)))
            (is (contains? data :schema/name))
            (is (contains? data :explanation))
            (is (not (contains? data :record)))
            (is (not (contains? data :value)))))))))

;; ---------------------------------------------------------------------------
;; Valid write delegates

(deftest valid-write-delegates-to-adapter
  (testing "valid record reaches the adapter unchanged"
    (let [calls (atom [])
          port {:record-repository-location!
                (fn [obs] (swap! calls conj obs) nil)
                :find-by-request-id (fn [_] nil)
                :list-ingestion-runs (fn [_] [])
                :list-checkpoints (fn [_] [])
                :list-revision-at-path-by-resource (fn [_] [])
                :list-section-extractions-by-revision (fn [_] [])
                :export-all (fn [] {})
                :import-all (fn [_] nil)}
          wrapped (validation/validating-observations-port port)
          obs (valid-repository-location-observation)]
      ((:record-repository-location! wrapped) obs)
      (is (= 1 (count @calls)))
      (is (= obs (first @calls))))))

;; ---------------------------------------------------------------------------
;; Error data excludes record content

(deftest error-data-excludes-record-content
  (testing "thrown ex-data never contains the record's content"
    (let [port {:record-repository-location! (fn [_] nil)
                :find-by-request-id (fn [_] nil)
                :list-ingestion-runs (fn [_] [])
                :list-checkpoints (fn [_] [])
                :list-revision-at-path-by-resource (fn [_] [])
                :list-section-extractions-by-revision (fn [_] [])
                :export-all (fn [] {})
                :import-all (fn [_] nil)}
          wrapped (validation/validating-observations-port port)
          secret-record {:observation/type :repository/location-observed
                         :resource-id #uuid "00000000-0000-0000-0000-000000000001"
                         :secret-data "classified"}]
      (try
        ((:record-repository-location! wrapped) secret-record)
        (catch clojure.lang.ExceptionInfo e
          (let [data (ex-data e)]
            (is (not (contains? data :record)))
            (is (not (contains? data :value)))
            (is (not (some #(re-find #"classified" (str (val %))) data)))))))))
