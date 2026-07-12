(ns epiphany.domain.status-test
  (:require [clojure.test :refer [deftest is testing]]
            [epiphany.domain.status :as status]))

;; ---------------------------------------------------------------------------
;; Mock adapters

(defn- mock-repo-metadata-adapter [repos]
  {:list-repositories (constantly repos)})

(defn- mock-git-adapter [refs]
  {:repository (constantly ::mock-repo)
   :resolve-ref (constantly refs)})

(defn- mock-obs-adapter [checkpoints]
  {:list-checkpoints (fn [resource-id projection]
                       (filter #(and (= resource-id (:checkpoint/resource-id %))
                                     (= projection (:checkpoint/projection-name %)))
                               checkpoints))})

(defn- mock-index-adapter [stats]
  {:index-stats (constantly stats)})

(def unavailable-repo-metadata
  {:list-repositories (fn [] (throw (ex-info "unavailable" {:code :unavailable})))})

(def error-git-adapter
  {:repository (fn [_] (throw (ex-info "repo not found" {:code :not-found})))
   :resolve-ref (fn [_ _] (throw (ex-info "no refs" {:code :not-found})))})

(def unavailable-index
  {:index-stats (fn [_] (throw (ex-info "unavailable" {:code :unavailable})))})

;; ---------------------------------------------------------------------------
;; make-stage-status tests

(deftest make-stage-status-defaults
  (testing "make-stage-status with defaults"
    (let [s (status/make-stage-status :registration)]
      (is (= :registration (:stage/name s)))
      (is (= :unknown (:stage/status s)))
      (is (= {} (:stage/counts s)))
      (is (= [] (:stage/failures s)))
      (is (= 0 (:stage/retries s)))
      (is (nil? (:stage/checkpoint s)))
      (is (nil? (:stage/lag s))))))

(deftest make-stage-status-custom
  (testing "make-stage-status with custom values"
    (let [s (status/make-stage-status :extraction
                                      :status :ok
                                      :counts {:processed 10}
                                      :retries 2)]
      (is (= :extraction (:stage/name s)))
      (is (= :ok (:stage/status s)))
      (is (= {:processed 10} (:stage/counts s)))
      (is (= 2 (:stage/retries s))))))

;; ---------------------------------------------------------------------------
;; make-failure-record tests

(deftest make-failure-record-defaults
  (testing "make-failure-record with defaults"
    (let [f (status/make-failure-record "error msg")]
      (is (= "error msg" (:failure/error f)))
      (is (nil? (:failure/resource-id f)))
      (is (nil? (:failure/commit-id f)))
      (is (nil? (:failure/version f))))))

