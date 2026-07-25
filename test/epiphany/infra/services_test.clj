(ns epiphany.infra.services-test
  (:require [clojure.test :refer [deftest is testing]]
            [epiphany.infra.services :as services])
  (:import [java.net ServerSocket]))

(deftest tcp-reachable-detects-open-port
  (testing "detects a deterministic local listener without requiring MongoDB"
    (with-open [listener (ServerSocket. 0)]
      (let [open-port (.getLocalPort listener)
            result (services/check-all
                    {:mongodb {:host "127.0.0.1"
                               :port open-port
                               :timeout-ms 100}
                     :s3 {:host "127.0.0.1"
                          :port 0
                          :timeout-ms 100}})]
        (is (= :available (:status (first result))))
        (is (= :unavailable (:status (second result))))))))

(deftest check-all-returns-vector-of-status-maps
  (let [results (services/check-all)]
    (is (= 2 (count results)))
    (is (every? #(contains? % :service) results))
    (is (every? #(contains? % :status) results))
    (is (some #(= :mongodb (:service %)) results))
    (is (some #(= :s3 (:service %)) results))))

(deftest report-returns-readable-string
  (let [r (services/report)]
    (is (string? r))
    (is (.contains r "Service readiness:"))
    (is (.contains r "mongodb"))))

(deftest all-available-returns-boolean
  (is (boolean? (services/all-available?))))
