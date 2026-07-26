(ns epiphany.review-feedback-regression-test
  (:require [clojure.java.io :as io]
            [clojure.java.shell :as shell]
            [clojure.string :as string]
            [clojure.test :refer [deftest is testing]]
            [epiphany.application.commands :as commands]
            [epiphany.domain.extraction-projection :as extraction]
            [epiphany.domain.revision-at-path :as revision-at-path]
            [epiphany.infra.adapters.in-memory :as in-memory]
            [epiphany.infra.adapters.lucene :as lucene]
            [epiphany.infra.git :as git]
            [epiphany.infra.http])
  (:import [java.io ByteArrayInputStream]
           [java.nio.file Files]
           [java.nio.file.attribute FileAttribute]))

(defn- delete-recursive!
  [^java.io.File file]
  (when (.exists file)
    (when (.isDirectory file)
      (doseq [child (.listFiles file)]
        (delete-recursive! child)))
    (io/delete-file file true)))

(defn- temp-dir
  [prefix]
  (Files/createTempDirectory prefix (into-array FileAttribute [])))

(defn- valid-candidate
  [candidate-id resource-id request-id]
  {:lineage-candidate/id candidate-id
   :resource-id resource-id
   :lineage-candidate/relation :continues
   :lineage-candidate/tier :provisional
   :lineage-candidate/confidence (double 0.9)
   :lineage-candidate/generator-version "test-v1"
   :lineage-candidate/generated-at (java.util.Date.)
   :lineage-candidate/source {:span/path-raw "a.md"
                              :span/heading-path ["A"]
                              :span/commit-oid "0123456789abcdef0123456789abcdef01234567"}
   :lineage-candidate/target {:span/path-raw "b.md"
                              :span/heading-path ["B"]
                              :span/commit-oid "fedcba9876543210fedcba9876543210fedcba98"}
   :observation/id (random-uuid)
   :observation/type :lineage/candidate-generated
   :observation/observed-at (java.util.Date.)
   :observation/adapter-version "test"
   :observation/schema-version 1
   :observation/request-id request-id})

(deftest review-replay-returns-the-persisted-decision
  (testing "same request returns the durable decision and conflicting reuse is rejected"
    (let [adapters (in-memory/make {:common-git-dir-fn identity})
          observations (:observations adapters)
          candidate-id (random-uuid)
          resource-id (random-uuid)
          request-id (random-uuid)
          candidate (valid-candidate candidate-id resource-id (random-uuid))
          command {:command/name :command/review-decision
                   :candidate-id candidate-id
                   :decision :accepted
                   :request-id request-id
                   :reason "verified"}]
      ((:record-lineage-candidate! observations) candidate)
      (let [first-outcome (commands/execute {:adapters adapters} command)
            replay-outcome (commands/execute {:adapters adapters} command)
            conflict-outcome (commands/execute {:adapters adapters}
                                               (assoc command :decision :rejected))
            first-decision (get-in first-outcome [:outcome/payload :decision])
            replay-decision (get-in replay-outcome [:outcome/payload :decision])]
        (is (= :accepted (:outcome/category first-outcome)))
        (is (= :accepted (:outcome/category replay-outcome)))
        (is (= (:review-decision/id first-decision)
               (:review-decision/id replay-decision)))
        (is (= (:review-decision/decided-at first-decision)
               (:review-decision/decided-at replay-decision)))
        (is (= 1 (count ((:list-review-decisions-by-candidate observations)
                         candidate-id))))
        (is (= :rejected (:outcome/category conflict-outcome)))))))

(deftest extraction-replay-converges-in-observations-and-lucene
  (testing "same ingest request cannot append duplicate extraction or section documents"
    (let [dir (temp-dir "epiphany-extraction-replay")
          adapters (in-memory/make {:common-git-dir-fn identity})
          observations (:observations adapters)
          index (lucene/make-index-adapter {:index-dir dir})
          resource-id (random-uuid)
          revision-id (random-uuid)
          request-id (random-uuid)
          source "# One\n\nDurable body.\n"
          revision {:resource-id resource-id
                    :revision-at-path/id revision-id
                    :revision/commit-oid "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa"
                    :revision/path-raw "docs/one.md"
                    :revision/blob-oid "bbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbb"}
          ports {:git {:read-blob (fn [_ _] {:blob/content source})}
                 :observations observations
                 :index index}]
      (try
        (is (some? (:extraction/record
                    (extraction/extract-revision ports request-id revision))))
        (is (some? (:extraction/record
                    (extraction/extract-revision ports request-id revision))))
        (is (= 1 (count ((:list-section-extractions-by-revision observations)
                         revision-id))))
        (is (= {:document-count 1} ((:index-stats index) resource-id)))
        (finally
          (delete-recursive! (.toFile dir)))))))

