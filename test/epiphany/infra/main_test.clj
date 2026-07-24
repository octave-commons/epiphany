(ns epiphany.infra.main-test
  (:require [clojure.java.io]
            [clojure.java.shell]
            [clojure.string :as string]
            [clojure.edn :as edn]
            [clojure.test :refer [deftest is testing]]
            [epiphany.infra.main :as main]
            [epiphany.domain.export :as export]))

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
  (testing "lexical search against a fresh durable index dir returns empty results"
    (let [index-dir (str (java.nio.file.Files/createTempDirectory
                          "epiphany-search-test" (make-array java.nio.file.attribute.FileAttribute 0)))
          {:keys [exit out]} (main/run ["search" "architecture" "--mode" "lexical"
                                        "--index-dir" index-dir])]
      (is (zero? exit))
      (is (string/includes? out "0 results")))))

(deftest search-text-format-default
  (let [index-dir (str (java.nio.file.Files/createTempDirectory
                        "epiphany-search-test" (make-array java.nio.file.attribute.FileAttribute 0)))
        {:keys [exit out]} (main/run ["search" "test" "--mode" "lexical"
                                      "--index-dir" index-dir])]
    (is (zero? exit))
    (is (string/includes? out "results"))))

(deftest search-edn-format
  (let [index-dir (str (java.nio.file.Files/createTempDirectory
                        "epiphany-search-test" (make-array java.nio.file.attribute.FileAttribute 0)))
        {:keys [exit out]} (main/run ["search" "test" "-f" "edn" "--mode" "lexical"
                                      "--index-dir" index-dir])]
    (is (zero? exit))
    (is (or (= "()" out) (string/includes? out "[")))))

