(ns epiphany.law.commands
  "Named command/query schemas — the single vocabulary both the CLI and
  HTTP surfaces decode into (ENG-017G2, ADR-004 decision 5).

  These are CONTRACTS, not behavior: a surface decodes its raw input
  (argv, JSON body) into a map satisfying one of these schemas, and the
  application layer executes only validated command maps. Schemas carry
  semantic intent only — surface concerns (output format, profile,
   index-dir, port selection) never appear here.")

(def review-decision-types
  "Valid review decision types. MUST stay the SAME set of keywords as
   `epiphany.domain.review/review-decision-types` — pinned by a test,
   mirroring how law/observation pins lineage/relation-types."
  #{:accepted :rejected :relabel :deferred :annotated :do-not-suggest})

(def schemas
  {"command/register"
   [:map {:closed true}
    [:command/name [:= :command/register]]
    [:repository-path [:string {:min 1}]]
    [:request-id {:optional true} :uuid]]

   "query/search"
   [:map {:closed true}
    [:command/name [:= :query/search]]
    [:query [:string {:min 1}]]
    [:mode [:enum :lexical :semantic :hybrid]]
    [:limit [:int {:min 1 :max 1000}]]
    [:filters {:optional true}
     [:map {:closed true}
      [:path-prefix {:optional true} :string]
      [:ref {:optional true} :string]]]]

   "query/status"
   [:map {:closed true}
    [:command/name [:= :query/status]]
    [:resource-id :uuid]]

   "command/review-decision"
   [:map {:closed true}
    [:command/name [:= :command/review-decision]]
    [:candidate-id :uuid]
    [:decision (into [:enum] (sort (map identity review-decision-types)))]
    [:reason {:optional true} :string]
    [:relabel-to {:optional true} :keyword]
    [:annotation {:optional true} :string]]})
