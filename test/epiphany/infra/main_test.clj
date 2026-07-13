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

(deftest check-services-reports-readiness
  (let [{:keys [exit out]} (main/run ["--check-services"])]
    (is (contains? #{0 1} exit))
    (is (string/includes? out "Service readiness:"))))

(deftest unknown-command-fails
  (let [{:keys [exit out]} (main/run ["bogus"])]
    (is (= 1 exit))
    (is (string/includes? out "Unknown command"))))

(deftest register-requires-path
  (let [{:keys [exit out]} (main/run ["register"])]
    (is (= 1 exit))
    (is (string/includes? out "repository path required"))))

(deftest register-rejects-invalid-profile
  (let [{:keys [exit out]} (main/run ["register" "-p" "nope" "/some/path"])]
    (is (= 1 exit))
    (is (string/includes? out "invalid profile"))))

(deftest register-local-mode-succeeds-on-this-repo
  (testing "register --profile :local on the epiphany repo itself succeeds"
    (let [{:keys [exit out]} (main/run ["register" "-p" :local "."])]
      (is (zero? exit))
      (is (string/includes? out "Registered:"))
      (is (string/includes? out "Resource ID:")))))

(deftest register-local-mode-fails-on-non-git-path
  (let [{:keys [exit out]} (main/run ["register" "-p" :local "/tmp"])]
    (is (= 1 exit))
    (is (string/includes? out "Error:"))))

(deftest register-with-request-id-is-idempotent
  (testing "registering with a request-id includes it in the output"
    (let [rid (str (java.util.UUID/randomUUID))
          {:keys [exit out]} (main/run ["register" "-r" rid "-p" :local "."])]
      (is (zero? exit))
      (is (string/includes? out "Resource ID:"))
      (is (string/includes? out (str "Request ID:      " rid))))))

(deftest status-requires-resource-id
  (let [{:keys [exit out]} (main/run ["status"])]
    (is (= 1 exit))
    (is (string/includes? out "resource-id required"))))

(deftest status-rejects-invalid-profile
  (let [{:keys [exit out]} (main/run ["status" "-p" "noke" "-r" (str (java.util.UUID/randomUUID))])]
    (is (= 1 exit))
    (is (string/includes? out "invalid profile"))))

(deftest status-local-profile-rejected
  (testing "status with :local profile reports not supported"
    (let [{:keys [exit out]} (main/run ["status" "-p" :local "-r" (str (java.util.UUID/randomUUID))])]
      (is (= 1 exit))
      (is (string/includes? out "does not persist")))))

(deftest status-services-profile-requires-mongo
  (testing "status with :services fails when MongoDB is unavailable"
    (let [{:keys [exit out]} (main/run ["status" "-p" :services "-r" (str (java.util.UUID/randomUUID))])]
      ;; Either succeeds (mongo is up) or fails with connection error
      (is (contains? #{0 1} exit))
      (is (or (string/includes? out "No ingestion runs")
              (string/includes? out "Error:")
              (string/includes? out "Cannot connect"))))))

;; ---------------------------------------------------------------------------
;; Search subcommand

(deftest search-requires-query
  (let [{:keys [exit out]} (main/run ["search"])]
    (is (= 1 exit))
    (is (string/includes? out "search query required"))))

(deftest search-shows-help
  (let [{:keys [exit out]} (main/run ["search" "--help"])]
    (is (zero? exit))
    (is (string/includes? out "Usage: ep search"))
    (is (string/includes? out "--mode"))
    (is (string/includes? out "--format"))))

(deftest search-returns-zero-results-on-empty-index
  (testing "search with in-memory adapters returns empty results"
    (let [{:keys [exit out]} (main/run ["search" "architecture"])]
      (is (zero? exit))
      (is (string/includes? out "0 results")))))

(deftest search-text-format-default
  (let [{:keys [exit out]} (main/run ["search" "test"])]
    (is (zero? exit))
    (is (string/includes? out "results"))))

(deftest search-edn-format
  (let [{:keys [exit out]} (main/run ["search" "test" "-f" "edn"])]
    (is (zero? exit))
    (is (or (= "()" out) (string/includes? out "[")))))

(deftest search-json-format
  (let [{:keys [exit out]} (main/run ["search" "test" "-f" "json"])]
    (is (zero? exit))
    (is (= "[]" out))))

(deftest search-rejects-invalid-mode
  (let [{:keys [exit out]} (main/run ["search" "-m" "bogus" "test"])]
    (is (= 1 exit))
    (is (string/includes? out "Must be lexical, semantic, or hybrid"))))

(deftest search-rejects-invalid-format
  (let [{:keys [exit out]} (main/run ["search" "-f" "xml" "test"])]
    (is (= 1 exit))
    (is (string/includes? out "Must be text, edn, or json"))))

(deftest search-rejects-invalid-profile
  (let [{:keys [exit out]} (main/run ["search" "-p" "nope" "test"])]
    (is (= 1 exit))
    (is (string/includes? out "invalid profile"))))

(deftest search-verbose-mode
  (let [{:keys [exit out]} (main/run ["search" "-v" "test"])]
    (is (zero? exit))
    (is (string/includes? out "Profile:"))))

;; ---------------------------------------------------------------------------
;; Show subcommand

(deftest show-requires-expression
  (let [{:keys [exit out]} (main/run ["show"])]
    (is (= 1 exit))
    (is (string/includes? out "section expression required"))))

(deftest show-shows-help
  (let [{:keys [exit out]} (main/run ["show" "--help"])]
    (is (zero? exit))
    (is (string/includes? out "Usage: ep show"))))

(deftest show-retrieves-real-evidence-from-this-repo
  (testing "show against a tracked file at HEAD in this repo returns real Git blob content"
    (let [{:keys [exit out]} (main/run ["show" "AGENTS.md@HEAD"])]
      (is (zero? exit))
      (is (string/includes? out "--- Source: AGENTS.md"))
      (is (string/includes? out "Epiphany")))))

(deftest show-reports-unavailable-for-missing-path
  (let [{:keys [exit out]} (main/run ["show" "no/such/path.md@HEAD"])]
    (is (= 1 exit))
    (is (string/includes? out "UNAVAILABLE"))))

;; ---------------------------------------------------------------------------
;; Diff subcommand

(deftest diff-requires-two-expressions
  (let [{:keys [exit out]} (main/run ["diff" "AGENTS.md@HEAD"])]
    (is (= 1 exit))
    (is (string/includes? out "exactly two section expressions"))))

(deftest diff-shows-help
  (let [{:keys [exit out]} (main/run ["diff" "--help"])]
    (is (zero? exit))
    (is (string/includes? out "Usage: ep diff"))))

(deftest diff-compares-real-revisions-in-this-repo
  (testing "diff between two real HEAD-relative revisions of the same tracked file"
    (let [{:keys [exit out]} (main/run ["diff" "AGENTS.md@HEAD~3" "AGENTS.md@HEAD"])]
      (is (zero? exit))
      (is (string/includes? out "--- AGENTS.md@HEAD~3"))
      (is (string/includes? out "+++ AGENTS.md@HEAD"))
      (is (string/includes? out "Continuity"))
      (is (string/includes? out "Summary")))))

;; ---------------------------------------------------------------------------
;; Trace subcommand

(deftest trace-requires-path
  (let [{:keys [exit out]} (main/run ["trace"])]
    (is (= 1 exit))
    (is (string/includes? out "path required"))))

(deftest trace-shows-help
  (let [{:keys [exit out]} (main/run ["trace" "--help"])]
    (is (zero? exit))
    (is (string/includes? out "Usage: ep trace"))))

(deftest trace-walks-real-history-in-this-repo
  (testing "trace against a tracked file's real Git history produces observed edges"
    (let [{:keys [exit out]} (main/run ["trace" "AGENTS.md"])]
      (is (zero? exit))
      (is (string/includes? out "node(s)"))
      (is (string/includes? out "observed")))))

(deftest trace-observed-only-flag-is-accepted
  (let [{:keys [exit out]} (main/run ["trace" "--observed-only" "AGENTS.md"])]
    (is (zero? exit))
    (is (string/includes? out "observed-only"))))

(deftest trace-reports-error-for-untracked-path
  (let [{:keys [exit out]} (main/run ["trace" "no/such/path.md"])]
    (is (= 1 exit))
    (is (string/includes? out "no revisions found"))))