(deftest make-failure-record-custom
  (testing "make-failure-record with custom values"
    (let [f (status/make-failure-record "parse error"
                                        :resource-id #uuid "00000000-0000-0000-0000-000000000001"
                                        :commit-id "abc123"
                                        :version "v1")]
      (is (= "parse error" (:failure/error f)))
      (is (= #uuid "00000000-0000-0000-0000-000000000001" (:failure/resource-id f)))
      (is (= "abc123" (:failure/commit-id f)))
      (is (= "v1" (:failure/version f))))))

;; ---------------------------------------------------------------------------
;; query-registration-status tests

(deftest query-registration-ok
  (testing "Registration with repos returns :ok"
    (let [adapter (mock-repo-metadata-adapter [{:id :a} {:id :b}])
          result (status/query-registration-status adapter)]
      (is (= :ok (:stage/status result)))
      (is (= {:registered 2} (:stage/counts result))))))

(deftest query-registration-empty
  (testing "Registration with no repos returns :ok with 0"
    (let [adapter (mock-repo-metadata-adapter [])
          result (status/query-registration-status adapter)]
      (is (= :ok (:stage/status result)))
      (is (= {:registered 0} (:stage/counts result))))))

(deftest query-registration-unavailable
  (testing "Unavailable adapter returns :unavailable"
    (let [result (status/query-registration-status unavailable-repo-metadata)]
      (is (= :unavailable (:stage/status result))))))

(deftest query-registration-error
  (testing "Error adapter returns :error with failure"
    (let [adapter {:list-repositories (fn [] (throw (ex-info "disk full" {})))}
          result (status/query-registration-status adapter)]
      (is (= :error (:stage/status result)))
      (is (= 1 (count (:stage/failures result)))))))

;; ---------------------------------------------------------------------------
;; query-discovery-status tests

(deftest query-discovery-ok
  (testing "Discovery with refs returns :ok"
    (let [adapter (mock-git-adapter ["refs/heads/main"])
          result (status/query-discovery-status adapter #uuid "00000000-0000-0000-0000-000000000001")]
      (is (= :ok (:stage/status result)))
      (is (= {:refs 1} (:stage/counts result))))))

(deftest query-discovery-unavailable
  (testing "Unavailable adapter returns :unavailable"
    (let [result (status/query-discovery-status error-git-adapter
                                                 #uuid "00000000-0000-0000-0000-000000000001")]
      (is (= :error (:stage/status result))))))

;; ---------------------------------------------------------------------------
;; query-extraction-status tests

(deftest query-extraction-ok
  (testing "Extraction with completed checkpoint returns :ok"
    (let [adapter (mock-obs-adapter [{:checkpoint/projection-name "extraction"
                                      :checkpoint/projection-version "v1"
                                      :checkpoint/status :completed
                                      :checkpoint/processed-count 100
                                      :checkpoint/resource-id #uuid "00000000-0000-0000-0000-000000000001"
                                      :checkpoint/last-updated-at (java.util.Date.)}])
          result (status/query-extraction-status adapter
                                                  #uuid "00000000-0000-0000-0000-000000000001")]
      (is (= :ok (:stage/status result)))
      (is (= {:processed 100} (:stage/counts result)))
      (is (some? (:stage/checkpoint result)))
      (is (some? (:stage/lag result))))))

(deftest query-extraction-in-progress
  (testing "Extraction with running checkpoint returns :in-progress"
    (let [adapter (mock-obs-adapter [{:checkpoint/projection-name "extraction"
                                      :checkpoint/status :running
                                      :checkpoint/processed-count 50
                                      :checkpoint/resource-id #uuid "00000000-0000-0000-0000-000000000001"}])
          result (status/query-extraction-status adapter
                                                  #uuid "00000000-0000-0000-0000-000000000001")]
      (is (= :in-progress (:stage/status result))))))

(deftest query-extraction-no-checkpoint
  (testing "Extraction with no checkpoint returns :unknown"
    (let [adapter (mock-obs-adapter [])
          result (status/query-extraction-status adapter
                                                  #uuid "00000000-0000-0000-0000-000000000001")]
      (is (= :unknown (:stage/status result))))))

(deftest query-extraction-unavailable
  (testing "Unavailable adapter returns :unavailable"
    (let [adapter {:list-checkpoints (fn [_ _]
                                       (throw (ex-info "unavailable" {:code :unavailable})))}
          result (status/query-extraction-status adapter
                                                  #uuid "00000000-0000-0000-0000-000000000001")]
      (is (= :unavailable (:stage/status result))))))

;; ---------------------------------------------------------------------------
;; query-indexing-status tests

(deftest query-indexing-ok
  (testing "Indexing with stats returns :ok"
    (let [adapter (mock-index-adapter {:document-count 500 :term-count 10000})
          result (status/query-indexing-status adapter
                                                #uuid "00000000-0000-0000-0000-000000000001")]
      (is (= :ok (:stage/status result)))
      (is (= {:documents 500 :terms 10000} (:stage/counts result))))))

(deftest query-indexing-unavailable
  (testing "Unavailable adapter returns :unavailable"
    (let [result (status/query-indexing-status unavailable-index
                                                #uuid "00000000-0000-0000-0000-000000000001")]
      (is (= :unavailable (:stage/status result))))))

;; ---------------------------------------------------------------------------
;; query-embedding-status tests

(deftest query-embedding-ok
  (testing "Embedding with completed checkpoint returns :ok"
    (let [adapter (mock-obs-adapter [{:checkpoint/projection-name "embedding"
                                      :checkpoint/projection-version "v1"
                                      :checkpoint/status :completed
                                      :checkpoint/processed-count 200
                                      :checkpoint/resource-id #uuid "00000000-0000-0000-0000-000000000001"}])
          result (status/query-embedding-status adapter
                                                 #uuid "00000000-0000-0000-0000-000000000001")]
      (is (= :ok (:stage/status result)))
      (is (= {:processed 200} (:stage/counts result))))))

(deftest query-embedding-no-checkpoint
  (testing "Embedding with no checkpoint returns :unknown"
    (let [adapter (mock-obs-adapter [])
          result (status/query-embedding-status adapter
                                                 #uuid "00000000-0000-0000-0000-000000000001")]
      (is (= :unknown (:stage/status result))))))

;; ---------------------------------------------------------------------------
;; query-status (aggregate) tests

(deftest query-status-aggregates-all-stages
  (testing "query-status returns all stages with summary"
    (let [adapters {:repo-metadata (mock-repo-metadata-adapter [{:id :a}])
                    :git (mock-git-adapter ["refs/heads/main"])
                    :observations (mock-obs-adapter [{:checkpoint/projection-name "extraction"
                                                      :checkpoint/status :completed
                                                      :checkpoint/processed-count 100
                                                      :checkpoint/resource-id #uuid "00000000-0000-0000-0000-000000000001"
                                                      :checkpoint/projection-version "v1"}
                                                     {:checkpoint/projection-name "embedding"
                                                      :checkpoint/status :completed
                                                      :checkpoint/processed-count 100
                                                      :checkpoint/resource-id #uuid "00000000-0000-0000-0000-000000000001"
                                                      :checkpoint/projection-version "v1"}])
                    :index (mock-index-adapter {:document-count 50})}
          result (status/query-status adapters #uuid "00000000-0000-0000-0000-000000000001")]
      (is (= #uuid "00000000-0000-0000-0000-000000000001" (:resource-id result)))
      (is (= 5 (count (:stages result))))
      (is (every? #(contains? #{:ok :error :unavailable :unknown} (:stage/status %))
                  (:stages result)))
      (is (map? (:summary result))))))

(deftest query-status-handles-mixed-states
  (testing "query-status handles mix of ok and unavailable"
    (let [adapters {:repo-metadata unavailable-repo-metadata
                    :git (mock-git-adapter [])
                    :observations (mock-obs-adapter [])
                    :index unavailable-index}
          result (status/query-status adapters #uuid "00000000-0000-0000-0000-000000000001")]
      (is (pos? (:unavailable (:summary result)))))))

(deftest query-status-stages-vector
  (testing "query-status returns stages as vector"
    (let [adapters {:repo-metadata (mock-repo-metadata-adapter [])
                    :git (mock-git-adapter [])
                    :observations (mock-obs-adapter [])
                    :index (mock-index-adapter {})}
          result (status/query-status adapters #uuid "00000000-0000-0000-0000-000000000001")]
      (is (vector? (:stages result))))))

;; ---------------------------------------------------------------------------
;; format-status tests

(deftest format-status-produces-string
  (testing "format-status returns a string"
    (let [status {:resource-id #uuid "00000000-0000-0000-0000-000000000001"
                  :stages [{:stage/name :registration :stage/status :ok :stage/counts {:registered 1}
                            :stage/failures [] :stage/retries 0 :stage/checkpoint nil :stage/lag nil}]
                  :summary {:ok 1 :error 0 :unavailable 0 :unknown 0}}
          result (status/format-status status)]
      (is (string? result))
      (is (.contains result "00000000-0000-0000-0000-000000000001"))
      (is (.contains result "ok")))))

(deftest format-status-shows-failures
  (testing "format-status shows failures"
    (let [status {:resource-id #uuid "00000000-0000-0000-0000-000000000001"
                  :stages [{:stage/name :indexing :stage/status :error
                            :stage/failures [{:failure/error "disk full"}]
                            :stage/retries 0 :stage/checkpoint nil :stage/lag nil
                            :stage/counts {}}]
                  :summary {:ok 0 :error 1 :unavailable 0 :unknown 0}}
          result (status/format-status status)]
      (is (.contains result "disk full")))))

;; ---------------------------------------------------------------------------
;; stages constant

(deftest stages-is-ordered
  (testing "stages vector contains all expected stages"
    (is (= [:registration :discovery :extraction :indexing :embedding]
           status/stages))))
