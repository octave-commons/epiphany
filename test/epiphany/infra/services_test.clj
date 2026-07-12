(ns epiphany.infra.services-test
  (:require [clojure.test :refer [deftest is testing]]
            [epiphany.infra.services :as services]))

(deftest tcp-reachable-detects-open-port
  (testing "detects MongoDB on localhost:27017"
    (let [result (services/check-all {:mongodb {:port 27017}
                                      :s3 {:port 99999}})]
      ;; MongoDB should be available on this machine
      (is (= :available (:status (first result))))
      ;; Port 99999 should be unavailable
      (is (= :unavailable (:status (second result)))))))

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