(deftest revisions-for-commit-emits-deletion-observations
  (testing "selected parent markdown paths absent from the child are durable deletes"
    (let [resource-id (random-uuid)
          child-oid "cccccccccccccccccccccccccccccccccccccccc"
          parent-oid "dddddddddddddddddddddddddddddddddddddddd"
          keep-blob "1111111111111111111111111111111111111111"
          gone-blob "2222222222222222222222222222222222222222"
          entries [{:entry/commit-oid child-oid
                    :entry/path-raw "keep.md"
                    :entry/blob-oid keep-blob
                    :entry/mode 33188
                    :entry/policy-version "markdown-tree-v1"}]
          selected-parent-entries
          [{:entry/commit-oid parent-oid
            :entry/path-raw "keep.md"
            :entry/blob-oid keep-blob
            :entry/mode 33188
            :entry/policy-version "markdown-tree-v1"}
           {:entry/commit-oid parent-oid
            :entry/path-raw "gone.md"
            :entry/blob-oid gone-blob
            :entry/mode 33188
            :entry/policy-version "markdown-tree-v1"}]
          parent-entries [{:git/path "keep.md" :git/blob-oid keep-blob :git/mode 33188}
                          {:git/path "gone.md" :git/blob-oid gone-blob :git/mode 33188}]
          observations
          (revision-at-path/revisions-for-commit
           entries
           {:resource-id resource-id
            :commit-oid child-oid
            :tree-oid "eeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeee"
            :parent-commit-oid parent-oid
            :parent-entries parent-entries
            :selected-parent-entries selected-parent-entries
            :observed-at #inst "2026-07-25T00:00:00Z"})
          deletion (first (filter #(= "gone.md" (:revision/path-raw %))
                                  observations))]
      (is (= 2 (count observations)))
      (is (= :delete (:revision/evidence deletion)))
      (is (= child-oid (:revision/commit-oid deletion)))
      (is (= gone-blob (:revision/blob-oid deletion)))
      (is (= gone-blob (:revision/parent-blob-oid deletion))))))

(deftest urlencoded-form-bodies-are-decoded-before-dispatch
  (testing "ordinary HTMX form payloads become keyword-keyed body params"
    (let [read-body (var-get (ns-resolve 'epiphany.infra.http 'read-body))
          body "resource-id=00000000-0000-0000-0000-000000000001&reason=needs+review"
          request {:body (ByteArrayInputStream. (.getBytes body "UTF-8"))
                   :headers {"content-type" "application/x-www-form-urlencoded"}}]
      (is (= {:resource-id "00000000-0000-0000-0000-000000000001"
              :reason "needs review"}
             (read-body request))))))

(deftest stale-lucene-schema-requires-an-explicit-rebuild
  (testing "append rejects stale data while rebuild replaces it with the current schema"
    (let [dir (temp-dir "epiphany-lucene-version")
          adapter (lucene/make-index-adapter {:index-dir dir})
          record {:resource-id (random-uuid)
                  :extraction/revision-at-path-id (random-uuid)
                  :extraction/commit-oid "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa"
                  :extraction/path-raw "docs/version.md"
                  :extraction/blob-oid "bbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbb"
                  :extraction/extractor-version "test-v1"
                  :extraction/sections [{:section/heading-path ["Version"]
                                         :section/level 1
                                         :section/ordinal 0
                                         :section/heading-span-start-byte 0
                                         :section/heading-span-end-byte 9
                                         :section/body-span-start-byte 10
                                         :section/body-span-end-byte 15
                                         :section/body-span-start-line 2
                                         :section/body-span-end-line 2}]
                  :extraction/content "# Version\n\nBody\n"}]
      (try
        ((:index-sections! adapter) record)
        (spit (.toFile (.resolve dir "index-version.edn"))
              (pr-str {:index/version 3}))
        (let [error (try
                      ((:index-sections! adapter) record)
                      nil
                      (catch clojure.lang.ExceptionInfo e e))]
          (is (= :index-version-mismatch (:code (ex-data error)))))
        ((:rebuild-index! adapter) [record])
        (is (= 4 ((:index-version adapter))))
        (is (= 1 (count ((:search adapter) "Version"))))
        (finally
          (delete-recursive! (.toFile dir)))))))

(deftest commit-resolution-does-not-shell-out
  (testing "JGit resolves commit expressions even when shell execution is disabled"
    (let [dir (temp-dir "epiphany-jgit-resolve")
          repo (.toFile dir)]
      (try
        (doseq [command [["git" "init" (.getPath repo)]
                         ["git" "-C" (.getPath repo) "config" "user.email" "test@example.invalid"]
                         ["git" "-C" (.getPath repo) "config" "user.name" "Epiphany Test"]]]
          (let [{:keys [exit err]} (apply shell/sh command)]
            (is (zero? exit) err)))
        (spit (io/file repo "README.md") "# Fixture\n")
        (is (zero? (:exit (shell/sh "git" "-C" (.getPath repo) "add" "README.md"))))
        (is (zero? (:exit (shell/sh "git" "-C" (.getPath repo) "commit" "-m" "fixture"))))
        (let [expected (string/trim
                        (:out (shell/sh "git" "-C" (.getPath repo) "rev-parse" "HEAD")))]
          (with-redefs [shell/sh (fn [& _]
                                  (throw (ex-info "shell disabled" {})))]
            (is (= expected (git/resolve-commit-oid (.getPath repo) "HEAD")))))
        (finally
          (delete-recursive! repo))))))
