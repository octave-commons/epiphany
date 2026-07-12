(ns epiphany.law.selection
  "Malli contracts for the Markdown tree-entry selection policy and
  its output. The policy records exact include/exclude glob patterns
  and a version; the default v1 policy is documented, not implicit.

  Each selected entry preserves the commit OID, exact Git path string,
  blob OID, file mode, and the selection-policy identity — nothing is
  normalized, resolved, or checked out.")

(def schemas
  {"selection/policy"
   [:map {:closed true}
    [:selection/policy-version [:string {:min 1}]]
    [:selection/include-globs [:vector [:string {:min 1}]]]
    [:selection/exclude-globs [:vector [:string {:min 1}]]]]

   "selection/entry"
   [:map {:closed true}
    [:entry/commit-oid [:ref "git/oid"]]
    [:entry/path-raw [:ref "path/raw"]]
    [:entry/blob-oid [:ref "git/oid"]]
    [:entry/mode :int]
    [:entry/policy-version [:string {:min 1}]]]})
