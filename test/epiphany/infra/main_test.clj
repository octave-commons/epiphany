(ns epiphany.infra.main-test
  (:require [clojure.string :as string]
            [clojure.test :refer [deftest is testing]]
            [epiphany.infra.main :as main]))

(deftest help-identifies-the-canonical-executable
  (testing "--help succeeds and names both epiphany and its ep alias"
    (let [{:keys [exit out]} (main/run ["--help"])]
      (is (zero? exit))
      (is (string/includes? out "epiphany"))
      (is (string/includes? out "ep ")))))

(deftest no-arguments-prints-usage
  (let [{:keys [exit out]} (main/run [])]
    (is (zero? exit))
    (is (string/includes? out "Usage:"))))

(deftest version-reports-the-executable-name-and-version
  (let [{:keys [exit out]} (main/run ["--version"])]
    (is (zero? exit))
    (is (= (str "epiphany " main/version) out))))

(deftest unknown-option-fails-with-usage
  (let [{:keys [exit out]} (main/run ["--no-such-option"])]
    (is (= 1 exit))
    (is (string/includes? out "Usage:"))))