(deftest search-json-format
  (let [index-dir (str (java.nio.file.Files/createTempDirectory
                        "epiphany-search-test" (make-array java.nio.file.attribute.FileAttribute 0)))
        {:keys [exit out]} (main/run ["search" "test" "-f" "json" "--mode" "lexical"
                                      "--index-dir" index-dir])]
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
  (let [index-dir (str (java.nio.file.Files/createTempDirectory
                        "epiphany-search-test" (make-array java.nio.file.attribute.FileAttribute 0)))
        {:keys [exit out]} (main/run ["search" "-v" "test" "--mode" "lexical"
                                      "--index-dir" index-dir])]
    (is (zero? exit))
    (is (string/includes? out "Profile:"))))

(deftest search-semantic-requires-ollama-or-fails-explicitly
  (testing "semantic search never silently falls back to lexical"
    (let [index-dir (str (java.nio.file.Files/createTempDirectory
                          "epiphany-search-test" (make-array java.nio.file.attribute.FileAttribute 0)))
          {:keys [exit out]} (main/run ["search" "test" "--mode" "semantic"
                                        "--index-dir" index-dir])]
      ;; With Ollama up this succeeds; with Ollama down it must exit 1 with
      ;; an explicit UNAVAILABLE-style error — never a silent mode change.
      (if (zero? exit)
        (is (string/includes? out "results"))
        (do (is (= 1 exit))
            (is (string/includes? out "Ollama")))))))

;; ---------------------------------------------------------------------------
;; Ingest subcommand

(defn- sh!
  [& command]
  (let [{:keys [exit out err]} (apply clojure.java.shell/sh command)]
    (when-not (zero? exit)
      (throw (ex-info "Fixture command failed" {:command command :err err})))
    out))

(defn- temp-dir
  [prefix]
  (str (java.nio.file.Files/createTempDirectory
        prefix (make-array java.nio.file.attribute.FileAttribute 0))))

(defn- fixture-markdown-repo
  "Create a temp Git repository containing one Markdown file with a unique
   heading. Returns the repository path."
  []
  (let [repo (temp-dir "epiphany-ingest-test")]
    (sh! "git" "init" repo)
    (sh! "git" "-C" repo "config" "user.email" "test@example.invalid")
    (sh! "git" "-C" repo "config" "user.name" "Epiphany Test")
    (spit (clojure.java.io/file repo "notes.md")
          "# Zqxwv Archaeology\n\nContinuity is not identity.\n")
    (sh! "git" "-C" repo "add" "notes.md")
    (sh! "git" "-C" repo "commit" "-m" "add notes")
    repo))

(deftest ingest-requires-path
  (let [{:keys [exit out]} (main/run ["ingest"])]
    (is (= 1 exit))
    (is (string/includes? out "repository path required"))))

(deftest ingest-shows-help
  (let [{:keys [exit out]} (main/run ["ingest" "--help"])]
    (is (zero? exit))
    (is (string/includes? out "Usage: ep ingest"))
    (is (string/includes? out "--index-dir"))))

(deftest ingest-rejects-invalid-profile
  (let [{:keys [exit out]} (main/run ["ingest" "-p" "nope" "."])]
    (is (= 1 exit))
    (is (string/includes? out "invalid profile"))))

(deftest ingest-fails-on-non-git-path
  (let [{:keys [exit out]} (main/run ["ingest" (temp-dir "epiphany-not-git")
                                      "-p" "local"
                                      "--index-dir" (temp-dir "epiphany-idx")])]
    (is (= 1 exit))
    (is (string/includes? out "Not a Git repository"))))

(deftest ingest-local-then-lexical-search-finds-section
  (testing "ep ingest populates the durable Lucene index ep search reads"
    (let [repo (fixture-markdown-repo)
          index-dir (temp-dir "epiphany-idx")
          ingest (main/run ["ingest" repo "-p" "local" "--index-dir" index-dir])]
      (is (zero? (:exit ingest)) (:out ingest))
      (is (string/includes? (:out ingest) "Sections extracted:"))
      (let [{:keys [exit out]} (main/run ["search" "Zqxwv" "--mode" "lexical"
                                          "--index-dir" index-dir])]
        (is (zero? exit))
        (is (string/includes? out "notes.md"))))))

(deftest ingest-non-ascii-body-is-fully-searchable
  (testing "non-ASCII Markdown extracts and indexes end-to-end (blocker regression, ENG-003G review)"
    (let [repo (temp-dir "epiphany-ingest-unicode")
          index-dir (temp-dir "epiphany-idx")]
      (sh! "git" "init" repo)
      (sh! "git" "-C" repo "config" "user.email" "test@example.invalid")
      (sh! "git" "-C" repo "config" "user.name" "Epiphany Test")
      (spit (clojure.java.io/file repo "u.md")
            "# Café notes\n\nThe naïve zqxwvuniq body lives here.\n")
      (sh! "git" "-C" repo "add" "u.md")
      (sh! "git" "-C" repo "commit" "-m" "add unicode notes")
      (let [ingest (main/run ["ingest" repo "-p" "local" "--index-dir" index-dir])]
        (is (zero? (:exit ingest)) (:out ingest))
        (is (string/includes? (:out ingest) "Sections extracted:  1")
            (str "non-ASCII documents must not silently fail extraction: " (:out ingest)))
        (is (string/includes? (:out ingest) "Extraction failures: 0")))
      (let [{:keys [exit out]} (main/run ["search" "zqxwvuniq" "--mode" "lexical"
                                          "--index-dir" index-dir])]
        (is (zero? exit))
        (is (string/includes? out "u.md")
            "body text after non-ASCII characters must be searchable")))))

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

(deftest show-surfaces-commit-author-committer-and-parent
  (testing "show against a real commit surfaces author/committer identity and parent OID(s), per AC1/AC3"
    (let [{:keys [exit out]} (main/run ["show" "AGENTS.md@HEAD"])]
      (is (zero? exit))
      (is (string/includes? out "Author:"))
      (is (string/includes? out "Committer:"))
      (is (string/includes? out "Parent(s):")))))

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

(deftest diff-seed-candidate-records-a-provisional-candidate
  (testing "--seed-candidate durably records a provisional lineage candidate via the observations port"
    (let [{:keys [exit out]} (main/run ["diff" "--seed-candidate" "continues"
                                        "AGENTS.md@HEAD~3" "AGENTS.md@HEAD"])]
      (is (zero? exit))
      (is (string/includes? out "Seeded candidate "))
      (is (string/includes? out "[continues, provisional]")))))

(deftest diff-seed-candidate-rejects-invalid-relation
  (let [{:keys [exit out]} (main/run ["diff" "--seed-candidate" "not-a-real-relation"
                                      "AGENTS.md@HEAD~3" "AGENTS.md@HEAD"])]
    (is (= 1 exit))
    (is (string/includes? out "Must be one of"))))

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
      (is (string/includes? out "observed"))
      (is (string/includes? out "ep show AGENTS.md@")))))

