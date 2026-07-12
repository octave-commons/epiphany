(ns epiphany.law.observation
  "Malli contracts for append-only platform observation records (ADR-001).

  An observation is an immutable fact the platform recorded: it is never
  updated or deleted; corrections append new observations. Each record is
  a closed map with two structural regions:

  - envelope keys (`:observation/*`, `:resource-id`) — platform
    provenance: who observed, when, with which adapter and contract
    version, under which idempotent request;
  - observed values — Git facts nested under `:git/observed`
    (see `epiphany.law.git`) and exact observed paths as `path/observed`
    provenance maps.

  Paths are preserved byte-for-byte as observed (ADR-001 §5). The closed
  `path/observed` map rejects any normalized/rewritten variant keys
  (`:path/normalized`, `:path/ascii`, ...), and `:path/comparison` is
  pinned to `:exact` — see `exact-path?` for the comparison contract.

  Record contracts are versioned in their registry name; instances carry
  the same version in `:observation/schema-version`. Changing a contract
  means a new versioned name, not an edit.")

(def path-raw-pattern
  "Non-empty, no NUL. Deliberately permissive: exact preservation means
  the schema must not reject unusual-but-observed spellings."
  "^[^\u0000]+$")

(def observation-envelope-entries
  "Platform provenance required on every observation record."
  [[:observation/id :uuid]
   ;; quoted symbol, not the function: keeps the registry EDN-serializable
   [:observation/observed-at 'inst?]
   [:observation/adapter-version [:string {:min 1}]]
   [:observation/schema-version [:int {:min 1}]]])

(def schemas
  {"path/raw" [:re path-raw-pattern]

   ;; An exact observed path with its provenance.
   "path/observed" [:map {:closed true}
                    [:path/raw [:ref "path/raw"]]
                    [:path/source [:enum :git-tree-entry :filesystem-argument]]
                    [:path/comparison [:= :exact]]]

   ;; Registration of a repository instance (ADR-001 §1, §3).
   "observation/registration-v1"
   (into [:map {:closed true}
          [:observation/type [:= :repository/registered]]
          [:observation/request-id :uuid]]
         (into observation-envelope-entries
               [[:resource-id :uuid]
                [:repository/path [:ref "path/observed"]]
                [:repository/common-git-dir [:ref "path/observed"]]
                [:repository/identity-persistence [:enum :git-local :external]]
                [:family/assessment [:enum :pending :new-family :joined-existing]]
                [:family/id {:optional true} :uuid]]))

   ;; A repository instance seen at a filesystem location (owned here;
   ;; persisted by ENG-001A).
   "observation/repository-location-v1"
   (into [:map {:closed true}
          [:observation/type [:= :repository/location-observed]]
          [:observation/request-id :uuid]]
         (into observation-envelope-entries
               [[:resource-id :uuid]
                [:repository/path [:ref "path/observed"]]
                [:repository/common-git-dir [:ref "path/observed"]]]))

    ;; One Git ref value observed on a registered instance. Request ID is
   ;; optional: ref observations may arrive from ingestion runs, not only
   ;; from user commands.
   "observation/git-ref-v1"
   (into [:map {:closed true}
          [:observation/type [:= :git/ref-observed]]
          [:observation/request-id {:optional true} :uuid]]
         (into observation-envelope-entries
               [[:resource-id :uuid]
                [:git/observed [:ref "git/ref"]]]))

    ;; A Git person identity: name, email, and timestamp as observed.
    "git/person" [:map {:closed true}
                  [:person/name [:string {:min 1}]]
                  [:person/email [:string {:min 1}]]
                  [:person/timestamp 'inst?]]

    ;; One Git commit object observed from the object database.
    "git/commit" [:map {:closed true}
                  [:commit/oid [:ref "git/oid"]]
                  [:commit/parent-oids [:vector [:ref "git/oid"]]]
                  [:commit/tree-oid [:ref "git/oid"]]
                  [:commit/author [:ref "git/person"]]
                  [:commit/committer [:ref "git/person"]]
                  [:commit/message-bytes :any]
                  [:commit/message-text [:string {:min 1}]]]

    ;; A commit observation: one immutable commit object seen during
   ;; ingestion, wrapped in the observation envelope. The same commit
   ;; OID observed again produces a structurally identical record
   ;; (immutable Git identity); the observation/id and observed-at
   ;; will differ per ingestion run.
   "observation/commit-observed-v1"
   (into [:map {:closed true}
          [:observation/type [:= :git/commit-observed]]
          [:observation/request-id {:optional true} :uuid]]
         (into observation-envelope-entries
               [[:resource-id :uuid]
                [:git/observed [:ref "git/commit"]]
                [:git/ref-context [:ref "git/ref-name"]]]))

    ;; A revision-at-path observation: one immutable file-at-commit fact
    ;; linking a selected tree entry to its Git evidence. Each record
    ;; distinguishes add/modify/delete/continuity evidence relative to
    ;; the parent commit, with exact path, blob OID, mode, and
    ;; repository context (family/instance). Root commits carry
    ;; :revision/evidence :initial and omit parent fields.
    "observation/revision-at-path-v1"
    (into [:map {:closed true}
           [:observation/type [:= :revision/at-path-observed]]
           [:observation/request-id {:optional true} :uuid]]
          (into observation-envelope-entries
                [[:resource-id :uuid]
                 [:revision-at-path/id :uuid]
                 [:revision/commit-oid [:ref "git/oid"]]
                 [:revision/tree-oid [:ref "git/oid"]]
                 [:revision/path-raw [:ref "path/raw"]]
                 [:revision/blob-oid [:ref "git/oid"]]
                 [:revision/mode :int]
                 [:revision/evidence [:enum :initial :add :modify :delete :continuity]]
                 [:revision/parent-commit-oid {:optional true} [:ref "git/oid"]]
                 [:revision/parent-blob-oid {:optional true} [:ref "git/oid"]]]))

    ;; An ingestion-run record: one durable event per traversal pass.
   ;; Records which commits were encountered and which objects failed.
   ;; Reruns produce a new ingestion-run rather than rewriting prior
   ;; commit observations.
   "observation/ingestion-run-v1"
   (into [:map {:closed true}
          [:observation/type [:= :ingestion/run-completed]]
          [:observation/request-id {:optional true} :uuid]]
         (into observation-envelope-entries
               [[:resource-id :uuid]
                [:ingestion/repo-path [:ref "path/observed"]]
                [:ingestion/selected-refs [:vector [:ref "git/ref-name"]]]
                [:ingestion/commit-count :int]
                [:ingestion/failure-count :int]
                [:ingestion/failures [:vector [:map {:closed true}
                                               [:failure/oid {:optional true} [:ref "git/oid"]]
                                               [:failure/ref {:optional true} [:ref "git/ref-name"]]
                                               [:failure/reason :string]
                                               [:failure/message {:optional true} :string]]]]]))})

(defn exact-path?
  "The `:path/comparison :exact` contract: a candidate string counts as
  the observed path only when it is identical, code point for code point.
  Unicode-normalized variants (NFC vs NFD `.ημ/`), case-folded or
  transliterated spellings are different identity values (ADR-001 §5)."
  [observed-raw candidate]
  (= observed-raw candidate))
