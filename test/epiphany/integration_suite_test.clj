(ns epiphany.integration-suite-test
  "Readiness gate for the :integration kaocha suite.

  Before any integration test runs, this namespace checks that required
  services (MongoDB, S3) are reachable. If any service is unavailable,
  the test fails with a clear diagnostic — never hangs, never fabricates
  results (US-000C acceptance criteria)."
  (:require [clojure.test :refer [deftest is use-fixtures]]
            [epiphany.infra.integration-config :as config]
            [epiphany.infra.services :as services]))

(defn- require-services
  "Refuse unavailable required services when integration tests actually execute."
  [f]
  (if (services/all-available? (config/readiness-options))
    (f)
    (throw (ex-info "Required integration services unavailable; run the configured local service supervisor"
                    {:code :integration-service-unavailable}))))

(use-fixtures :each require-services)

(deftest ^:integration services-are-reachable
  (is (services/all-available? (config/readiness-options))
      "MongoDB and S3 must be running for integration tests"))

(deftest ^:integration integration-suite-is-wired
  (is true "the :integration suite selects ^:integration tests"))
