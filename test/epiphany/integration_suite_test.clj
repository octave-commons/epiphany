(ns epiphany.integration-suite-test
  "Anchors the :integration kaocha suite so metadata filtering has a target.
  US-000C replaces this with real local-service readiness diagnostics."
  (:require [clojure.test :refer [deftest is]]))

(deftest ^:integration integration-suite-is-wired
  (is true "the :integration suite selects ^:integration tests"))
