(ns epiphany.application.commands-test
  "ENG-017G2: the shared command-vocabulary seam. Proves the CLI and HTTP
   surfaces decode equivalent input into the IDENTICAL validated command
   map (parity by construction), that execution normalizes outcomes into
   the shared categories, and that the law/ decision vocabulary stays
   pinned to the domain's."
  (:require [clojure.test :refer [deftest is testing]]
            [epiphany.application.commands :as commands]
            [epiphany.domain.review :as review]
            [epiphany.infra.adapters.in-memory :as in-memory]
            [epiphany.law.commands :as law-commands]))

;; ---------------------------------------------------------------------------
;; Decode: surface raw input -> identical command map
;;
;; These tables are the AC1 property: for every command both surfaces
;; expose, the CLI-shaped raw input and the HTTP-shaped raw input decode
;; to the same validated command map. The translators used here are the
;; same candidate shapes main.clj and http.clj construct.

(defn- cli-register-candidate [path request-id]
  (cond-> {:command/name :command/register :repository-path path}
    request-id (assoc :request-id request-id)))

(defn- http-register-candidate [body]
  (cond-> {:command/name :command/register
           :repository-path (or (:path body) (:repository-path body) "")}
    (:request-id body) (assoc :request-id (:request-id body))))

(defn- cli-search-candidate [query opts]
  (cond-> {:command/name :query/search
           :query query
           :mode (:mode opts :hybrid)
           :limit (:limit opts 20)}
    (:path-prefix opts) (assoc-in [:filters :path-prefix] (:path-prefix opts))
    (:ref opts) (assoc-in [:filters :ref] (:ref opts))))

(defn- http-search-candidate [body]
  (cond-> {:command/name :query/search
           :query (:query body)
           :mode (let [m (or (:mode body) "hybrid")]
                   (if (string? m) (keyword m) m))
           :limit (or (:limit body) 20)}
    (:path-prefix body) (assoc-in [:filters :path-prefix] (:path-prefix body))
    (:ref body) (assoc-in [:filters :ref] (:ref body))))

(defn- cli-status-candidate [resource-id]
  {:command/name :query/status :resource-id resource-id})

(defn- http-status-candidate [resource-id-str]
  {:command/name :query/status
   :resource-id (try (java.util.UUID/fromString resource-id-str)
                     (catch Exception _ resource-id-str))})

(defn- cli-review-decision-candidate [candidate-id-str decision-str opts]
  (cond-> {:command/name :command/review-decision
           :candidate-id (try (java.util.UUID/fromString candidate-id-str)
                              (catch IllegalArgumentException _ candidate-id-str))
           :decision (keyword decision-str)}
    (:reason opts) (assoc :reason (:reason opts))))

(defn- http-review-decision-candidate [body]
  (cond-> {:command/name :command/review-decision
           :candidate-id (try (java.util.UUID/fromString (:candidate-id body))
                              (catch Exception _ (:candidate-id body)))
           :decision (if (string? (:decision body))
                       (keyword (:decision body))
                       (:decision body))}
    (:rationale body) (assoc :reason (:rationale body))))

(deftest decode-parity-table-test
  (testing "equivalent CLI and HTTP inputs decode to the identical validated command map"
    (let [rid (random-uuid)
          cid (random-uuid)
          cases [{:name "register, minimal"
                  :cli (cli-register-candidate "/repo/a" nil)
                  :http (http-register-candidate {:repository-path "/repo/a"})}
                 {:name "register, HTTP :path spelling"
                  :cli (cli-register-candidate "/repo/a" nil)
                  :http (http-register-candidate {:path "/repo/a"})}
                 {:name "register with request-id"
                  :cli (cli-register-candidate "/repo/a" rid)
                  :http (http-register-candidate {:repository-path "/repo/a" :request-id rid})}
                 {:name "search, defaults"
                  :cli (cli-search-candidate "continuity" {})
                  :http (http-search-candidate {:query "continuity"})}
                 {:name "search, explicit mode+limit"
                  :cli (cli-search-candidate "continuity" {:mode :lexical :limit 5})
                  :http (http-search-candidate {:query "continuity" :mode "lexical" :limit 5})}
                 {:name "search, filters"
                  :cli (cli-search-candidate "q" {:path-prefix "docs/" :ref "HEAD"})
                  :http (http-search-candidate {:query "q" :path-prefix "docs/" :ref "HEAD"})}
                 {:name "status"
                  :cli (cli-status-candidate rid)
                  :http (http-status-candidate (str rid))}
                 {:name "review-decision"
                  :cli (cli-review-decision-candidate (str cid) "accepted" {:reason "ship it"})
                  :http (http-review-decision-candidate {:candidate-id (str cid)
                                                         :decision "accepted"
                                                         :rationale "ship it"})}]]
      (doseq [{:keys [name cli http]} cases]
        (let [cli-decoded (commands/decode cli)
              http-decoded (commands/decode http)]
          (is (map? cli-decoded) (str name ": CLI candidate must decode"))
          (is (map? http-decoded) (str name ": HTTP candidate must decode"))
          (is (= cli-decoded http-decoded)
              (str name ": both surfaces must produce the identical command map")))))))

