(ns epiphany.infra.resource-ingestion-test
  (:require [clio.extern.jvm.fs :as fs]
            [clojure.edn :as edn]
            [clojure.java.io :as io]
            [clojure.string :as str]
            [clojure.test :refer [deftest is]]
            [epiphany.extern.clio-observations :as host]
            [epiphany.extern.resource-selection :as policy-file]
            [epiphany.domain.resource-selection :as selection]
            [epiphany.infra.adapters.clio :as clio]
            [epiphany.infra.adapters.mongo :as mongo]
            [epiphany.infra.git :as git]
            [epiphany.infra.main :as main])
  (:import [org.eclipse.jgit.api Git]))

(defn- commit! [^Git repository message]
  (.call (.addFilepattern (.add repository) "."))
  (.getName (-> (.commit repository)
                (.setAuthor "Epiphany fixture" "fixture@example.invalid")
                (.setCommitter "Epiphany fixture" "fixture@example.invalid")
                (.setMessage message) (.call))))

(defn- with-fixture [f]
  (let [directory (str (System/getProperty "java.io.tmpdir") "/epiphany-resource-" (random-uuid))
        path (str directory "/repo")]
    (try
      (.mkdirs (io/file path))
      (with-open [repository (.call (.setDirectory (Git/init) (io/file path)))]
        (f directory path repository))
      (finally (fs/remove-tree! directory)))))

(defn- policy! [file repo paths]
  (spit file (pr-str {:selection/version 1 :selection/repository repo :selection/paths paths})))

