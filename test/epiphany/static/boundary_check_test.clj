(ns epiphany.static.boundary-check-test
  (:require [clojure.test :refer [deftest testing is]]
            [epiphany.static.boundary-check :as bc]))

(deftest quadrant-of-test
  (testing "recognizes the five quadrants and ignores everything else"
    (is (= :law (bc/quadrant-of 'epiphany.law.ports)))
    (is (= :shape (bc/quadrant-of 'epiphany.shape.markdown)))
    (is (= :domain (bc/quadrant-of 'epiphany.domain.status)))
    (is (= :application (bc/quadrant-of 'epiphany.application.registration)))
    (is (= :infra (bc/quadrant-of 'epiphany.infra.http)))
    (is (nil? (bc/quadrant-of 'clojure.core)))
    (is (nil? (bc/quadrant-of 'epiphany.integration.foo)))))

(deftest find-violations-clean-graph-test
  (testing "a graph respecting the layer laws reports no violations"
    (is (empty? (bc/find-violations
                 {'epiphany.law.ports #{}
                  'epiphany.shape.markdown #{'epiphany.law.ports}
                  'epiphany.domain.status #{'epiphany.law.ports 'epiphany.shape.markdown}
                  'epiphany.application.registration #{'epiphany.domain.status}
                  'epiphany.infra.http #{'epiphany.application.registration
                                         'epiphany.domain.status
                                         'epiphany.infra.profile}})))))

(deftest find-violations-law-requiring-domain-test
  (testing "law.* requiring domain.* is a violation — law must be usable without infrastructure"
    (let [violations (bc/find-violations
                       {'epiphany.law.tainted #{'epiphany.domain.status}})]
      (is (= 1 (count violations)))
      (is (= 'epiphany.law.tainted (:ns (first violations))))
      (is (= :domain (:offending-quadrant (first violations)))))))

(deftest find-violations-domain-requiring-infra-test
  (testing "domain.* requiring infra.* is a violation — domain must stay adapter-free"
    (let [violations (bc/find-violations
                       {'epiphany.domain.tainted #{'epiphany.infra.adapters.mongo}})]
      (is (= 1 (count violations)))
      (is (= :infra (:offending-quadrant (first violations)))))))

(deftest find-violations-application-requiring-infra-test
  (testing "application.* requiring infra.* is a violation — it must not become a transport adapter"
    (let [violations (bc/find-violations
                       {'epiphany.application.tainted #{'epiphany.infra.git}})]
      (is (= 1 (count violations)))
      (is (= :infra (:offending-quadrant (first violations)))))))

(deftest find-violations-shape-requiring-domain-test
  (testing "shape.* requiring domain.* is a violation — shape must not decide domain meaning"
    (let [violations (bc/find-violations
                       {'epiphany.shape.tainted #{'epiphany.domain.status}})]
      (is (= 1 (count violations)))
      (is (= :domain (:offending-quadrant (first violations)))))))

(deftest find-violations-infra-may-require-anything-test
  (testing "infra.* may require any quadrant, including other infra.*"
    (is (empty? (bc/find-violations
                 {'epiphany.infra.main #{'epiphany.infra.adapters.mongo
                                         'epiphany.domain.status
                                         'epiphany.law.ports}})))))

(deftest scan-source-tree-current-repo-is-clean-test
  (testing "the real src tree, scanned today, has no layer-boundary violations"
    (is (empty? (bc/find-violations (bc/scan-source-tree "src"))))))
