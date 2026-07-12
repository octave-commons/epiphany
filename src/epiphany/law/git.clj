(ns epiphany.law.git
  "Malli contracts for values observed directly from Git.

  These are immutable Git facts — commit/blob object IDs, ref names, ref
  targets — as opposed to platform observation metadata, which lives in
  `epiphany.law.observation`. Registry keys are versionless for value
  types; record contracts carry an explicit version in their name.

  Every schema body is plain EDN data (string regex patterns, no
  functions), so the registry round-trips through EDN unchanged.")

(def oid-pattern
  "SHA-1 (40) or SHA-256 (64) lowercase hex. Structural only — whether an
  OID resolves is Git's authority, never the schema's."
  "^([0-9a-f]{40}|[0-9a-f]{64})$")

(def ref-name-pattern
  "HEAD or a fully qualified ref. Structural: Git's full refname grammar
  (git-check-ref-format) stays authoritative."
  "^(HEAD|refs/[^\u0000 ]+)$")

(def schemas
  {"git/oid" [:re oid-pattern]
   "git/commit-oid" [:ref "git/oid"]
   "git/blob-oid" [:ref "git/oid"]
   "git/object-format" [:enum :sha1 :sha256]
   "git/ref-name" [:re ref-name-pattern]

   ;; One observed ref value: name -> target at a moment in time.
   "git/ref" [:map {:closed true}
              [:ref/name [:ref "git/ref-name"]]
              [:ref/target [:ref "git/oid"]]
              [:git/object-format [:ref "git/object-format"]]]})
