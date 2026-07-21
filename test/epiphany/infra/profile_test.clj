(ns epiphany.infra.profile-test
  (:require [clojure.test :refer [deftest is]]
            [epiphany.infra.profile :as profile]
            [epiphany.application.registration :as registration]))

(defn- fake-common-git-dir [path]
  (str path "/.git"))

(defn- fake-observations-port
  "A complete, no-op observations port (all registered write ops + reads)
  usable to stand in for a resolved adapter's :observations map."
  []
  {:find-by-request-id (fn [_] nil)
   :record-repository-location! (fn [_] nil)
   :record-revision-at-path! (fn [_] nil)
   :record-ingestion-run! (fn [_] nil)
   :record-checkpoint! (fn [_] nil)
   :record-section-extraction! (fn [_] nil)
   :record-review-decision! (fn [_] nil)
   :list-ingestion-runs (fn [_] [])
   :list-checkpoints (fn [_] [])
   :list-revision-at-path-by-resource (fn [_] [])
   :list-section-extractions-by-revision (fn [_] [])
   :list-review-decisions (fn [_] [])
   :list-review-decisions-by-candidate (fn [_] [])
   :export-all (fn [] {})
   :import-all (fn [_] nil)})

;; ---------------------------------------------------------------------------
;; Profile validation

(deftest valid-profile-accepts-known-keywords
  (is (profile/valid-profile? :local))
  (is (profile/valid-profile? :services))
  (is (not (profile/valid-profile? :production)))
  (is (not (profile/valid-profile? nil))))

(deftest resolve-adapters-rejects-unknown-profile
  (is (thrown? clojure.lang.ExceptionInfo
              (profile/resolve-adapters {:profile :unknown}))))

;; ---------------------------------------------------------------------------
;; :local profile

(deftest local-profile-returns-in-memory-adapters
  (let [adapters (profile/resolve-adapters {:profile :local
                                            :common-git-dir-fn fake-common-git-dir})]
    (is (map? adapters))
    (is (contains? adapters :git))
    (is (contains? adapters :repository-metadata))
    (is (contains? adapters :observations))
    (is (fn? (:common-git-directory (:git adapters))))
    (is (fn? (:find-by-request-id (:observations adapters))))
    (is (fn? (:record-repository-location! (:observations adapters))))))

(deftest local-profile-adapters-are-independent-worlds
  (let [a1 (profile/resolve-adapters {:profile :local :common-git-dir-fn fake-common-git-dir})
        a2 (profile/resolve-adapters {:profile :local :common-git-dir-fn fake-common-git-dir})]
    ;; Write to a1, verify a2 is unaffected
    ((:write (:repository-metadata a1)) "/repo/.git" #uuid "00000000-0000-0000-0000-000000000001")
    (is (nil? ((:read (:repository-metadata a2)) "/repo/.git")))))

;; ---------------------------------------------------------------------------
;; :services profile

(deftest services-profile-throws-unavailable
  (let [ex (try
             (profile/resolve-adapters {:profile :services})
             nil
             (catch clojure.lang.ExceptionInfo e e))]
    (is (some? ex))
    (is (= :unavailable (:code (ex-data ex))))
    (is (= :services (:profile (ex-data ex))))))

;; ---------------------------------------------------------------------------
;; Composition with application layer (bootstrap test)

(deftest bootstrap-local-mode-composes-with-registration
  (let [adapters (profile/resolve-adapters {:profile :local
                                            :common-git-dir-fn fake-common-git-dir})
        result (registration/register! adapters "/repos/notes")]
    (is (= "/repos/notes" (:repository-path result)))
    (is (= "/repos/notes/.git" (:common-git-dir result)))
    (is (uuid? (:resource-id result)))))

(deftest bootstrap-local-mode-idempotent-by-request-id
  (let [adapters (profile/resolve-adapters {:profile :local
                                            :common-git-dir-fn fake-common-git-dir})
        cmd {:request-id #uuid "11111111-1111-1111-1111-111111111111"
             :repository-path "/repos/notes"}
        first-result  (registration/register! adapters cmd)
        second-result (registration/register! adapters cmd)]
    (is (= first-result second-result))))

;; ---------------------------------------------------------------------------
;; Validation wrapper composed for BOTH profiles (ENG-017B)

(deftest local-profile-composes-validating-observations-wrapper
  (let [adapters (profile/resolve-adapters {:profile :local
                                            :common-git-dir-fn fake-common-git-dir})]
    ;; An invalid record must be rejected by the wrapper before it reaches
    ;; the adapter — proof the observations port is the validating wrapper.
    (is (thrown-with-msg?
         clojure.lang.ExceptionInfo #"Schema validation failed"
         ((:record-repository-location! (:observations adapters))
          {:not-a-valid-observation true})))))

(deftest services-profile-composes-validating-observations-wrapper
  ;; :services adapters are UNAVAILABLE today, so stub the per-profile raw
  ;; resolution. This proves the validation wrapping is profile-agnostic
  ;; (applied outside the per-profile branch): whatever :services resolves
  ;; to, its observations port is wrapped by the same code path as :local.
  (with-redefs [profile/resolve-raw-adapters
                (fn [_] {:observations (fake-observations-port)})]
    (let [adapters (profile/resolve-adapters {:profile :services})]
      (is (thrown-with-msg?
           clojure.lang.ExceptionInfo #"Schema validation failed"
           ((:record-repository-location! (:observations adapters))
            {:not-a-valid-observation true}))))))

;; ---------------------------------------------------------------------------
;; Diagnostics

(deftest profile-description-returns-readable-string
  (is (string? (profile/profile-description :local)))
  (is (string? (profile/profile-description :services)))
  (is (.contains (profile/profile-description :local) "in-memory")))