(deftest legacy-default-does-not-retry-or-embed-previously-selected-chat
  (doseq [failed-chat? [false true]]
    (with-fixture
      (fn [directory repo repository]
        (let [file (str directory "/policy.edn") ledger (str directory "/ledger")
              index (str directory "/index") original-read git/read-blob
              reads (atom []) embedded (atom [])]
          (spit (io/file repo "notes.md") "# Historical memory\n\nA synthetic note.\n")
          (spit (io/file repo "chat.json") "{\"schema\":\"epiphany.conversation.v1\",\"messages\":[{\"role\":\"user\",\"text\":\"Synthetic selected chat\"}]}")
          (let [head (commit! repository "Explicit synthetic chat selection")
                chat-oid (some #(when (= "chat.json" (:git/path %)) (:git/blob-oid %))
                               (:entries (git/commit-tree-entries repo head)))]
            (policy! file repo ["notes.md" "chat.json"])
            (with-redefs [host/configured-directory (constantly ledger)]
              (with-redefs [git/read-blob (fn read
                                            ([path oid] (read path oid nil))
                                            ([path oid limit]
                                             (if (and failed-chat? (= chat-oid oid))
                                               {:blob/failure {:failure/reason "synthetic-unavailable"}}
                                               (original-read path oid limit))))]
                (let [result (main/run ["ingest" "--profile" "edn" "--selection-policy" file
                                        "--index-dir" index repo])]
                  (is (= (if failed-chat? 1 0) (:exit result)) (:out result))))
              (let [snapshot ((:export-all (clio/make-observations-adapter (clio/open-store ledger))))]
                (is (= 2 (count (get snapshot "revision-at-path"))))
                (is (= (if failed-chat? 1 2) (count (get snapshot "section-extraction")))))
              (with-redefs-fn
                {#'main/require-ollama! (constantly nil)
                 #'main/make-edn-adapters
                 (let [original #'main/make-edn-adapters
                       make-adapters @original]
                   (fn [index-dir]
                     (assoc (make-adapters index-dir) :embeddings
                            {:embedding-version (constantly 1)
                             :embed-sections! (fn [records]
                                                (swap! embedded into (map :extraction/path-raw records)) [])})))
                 #'git/read-blob (fn read
                                   ([path oid] (read path oid nil))
                                   ([path oid limit]
                                    (swap! reads conj oid)
                                    (original-read path oid limit)))}
                (fn []
                  (let [result (main/run ["ingest" "--profile" "edn" "--embed" "--index-dir" index repo])]
                    (is (= 0 (:exit result)) (:out result)))))
              (is (seq @reads) "The default still rehydrates its admitted Markdown")
              (is (not-any? #{chat-oid} @reads) (str "chat body read after default reuse; failed-chat?=" failed-chat?))
              (is (= ["notes.md"] @embedded))
              (let [snapshot ((:export-all (clio/make-observations-adapter (clio/open-store ledger))))]
                (is (= (if failed-chat? 1 2) (count (get snapshot "section-extraction"))))))))))))

(deftest policy-is-exact-repository-scoped-and-non-evaluating
  (is (not (selection/includes-path? nil nil)) "Missing persisted path metadata fails closed")
  (with-fixture
    (fn [directory repo _]
      (let [file (str directory "/policy.edn")]
        (policy! file repo ["a,b*.json" ".ημ/λ.edn" "a,b*.json"])
        (let [policy (policy-file/read-policy! file repo)]
          (is (= [".ημ/λ.edn" "a,b*.json"] (:selection/paths policy)))
          (is (selection/includes-path? policy "a,b*.json"))
          (is (not (selection/includes-path? policy "other/a,b*.json")))
          (is (not (selection/includes-path? policy "a,b9.json"))))
        (is (thrown? Exception (policy-file/read-policy! file "another-repository")))
        (doseq [value ["#=(System/exit 1)" "#tag/x {}" "{} {}"
                       (pr-str {:selection/version 1 :selection/repository repo :selection/paths ["../escape.json"]})
                       (pr-str {:selection/version 1 :selection/repository repo :selection/paths ["/abs.json"]})]]
          (spit file value)
          (is (thrown? Exception (policy-file/read-policy! file repo))))))))

(deftest invalid-policy-reaches-no-adapter-or-source-body
  (with-fixture
    (fn [directory repo _]
      (let [file (str directory "/policy.edn") reads (atom 0)]
        (policy! file "wrong-repository" ["a.json"])
        (with-redefs [git/read-blob (fn [& _] (swap! reads inc) (throw (ex-info "Unexpected body access" {})))
                      host/configured-directory (fn [] (throw (ex-info "Unexpected adapter startup" {})))]
          (is (= 1 (:exit (main/run ["ingest" "--profile" "edn" "--selection-policy" file repo]))))
          (is (zero? @reads)))))))

(deftest deleted-paths-remain-observations-without-inventing-a-body-at-child
  (with-fixture
    (fn [directory repo repository]
      (let [file (str directory "/policy.edn") ledger (str directory "/ledger")]
        (spit (io/file repo "notes.md") "# Historical idea\n")
        (commit! repository "Before deletion")
        (.call (.addFilepattern (.rm repository) "notes.md"))
        (commit! repository "Delete only the fixture path")
        (policy! file repo ["notes.md"])
        (with-redefs [host/configured-directory (constantly ledger)]
          (let [result (main/run ["ingest" "--profile" "edn" "--selection-policy" file
                                  "--index-dir" (str directory "/index") repo])
                snapshot ((:export-all (clio/make-observations-adapter (clio/open-store ledger))))
                head (git/resolve-commit-oid repo "HEAD")]
            (is (= 0 (:exit result)) (:out result))
            (is (= #{:initial :delete} (set (map :revision/evidence (get snapshot "revision-at-path")))))
            (is (not-any? #(= head (:extraction/commit-oid %)) (get snapshot "section-extraction")))))))))

(deftest accepted-request-cannot-expand-after-head-moves
  (with-fixture
    (fn [directory repo repository]
      (let [file (str directory "/policy.edn") ledger (str directory "/ledger")
            request-id (str (random-uuid))
            args ["ingest" "--profile" "edn" "--request-id" request-id
                  "--selection-policy" file "--index-dir" (str directory "/index") repo]]
        (spit (io/file repo "notes.md") "# First source\n")
        (commit! repository "First")
        (policy! file repo ["notes.md"])
        (with-redefs [host/configured-directory (constantly ledger)]
          (is (= 0 (:exit (main/run args))))
          (spit (io/file repo "notes.md") "# Moved source\n")
          (commit! repository "Move HEAD")
          (let [store (clio/open-store ledger) bytes (fs/read-text (:file store)) reads (atom 0)]
            (with-redefs [git/read-blob (fn [& _] (swap! reads inc) (throw (ex-info "Unexpected body read" {})))]
              (is (= 1 (:exit (main/run args))))
              (is (zero? @reads)))
            (is (= bytes (fs/read-text (:file store))))))))))

(deftest embedding-rehydration-obeys-path-and-ref-scope-in-reused-memory
  (let [resource-id (random-uuid) reads (atom [])
        revisions [{:revision-at-path/id :allowed :revision/path-raw "allowed.md" :revision/commit-oid "current"}
                   {:revision-at-path/id :excluded :revision/path-raw "excluded.md" :revision/commit-oid "current"}
                   {:revision-at-path/id :old-ref :revision/path-raw "allowed.md" :revision/commit-oid "old"}]
        adapters {:observations {:list-revision-at-path-by-resource (constantly revisions)
                                 :list-section-extractions-by-revision (fn [id] [{:extraction/blob-oid (name id)}])
                                 :record-checkpoint! (constantly nil)}
                  :embeddings {:embedding-version (constantly 1)
                               :embed-sections! (fn [records] (is (= 1 (count records))) [])}
                  :index {:index-embeddings! (constantly nil)}}]
    (with-redefs [git/read-blob (fn [_ oid _limit] (swap! reads conj oid) {:blob/content "# Memory\n"})]
      (is (= 0 (#'main/embed-extractions! adapters "/fixture" resource-id (random-uuid) (random-uuid)
                                          {:selection/paths ["allowed.md"]} #{"current"})))
      (is (= ["allowed"] @reads)))))

(deftest source-size-refusal-happens-at-git-loader
  (with-fixture
    (fn [_ repo repository]
      (spit (io/file repo "large.json") (apply str (repeat 100 " ")))
      (let [head (commit! repository "Oversize synthetic resource")
            oid (:git/blob-oid (first (:entries (git/commit-tree-entries repo head))))
            result (git/read-blob repo oid 32)]
        (is (= "blob-too-large" (get-in result [:blob/failure :failure/reason])))
        (is (nil? (:blob/content result)))
        (is (nil? (:blob/bytes result)))))))

(deftest malformed-resource-retry-retains-diagnostic-without-rereading-body
  (with-fixture
    (fn [directory repo repository]
      (let [file (str directory "/policy.edn") ledger (str directory "/ledger")
            args ["ingest" "--profile" "edn" "--request-id" (str (random-uuid))
                  "--selection-policy" file "--index-dir" (str directory "/index") repo]]
        (spit (io/file repo "broken.json") "{not-json}")
        (commit! repository "Malformed inert data fixture")
        (policy! file repo ["broken.json"])
        (with-redefs [host/configured-directory (constantly ledger)]
          (is (= 1 (:exit (main/run args))))
          (let [store (clio/open-store ledger) original (fs/read-text (:file store)) reads (atom 0)]
            (with-redefs [git/read-blob (fn [& _] (swap! reads inc) (throw (ex-info "Unexpected retry read" {})))]
              (is (= 1 (:exit (main/run args))))
              (is (zero? @reads)))
            (is (= original (fs/read-text (:file store))))))))))

(deftest unavailable-ref-refuses-body-reads-and-inspect-accepts-literal-paths
  (with-fixture
    (fn [directory repo repository]
      (let [file (str directory "/policy.edn") path "λ,#@.json"]
        (spit (io/file repo path) "{\"idea\":true}")
        (commit! repository "Literal structured resource")
        (policy! file repo [path])
        (with-redefs [host/configured-directory (constantly (str directory "/ledger"))
                      git/read-blob (fn [& _] (throw (ex-info "Unexpected source body read" {})))]
          (let [result (main/run ["ingest" "--profile" "edn" "--selection-policy" file
                                  "--refs" "refs/heads/absent" "--index-dir" (str directory "/index") repo])]
            (is (= 1 (:exit result)))
            (is (str/includes? (:out result) "no bodies were read"))))
        (let [result (main/run ["inspect" "--repo" repo "--path" path "--commit" "HEAD"])
              value (edn/read-string (:out result))]
          (is (= 0 (:exit result)) (:out result))
          (is (= path (get-in value [:resource/provenance :source/path-raw]))))))))

(deftest selected-ingestion-preserves-typed-chats-retry-and-narrowed-store-boundaries
  (with-fixture
    (fn [directory repo repository]
      (let [file (str directory "/policy.edn") ledger (str directory "/ledger")
            index (str directory "/index") reads (atom []) original-read git/read-blob
            allowed? (atom (constantly true))
            request (str (random-uuid))
            ingest (fn [request-id refs]
                     (main/run ["ingest" "--profile" "edn" "--selection-policy" file
                                "--refs" refs "--request-id" request-id "--index-dir" index repo]))]
        (spit (io/file repo "notes.md") "# Ideas\n\nMemory across history.\n")
        (spit (io/file repo "chat.json") "{\"schema\":\"epiphany.conversation.v1\",\"messages\":[{\"role\":\"user\",\"text\":\"A historical idea\"}]}")
        (spit (io/file repo "excluded.json") "{\"synthetic_canary\":\"DO NOT READ\"}")
        (let [first-commit (commit! repository "First synthetic resource revision")]
          (policy! file repo ["notes.md" "chat.json"])
          (with-redefs [host/configured-directory (constantly ledger)
                        git/read-blob (fn read
                                        ([path oid] (read path oid nil))
                                        ([path oid limit]
                                         (swap! reads conj oid)
                                         (when-not (@allowed? oid) (throw (ex-info "Excluded body read" {})))
                                         (original-read path oid limit)))]
            (let [excluded (some #(when (= "excluded.json" (:git/path %)) (:git/blob-oid %))
                                 (:entries (git/commit-tree-entries repo first-commit)))]
              (reset! allowed? #(not= excluded %))
              (let [result (ingest request "HEAD")] (is (zero? (:exit result)) (:out result)))
              (is (not (some #{excluded} @reads))))
            (let [store (clio/open-store ledger)
                  port (clio/make-observations-adapter store)
                  snapshot ((:export-all port))
                  extraction (some :extraction/resource (get snapshot "section-extraction"))
                  bytes (fs/read-text (:file store))]
              (let [record (some #(when (:extraction/resource %) %) (get snapshot "section-extraction"))
                    run (first (get snapshot "ingestion-run"))]
                (is (= record (mongo/doc->section-extraction (#'mongo/section-extraction->doc record))))
                (is (= run (mongo/doc->ingestion-run (#'mongo/ingestion-run->doc run)))))
              (is (= "user" (get-in extraction [:resource/conversations 0 :conversation/messages 0 :message/role])))
              (is (not (contains? (get-in extraction [:resource/conversations 0 :conversation/messages 0]) :message/timestamp)))
              (is (= "chat.json" (get-in extraction [:resource/provenance :source/path-raw])))
              (let [result (ingest request "HEAD")] (is (zero? (:exit result)) (:out result)))
              (is (= bytes (fs/read-text (:file store))))
              (policy! file repo ["notes.md"])
              (is (= 1 (:exit (ingest request "HEAD"))))
              (is (= bytes (fs/read-text (:file store)))))
            (spit (io/file repo "notes.md") "# Ideas\n\nMemory and awareness evolve.\n")
            (let [head (commit! repository "Second synthetic revision")
                  allowed-oids (set (mapcat (fn [commit]
                                              (keep #(when (= "notes.md" (:git/path %)) (:git/blob-oid %))
                                                    (:entries (git/commit-tree-entries repo commit))))
                                            [head first-commit]))]
              (reset! reads [])
              (reset! allowed? allowed-oids)
              (is (zero? (:exit (ingest (str (random-uuid)) "HEAD"))))
              (is (seq @reads))
              (is (every? allowed-oids @reads))
              (is (= head (git/resolve-commit-oid repo "HEAD")))
              (is (= [first-commit] (:commit/parent-oids (:commit (git/read-commit repo head))))))
            (reset! allowed? (constantly true))
            (let [result (main/run ["inspect" "--repo" repo (str "chat.json@" first-commit)])
                  parsed (edn/read-string (:out result))]
              ;; Explicit evidence retrieval is a separately selected resource.
              (is (= 0 (:exit result)))
              (is (= first-commit (get-in parsed [:resource/provenance :source/commit-oid]))))))))))
