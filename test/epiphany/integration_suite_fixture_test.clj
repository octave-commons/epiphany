(ns epiphany.integration-suite-fixture-test
  (:require [clojure.test :refer [deftest is]]
            [epiphany.infra.integration-config :as config]
            [epiphany.infra.services :as services]
            [epiphany.integration-suite-test]))

(deftest unavailable-integration-services-refuse-instead-of-skipping
  (let [called? (atom false)
        fixture (ns-resolve 'epiphany.integration-suite-test 'require-services)]
    (with-redefs [services/all-available? (constantly false)
                  config/readiness-options (constantly {})]
      (is (thrown-with-msg? clojure.lang.ExceptionInfo #"Required integration services unavailable"
                            (fixture #(reset! called? true))))
      (is (false? @called?)))))
