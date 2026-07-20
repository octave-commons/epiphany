(ns epiphany.static.interop-inventory-test
  (:require [clojure.test :refer [deftest testing is]]
            [epiphany.static.interop-inventory :as inv]))

(defn- fixture-inventory
  [ns-sym quadrant & {:keys [imports dot-calls static-calls type-hints]
                       :or {imports #{} dot-calls 0 static-calls 0 type-hints 0}}]
  {ns-sym {:ns ns-sym :quadrant quadrant :imports imports
           :dot-calls dot-calls :static-calls static-calls :type-hints type-hints}})

(deftest interop-count-test
  (testing "sums imports, dot-calls, static-calls, and type-hints"
    (is (= 4 (inv/interop-count {:imports #{'java.util.Date} :dot-calls 2
                                  :static-calls 0 :type-hints 1})))))

(deftest ratchet-violations-unchanged-domain-test
  (testing "a domain.* namespace with no growth over baseline is clean"
    (let [inv-map (fixture-inventory 'epiphany.domain.status :domain :dot-calls 2)]
      (is (empty? (inv/ratchet-violations inv-map inv-map #{}))))))

(deftest ratchet-violations-new-domain-interop-test
  (testing "domain.* growing new dot-calls beyond baseline is a violation"
    (let [baseline (fixture-inventory 'epiphany.domain.status :domain :dot-calls 0)
          current (fixture-inventory 'epiphany.domain.status :domain :dot-calls 3)]
      (let [violations (inv/ratchet-violations baseline current #{})]
        (is (= 1 (count violations)))
        (is (= 'epiphany.domain.status (:ns (first violations))))))))

(deftest ratchet-violations-law-new-import-test
  (testing "law.* growing a new Java import beyond baseline is a violation"
    (let [baseline (fixture-inventory 'epiphany.law.ports :law)
          current (fixture-inventory 'epiphany.law.ports :law :imports #{'java.util.UUID})]
      (is (= 1 (count (inv/ratchet-violations baseline current #{})))))))

(deftest ratchet-violations-application-new-interop-test
  (testing "application.* growing static calls beyond baseline is a violation"
    (let [baseline (fixture-inventory 'epiphany.application.registration :application)
          current (fixture-inventory 'epiphany.application.registration :application :static-calls 2)]
      (is (= 1 (count (inv/ratchet-violations baseline current #{})))))))

(deftest ratchet-violations-exception-suppresses-test
  (testing "a recorded dated exception suppresses an otherwise-flagged growth"
    (let [baseline (fixture-inventory 'epiphany.domain.status :domain)
          current (fixture-inventory 'epiphany.domain.status :domain :dot-calls 5)]
      (is (empty? (inv/ratchet-violations baseline current #{'epiphany.domain.status}))))))

(deftest ratchet-violations-infra-and-shape-unbounded-test
  (testing "infra.* and shape.* growth is never ratcheted — adapter interop is measured, not banned"
    (let [baseline (merge (fixture-inventory 'epiphany.infra.adapters.mongo :infra)
                          (fixture-inventory 'epiphany.shape.markdown :shape))
          current (merge (fixture-inventory 'epiphany.infra.adapters.mongo :infra :dot-calls 500)
                         (fixture-inventory 'epiphany.shape.markdown :shape :dot-calls 50))]
      (is (empty? (inv/ratchet-violations baseline current #{}))))))

(deftest scan-source-tree-reproducible-test
  (testing "two consecutive scans of the real tree produce identical inventories"
    (is (= (inv/scan-source-tree "src") (inv/scan-source-tree "src")))))

(deftest scan-source-tree-matches-committed-baseline-test
  (testing "the real src tree, scanned today, ratchets clean against the committed baseline"
    (let [baseline-raw (clojure.edn/read-string (slurp inv/baseline-path))
          baseline (into {} (map (fn [[k v]] [k (assoc v :ns k)])) baseline-raw)
          current (inv/scan-source-tree "src")]
      (is (empty? (inv/ratchet-violations baseline current #{}))))))
