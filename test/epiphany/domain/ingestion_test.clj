(ns epiphany.domain.ingestion-test
  (:require [clojure.test :refer [deftest is testing]]
            [epiphany.domain.ingestion :as ingestion]))

(deftest make-run-id-returns-uuid
  (is (uuid? (ingestion/make-run-id))))

(deftest make-run-record-has-required-fields
  (let [record (ingestion/make-run-record
                {:resource-id    #uuid "aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeeee"
                 :repo-path      {:path/raw       "/repo"
                                  :path/source    :filesystem-argument
                                  :path/comparison :exact}
                 :selected-refs  ["refs/heads/main"]
                 :commit-count   42
                 :failure-count  1
                 :failures       [{:failure/reason "object-unreadable"
                                   :failure/message "boom"}]})]
    (is (= :ingestion/run-completed (:observation/type record)))
    (is (uuid? (:observation/id record)))
    (is (inst? (:observation/observed-at record)))
    (is (= "0.1.0" (:observation/adapter-version record)))
    (is (= 1 (:observation/schema-version record)))
    (is (= #uuid "aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeeee" (:resource-id record)))
    (is (= "/repo" (get-in record [:ingestion/repo-path :path/raw])))
    (is (= ["refs/heads/main"] (:ingestion/selected-refs record)))
    (is (= 42 (:ingestion/commit-count record)))
    (is (= 1 (:ingestion/failure-count record)))
    (is (= 1 (count (:ingestion/failures record))))))

(deftest make-run-record-with-request-id
  (let [rid (random-uuid)
        record (ingestion/make-run-record
                {:resource-id    #uuid "aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeeee"
                 :repo-path      {:path/raw "/repo" :path/source :filesystem-argument :path/comparison :exact}
                 :selected-refs  []
                 :commit-count   0
                 :failure-count  0
                 :failures       []
                 :request-id     rid})]
    (is (= rid (:observation/request-id record)))))

(deftest make-checkpoint-record-has-required-fields
  (let [run-id (random-uuid)
        record (ingestion/make-checkpoint-record
                {:resource-id         #uuid "aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeeee"
                 :projection-name     "revision-at-path"
                 :projection-version  1
                 :ingestion-run-id    run-id
                 :status              :completed
                 :processed-count     42})]
    (is (= :projection/checkpoint-recorded (:observation/type record)))
    (is (uuid? (:observation/id record)))
    (is (= "revision-at-path" (:checkpoint/projection-name record)))
    (is (= 1 (:checkpoint/projection-version record)))
    (is (= run-id (:checkpoint/ingestion-run-id record)))
    (is (= :completed (:checkpoint/status record)))
    (is (= 42 (:checkpoint/processed-count record)))
    (is (nil? (:checkpoint/last-processed-oid record)))
    (is (nil? (:checkpoint/error-message record)))))

(deftest make-checkpoint-record-with-optional-fields
  (let [record (ingestion/make-checkpoint-record
                {:resource-id         #uuid "aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeeee"
                 :projection-name     "revision-at-path"
                 :ingestion-run-id    (random-uuid)
                 :status              :failed
                 :processed-count     10
                 :last-processed-oid  "abc123"
                 :error-message       "connection refused"})]
    (is (= "abc123" (:checkpoint/last-processed-oid record)))
    (is (= "connection refused" (:checkpoint/error-message record)))))

(deftest run-ingestion-orchestrates-traversal
  (testing "run-ingestion walks commits, records run and checkpoint"
    (let [runs (atom [])
          checkpoints (atom [])
          fake-git {:common-git-directory (constantly "/repo/.git")
                    :reachable-commits (fn [_ _]
                                         {:commits  [{:commit/oid "aaa"} {:commit/oid "bbb"}]
                                          :failures [{:failure/reason "object-unreadable"
                                                      :failure/message "boom"}]
                                          :commit-count 2
                                          :failure-count 1})}
          fake-obs {:find-by-request-id (constantly nil)
                    :record-repository-location! (constantly nil)
                    :record-ingestion-run! (fn [r] (swap! runs conj r) nil)
                    :record-checkpoint! (fn [c] (swap! checkpoints conj c) nil)}
          result (ingestion/run-ingestion
                  {:git fake-git :observations fake-obs}
                  {:resource-id    #uuid "aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeeee"
                   :repository-path "/repo"
                    :selected-refs   ["refs/heads/main"]})]
      (is (= :ingestion/run-completed (:observation/type result)))
      (is (= 2 (:ingestion/commit-count result)))
      (is (= 1 (:ingestion/failure-count result)))
      (is (= 1 (count @runs)))
      (is (= 1 (count @checkpoints)))
      (is (= :completed (:checkpoint/status (first @checkpoints))))
      (is (= 2 (:checkpoint/processed-count (first @checkpoints)))))))
