(ns epiphany.infra.warning-gate-test
  (:require [clojure.java.shell :as shell]
            [clojure.string :as string]
            [clojure.test :refer [deftest is]]))

(deftest warning-gate-preserves-output-and-rejects-warning-success
  (doseq [diagnostic ["WARNING: Using incubator modules: jdk.incubator.vector"
                      "WARNING: Java vector incubator module is not readable."
                      "Reflection warning, sample.clj:1 - unresolved method"
                      "[main] WARN sample - unavailable"]
          destination ["1" "2"]]
    (let [result (shell/sh "bash" "bin/with-zero-warnings" "bash" "-c"
                           "printf '%s\\n' \"$1\" >&\"$2\""
                           "warning-fixture" diagnostic destination)]
      (is (= 1 (:exit result)))
      (is (string/includes? (if (= "1" destination) (:out result) (:err result))
                            diagnostic))
      (is (string/includes? (:err result) "Zero-warning gate failed")))))

(deftest warning-gate-keeps-clean-results-and-command-failures
  (let [result (shell/sh "bash" "bin/with-zero-warnings" "bash" "-c"
                         "printf 'ordinary stdout\\n'; printf 'ordinary stderr\\n' >&2; exit 7")]
    (is (= 7 (:exit result)))
    (is (= "ordinary stdout\n" (:out result)))
    (is (= "ordinary stderr\n" (:err result))))
  (let [result (shell/sh "bash" "bin/with-zero-warnings" "bash" "-c"
                         "printf 'PASS: complete\\n'")]
    (is (zero? (:exit result)))
    (is (= "PASS: complete\n" (:out result)))
    (is (= "" (:err result)))))
