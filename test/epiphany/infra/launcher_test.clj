(ns epiphany.infra.launcher-test
  "Guard against the bin/epiphany launcher regressing to a broken state.

  Regression history (ENG-017M): commit 0a99597 shipped a launcher that
  exec'd itself (`exec \"$script_dir/epiphany\"`) — an infinite loop that
  never invoked clojure, so the documented `bin/ep` entrypoint hung and
  produced no output. This is a cheap static guard: it does not boot the JVM,
  it asserts the launcher script is structurally sane."
  (:require [clojure.test :refer [deftest is testing]]
            [clojure.java.io :as io]
            [clojure.string :as string]))

(def ^:private launcher-file (io/file "bin/epiphany"))

(deftest launcher-invokes-clojure
  (testing "the launcher actually runs the program via clojure, not itself"
    (is (.exists launcher-file)
        "bin/epiphany must exist")
    (let [script (slurp launcher-file)]
      (is (re-find #"\bclojure\b" script)
          "launcher must invoke `clojure` (the 0a99597 regression did not)")
      (is (re-find #"-M:run" script)
          "launcher must run the :run alias (ADR-003 unified executable)")
      (is (not (re-find #"exec\s+\"\$script_dir/epiphany\"" script))
          "launcher must not exec itself — that is the infinite-loop regression"))))
