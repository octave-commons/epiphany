(ns epiphany.law.registry-test
  (:require [clojure.edn :as edn]
            [clojure.java.io :as io]
            [clojure.string :as string]
            [clojure.test :refer [deftest is testing]]
            [epiphany.law.observation :as observation]
            [epiphany.law.registry :as registry])
  (:import [java.text Normalizer Normalizer$Form]))

(defn- fixture [resource-name]
  (edn/read-string (slurp (io/resource (str "epiphany/law/fixtures/" resource-name)))))

(def registration (fixture "registration_observation_v1.edn"))
(def location (fixture "repository_location_observation_v1.edn"))
(def git-ref (fixture "git_ref_observation_v1.edn"))

(deftest versioned-fixtures-validate
  (testing "direct-mode EDN fixtures satisfy their contracts"
    (is (registry/valid? "observation/registration-v1" registration)
        (pr-str (registry/explain "observation/registration-v1" registration)))
    (is (registry/valid? "observation/repository-location-v1" location)
        (pr-str (registry/explain "observation/repository-location-v1" location)))
    (is (registry/valid? "observation/git-ref-v1" git-ref)
        (pr-str (registry/explain "observation/git-ref-v1" git-ref))))
  (testing "every EDN fixture has a JSON twin documenting the Mongo document shape"
    (doseq [twin ["registration_observation_v1.json"
                  "repository_location_observation_v1.json"
                  "git_ref_observation_v1.json"]]
      (is (some? (io/resource (str "epiphany/law/fixtures/" twin))) twin))))

(deftest provenance-fields-are-required
  (doseq [required [:observation/id
                    :observation/observed-at
                    :observation/adapter-version
                    :observation/schema-version
                    :observation/request-id
                    :resource-id]]
    (is (not (registry/valid? "observation/registration-v1"
                              (dissoc registration required)))
        (str "registration must be rejected without " required))))

(deftest paths-must-carry-exact-provenance
  (testing "a bare string is not an observed path"
    (is (not (registry/valid? "observation/registration-v1"
                              (assoc registration :repository/path "/home/aaron/notes")))))
  (testing "normalized/rewritten variant keys are rejected (closed map)"
    (is (not (registry/valid? "observation/registration-v1"
                              (update registration :repository/path
                                      assoc :path/normalized "/home/aaron/notes/.hm")))))
  (testing "provenance keys are required"
    (is (not (registry/valid? "observation/registration-v1"
                              (update registration :repository/path
                                      dissoc :path/source)))))
  (testing "only exact comparison is lawful"
    (is (not (registry/valid? "observation/registration-v1"
                              (update registration :repository/path
                                      assoc :path/comparison :case-insensitive)))))
  (testing "empty and NUL-containing raw paths are rejected"
    (doseq [raw ["" "notes/\u0000evil.md"]]
      (is (not (registry/valid? "path/observed"
                                {:path/raw raw
                                 :path/source :git-tree-entry
                                 :path/comparison :exact}))))))

(deftest exact-path-comparison-is-byte-for-byte
  ;; ή (eta with tonos) decomposes under NFD; bare ημ would not.
  (let [nfc (Normalizer/normalize ".ήμ/résumé.md" Normalizer$Form/NFC)
        nfd (Normalizer/normalize ".ήμ/résumé.md" Normalizer$Form/NFD)]
    (is (not= nfc nfd) "fixture sanity: the two normal forms differ")
    (is (observation/exact-path? nfc nfc))
    (is (not (observation/exact-path? nfc nfd))
        "Unicode normalization produces a different identity value")
    (is (not (observation/exact-path? "docs/Spec.md" "docs/spec.md"))
        "case folding produces a different identity value")))

(deftest git-observed-values-are-structurally-checked
  (testing "lawful ref observations"
    (is (registry/valid? "git/ref" (:git/observed git-ref)))
    (is (registry/valid? "git/ref-name" "HEAD"))
    (is (registry/valid? "git/oid" (apply str (repeat 64 "a")))
        "sha-256 object names are lawful"))
  (testing "unlawful ref observations"
    (is (not (registry/valid? "git/ref-name" "main"))
        "short ref names are not observed values")
    (doseq [bad-oid [(apply str (repeat 39 "a"))
                     (string/upper-case (:ref/target (:git/observed git-ref)))
                     "not-an-oid"]]
      (is (not (registry/valid? "git/oid" bad-oid)) bad-oid))
    (is (not (registry/valid? "observation/git-ref-v1"
                              (assoc-in git-ref [:git/observed :ref/extra] true)))
        "unknown keys inside the observed value are rejected")))

(deftest registration-retains-instance-and-family-state
  (is (not (registry/valid? "observation/registration-v1"
                            (dissoc registration :family/assessment))))
  (is (not (registry/valid? "observation/registration-v1"
                            (dissoc registration :repository/common-git-dir))))
  (is (not (registry/valid? "observation/registration-v1"
                            (assoc registration :family/assessment :guessed))))
  (is (registry/valid? "observation/registration-v1"
                       (assoc registration
                              :family/assessment :joined-existing
                              :family/id (random-uuid)))))

(deftest the-registry-is-edn-data
  (testing "μ rule: schemas round-trip through EDN unchanged"
    (is (= registry/schemas
           (edn/read-string (pr-str registry/schemas))))))
