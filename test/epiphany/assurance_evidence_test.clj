(ns epiphany.assurance-evidence-test
  "ENG-017J: the assurance artifact validates against its own Malli
   schema (dogfood), and a structurally-invalid artifact is rejected."
  (:require [clojure.test :refer [deftest is testing]]
            [malli.core :as m]
            [epiphany.assurance-evidence :as evidence]))

(def ^:private minimal-valid-artifact
  {:artifact/version 1
   :revision {:sha "abc123" :branch "main" :dirty? false}
   :generated-at "2026-07-25T00:00:00Z"
   :commands [{:name "unit-test" :exit 0 :summary "1 tests, 1 assertions, 0 failures."}]
   :property-seeds [{:label "replay" :seed 42}]
   :interop {:baseline-clean? true :delta "()"}
   :suites {:unit {:tests 1 :assertions 1 :failures 0 :errors 0}}})

(deftest artifact-schema-accepts-a-valid-artifact
  (is (m/validate evidence/artifact-schema minimal-valid-artifact)))

(deftest artifact-schema-rejects-undeclared-fields
  (testing "a hand-edited artifact with an extra field is invalid (closed map)"
    (is (not (m/validate evidence/artifact-schema
                         (assoc minimal-valid-artifact :agent-note "trust me"))))))

(deftest artifact-schema-rejects-missing-required-fields
  (doseq [field [:artifact/version :revision :generated-at :commands
                 :property-seeds :interop :suites]]
    (is (not (m/validate evidence/artifact-schema
                         (dissoc minimal-valid-artifact field)))
        (str "artifact missing " field " must be invalid"))))

(deftest suite-summary-parser
  (is (= {:tests 737 :assertions 1988 :failures 0 :errors 0}
         (evidence/parse-suite-summary "737 tests, 1988 assertions, 0 failures.")))
  (is (= {:tests 20 :assertions 99 :failures 2 :errors 0}
         (evidence/parse-suite-summary "20 tests, 99 assertions, 2 failures.")))
  (testing "ANSI codes do not break the parse"
    (is (= {:tests 5 :assertions 9 :failures 0 :errors 0}
           (evidence/parse-suite-summary "[32m5 tests, 9 assertions, 0 failures.[m")))))
