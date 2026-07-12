(ns epiphany.infra.profile-test
  (:require [clojure.test :refer [deftest is]]
            [epiphany.infra.profile :as profile]
            [epiphany.application.registration :as registration]))

(defn- fake-common-git-dir [path]
  (str path "/.git"))

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
;; Diagnostics

(deftest profile-description-returns-readable-string
  (is (string? (profile/profile-description :local)))
  (is (string? (profile/profile-description :services)))
  (is (.contains (profile/profile-description :local) "in-memory")))
