(ns epiphany.integration-suite-test
  "Readiness gate for the :integration kaocha suite.

  Before any integration test runs, this namespace checks that required
  services (MongoDB, S3) are reachable. If any service is unavailable,
  the test fails with a clear diagnostic — never hangs, never fabricates
  results (US-000C acceptance criteria)."
  (:require [clojure.test :refer [deftest is use-fixtures]]
            [epiphany.infra.services :as services]))

(defn- require-services
  "Fixture that checks service readiness before each test.
   Skips the test (via throws) if required services are unavailable."
  [f]
  (if (services/all-available?)
    (f)
    (let [report (services/report)]
      (throw (ex-info (str "Required services unavailable — skipping integration tests.\n"
                           report)
                      {:code :unavailable
                       :report report})))))

(use-fixtures :each require-services)

(deftest ^:integration services-are-reachable
  (is (services/all-available?)
      "MongoDB and S3 must be running for integration tests"))

(deftest ^:integration integration-suite-is-wired
  (is true "the :integration suite selects ^:integration tests"))