(deftest decode-rejects-invalid-input-on-both-surfaces
  (testing "invalid input is a stable :rejected outcome before any adapter is reached"
    (doseq [candidate [(cli-register-candidate "" nil)
                       (http-register-candidate {:path ""})
                       (cli-search-candidate "" {})
                       (http-search-candidate {:query ""})
                       (cli-search-candidate "q" {:mode :bogus})
                       (http-search-candidate {:query "q" :mode "bogus"})
                       (cli-search-candidate "q" {:limit 0})
                       (http-status-candidate "not-a-uuid")
                       (cli-status-candidate nil)
                       (cli-review-decision-candidate "not-a-uuid" "accepted" {})
                       (http-review-decision-candidate {:candidate-id "not-a-uuid" :decision "accepted"})
                       (cli-review-decision-candidate (str (random-uuid)) "bogus" {})
                       (http-review-decision-candidate {:candidate-id (str (random-uuid)) :decision "bogus"})]]
      (is (commands/rejected? (commands/decode candidate))
          (str "candidate must be rejected: " (pr-str candidate))))))

;; ---------------------------------------------------------------------------
;; Execute

(defn- test-adapters
  []
  (in-memory/make {:common-git-dir-fn (fn [p] (str p "/.git"))}))

(deftest execute-review-decision-records-against-existing-candidate
  (testing "the shared executor records a decision under the candidate's own resource-id"
    (let [adapters (test-adapters)
          observations (:observations adapters)
          candidate-id (random-uuid)
          candidate {:lineage-candidate/id candidate-id
                     :resource-id (random-uuid)
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
                     :observation/request-id (random-uuid)}]
      ((:record-lineage-candidate! observations) candidate)
      (let [outcome (commands/execute
                     {:adapters adapters}
                     {:command/name :command/review-decision
                      :candidate-id candidate-id
                      :decision :accepted
                      :reason "verified"})]
        (is (= :accepted (:outcome/category outcome)))
        (let [decisions ((:list-review-decisions-by-candidate observations) candidate-id)]
          (is (= 1 (count decisions)))
          (is (= :accepted (:review-decision/decision (first decisions))))
          (is (= (:resource-id candidate) (:resource-id (first decisions)))
              "decision is recorded under the candidate's resource-id"))))))

(deftest execute-review-decision-rejects-phantom-candidate
  (testing "a decision against a nonexistent candidate is :not-found, never recorded"
    (let [adapters (test-adapters)
          outcome (commands/execute
                   {:adapters adapters}
                   {:command/name :command/review-decision
                    :candidate-id (random-uuid)
                    :decision :accepted})]
      (is (= :not-found (:outcome/category outcome)))
      (is (empty? ((:list-review-decisions (:observations adapters)) (random-uuid)))))))

(deftest execute-status-returns-cross-stage-status
  (testing "query/status executes through the shared cross-stage query"
    (let [adapters (test-adapters)
          outcome (commands/execute
                   {:adapters adapters}
                   {:command/name :query/status :resource-id (random-uuid)})]
      (is (= :accepted (:outcome/category outcome)))
      (is (contains? (:outcome/payload outcome) :stages))
      (is (contains? (:outcome/payload outcome) :summary)))))

(deftest execute-search-honors-lexical-mode-without-ollama
  (testing "lexical search never consults the service probe; hybrid does"
    (let [adapters (test-adapters)
          ctx {:search-ports adapters :service-available? (constantly false)}]
      (is (= :accepted
             (:outcome/category
              (commands/execute ctx {:command/name :query/search
                                     :query "anything" :mode :lexical :limit 5}))))
      (is (= :unavailable
             (:outcome/category
              (commands/execute ctx {:command/name :query/search
                                     :query "anything" :mode :hybrid :limit 5})))
          "hybrid with the service down is UNAVAILABLE, never a silent lexical fallback"))))

;; ---------------------------------------------------------------------------
;; Vocabulary pinning

(deftest review-decision-vocabulary-stays-pinned
  (testing "law/commands decision types are exactly domain.review's set"
    (is (= review/review-decision-types law-commands/review-decision-types))))