(deftest trace-observed-only-flag-is-accepted
  (let [{:keys [exit out]} (main/run ["trace" "--observed-only" "AGENTS.md"])]
    (is (zero? exit))
    (is (string/includes? out "observed-only"))))

(deftest trace-reports-error-for-untracked-path
  (let [{:keys [exit out]} (main/run ["trace" "no/such/path.md"])]
    (is (= 1 exit))
    (is (string/includes? out "no revisions found"))))

;; ---------------------------------------------------------------------------
;; Inbox subcommand

(deftest inbox-shows-help
  (let [{:keys [exit out]} (main/run ["inbox" "--help"])]
    (is (zero? exit))
    (is (string/includes? out "Usage: ep inbox"))))

(deftest inbox-empty-store-reports-no-candidates
  (let [{:keys [exit out]} (main/run ["inbox"])]
    (is (zero? exit))
    (is (string/includes? out "No unreviewed candidates."))))

(deftest inbox-decide-shows-help
  (let [{:keys [exit out]} (main/run ["inbox" "decide" "--help"])]
    (is (zero? exit))
    (is (string/includes? out "Usage: ep inbox decide"))))

(deftest inbox-decide-requires-candidate-id-and-decision
  (let [{:keys [exit out]} (main/run ["inbox" "decide"])]
    (is (= 1 exit))
    (is (string/includes? out "candidate id and decision required"))))

(deftest inbox-decide-rejects-invalid-decision-type
  (let [{:keys [exit out]} (main/run ["inbox" "decide" (str (random-uuid)) "not-a-real-decision"])]
    (is (= 1 exit))
    (is (string/includes? out "decision must be one of"))))

(deftest inbox-decide-rejects-invalid-candidate-id
  (let [{:keys [exit out]} (main/run ["inbox" "decide" "not-a-uuid" "accepted"])]
    (is (= 1 exit))
    (is (string/includes? out "invalid candidate id"))))

(deftest inbox-decide-records-a-real-decision
  (testing "a valid decide call durably records through the observations port and confirms it"
    (let [{:keys [exit out]} (main/run ["inbox" "decide" (str (random-uuid)) "rejected"])]
      (is (zero? exit))
      (is (string/includes? out "Recorded rejected for candidate")))))

;; ---------------------------------------------------------------------------
;; Export subcommand

(deftest export-shows-help
  (let [{:keys [exit out]} (main/run ["export" "--help"])]
    (is (zero? exit))
    (is (string/includes? out "Usage: ep export"))))

(deftest export-produces-a-real-packet-with-content-hash
  (testing "markdown export of an empty store still produces a real, tamper-evident packet"
    (let [{:keys [exit out]} (main/run ["export"])]
      (is (zero? exit))
      (is (string/includes? out "# Evidence Packet"))
      (is (string/includes? out "Content-hash:")))))

(deftest export-edn-format-round-trips-through-content-hash-check
  (testing "the EDN packet's content-hash is independently verifiable"
    (let [{:keys [exit out]} (main/run ["export" "--format" "edn"])
          packet (edn/read-string out)]
      (is (zero? exit))
      (is (export/content-hash-valid? packet)))))

(deftest export-rejects-invalid-format
  (let [{:keys [exit out]} (main/run ["export" "--format" "yaml"])]
    (is (= 1 exit))
    (is (string/includes? out "Must be markdown, edn, or json"))))
