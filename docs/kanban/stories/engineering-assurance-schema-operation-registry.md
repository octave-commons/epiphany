---
id: "01900d7c-7f3a-7e8b-9c4d-000000001701"
title: "ENG-017A: Define the schema operation registry"
status: "ready"
type: "story"
priority: "P0"
phase: 1
epic: "01900d7c-7f3a-7e8b-9c4d-000000000001"
design: "docs/designs/verification-architecture.md"
adr: "docs/adrs/adr-004-contract-first-adversarial-verification.md"
points: 3
labels: ["quality", "schemas", "contracts", "verification", "phase-1"]
category: "stories"
dependency: ["01900d7c-7f3a-7e8b-9c4d-000000000000-b"]
verification: ["unit-test"]
risk: "low"
---

# ENG-017A: Define the schema operation registry

## Intent

Unit tests currently pass writes that production storage would reject, because
nothing ties a port write operation to the schema its records must satisfy.
This slice creates that tie as inspectable data: one registry mapping every
public durable observation write to its named, versioned Malli schema. It is
the contract that every later enforcement slice (ENG-017B–F) reads; it changes
no adapter behavior itself.

## Decision context

Implements `docs/designs/verification-architecture.md` § "Schema ownership"
("The source of truth is an operation registry, not inference from an
arbitrary map") under ADR-004 decisions 1–2 (every durable observation has a
named versioned schema; enforcement must not depend on a caller remembering to
validate). Direct response to the audited false-green oracle
(`in_memory.clj:55-105`, US-000B audit comment) and to the Mongo adapter
validating only one of five persisted record kinds (`mongo.clj:26,413`,
ENG-001A audit comment).

## Scope

- New namespace `epiphany.law.operations` (law layer: data + pure lookups, no
  I/O) declaring one entry per public observations-port write:

  | Operation | Schema | Version |
  |---|---|---|
  | `:record-repository-location!` | `observation/repository-location-v1` | 1 |
  | `:record-revision-at-path!` | `observation/revision-at-path-v1` | 1 |
  | `:record-ingestion-run!` | `observation/ingestion-run-v1` | 1 |
  | `:record-checkpoint!` | `observation/projection-checkpoint-v1` | 1 |
  | `:record-section-extraction!` | `observation/section-extraction-v1` | 1 |

  Each entry: `{:input-schema <name> :version <n> :persistence :append-only}`.
- `:import-all` (backup import) is registered as a bulk operation whose
  payload references the same per-collection schemas; its enforcement is
  ENG-017F's job, but the mapping data lives here.
- Lookup fns: `schema-for-operation`, `registered-operations`, and
  `validate-version` (record's `:observation/schema-version` must equal the
  registry entry's version; mismatch returns stable error data, it does not
  throw here).
- Completeness checks as unit tests (see Verification).
- Stable error shape for downstream use:
  `{:code :unregistered-write-operation | :schema-version-mismatch, :operation ..., :schema/name ...}`
  — never embedding the offending record's content (repository text may be
  sensitive).

## Non-goals

- No port wrapping or decorator (ENG-017B).
- No change to `in_memory.clj`, `mongo.clj`, or any adapter (ENG-017C/E).
- No read/decode or backup-import validation (ENG-017F).
- No new schemas and no version bumps; `observation/history-replacement-v1`
  and other schemas without a dedicated port operation are explicitly listed
  as read-only/embedded in the registry data rather than silently omitted.

## Contract changes

Additive only. New law namespace and registry data; no existing signature,
schema, port, CLI, or HTTP behavior changes. `law/ports.clj` op entries remain
`:any` in this slice (tightening them is ENG-017B's contract change).

## Invariants

- Every public durable write operation has exactly one registry entry.
- An operation absent from the registry is representable only as the stable
  error datum — there is no permissive fallback path.
- A record claiming a schema version other than the registered one is
  version-mismatch error data, regardless of whether it would validate.
- Registry entries are plain EDN data — usable by tests, docs generation, and
  the future checker without invoking any function.

## Implementation outline

1. `src/epiphany/law/operations.clj`: `operations` map (data), lookups, error
   constructors.
2. Register with `epiphany.law.registry/schemas` so completeness tests can
   compare both directions.
3. `test/epiphany/law/operations_test.clj`: completeness both ways
   (port ops ⊆ registry, registry ⊆ known schemas), version-mismatch datum,
   unregistered-op datum, and a fixture-valid record for every registered
   schema (fails if a schema name in the registry cannot validate its own
   known-good fixture).

## Verification

| Claim | Evidence | Command / location |
|---|---|---|
| Every observations-port write op is registered | Completeness test diffing `law/ports.clj` op keys against registry | `clojure -M:unit-test` → `epiphany.law.operations-test/every-port-write-is-registered` |
| No registry entry points at a nonexistent schema | Registry resolves each `:input-schema` via `law.registry` | `.../every-registered-schema-resolves` |
| Unknown operation yields stable error datum | Negative unit test on `schema-for-operation` | `.../unregistered-operation-is-explicit` |
| Version mismatch is detected as data | `validate-version` against fixture with bumped `:observation/schema-version` | `.../schema-version-mismatch-detected` |
| Error data never contains record content | Assertion on error datum keys | `.../error-data-excludes-record-content` |

## Acceptance criteria

- All five verification tests above exist, are named as listed, and pass under
  `clojure -M:unit-test`.
- The registry is data (a var holding a map), not a function of runtime state.
- Adding a new op to `law/ports.clj` without a registry entry turns the suite
  red (demonstrated in the completeness test by construction).
- No adapter or application namespace is modified (checked at review via
  `git diff --stat`).

## Dependencies and interfaces

- Depends on US-000B (done): `law/ports.clj` op vocabulary and
  `law/registry.clj` exist.
- Provides to ENG-017B: `schema-for-operation` + error data contract.
- Provides to ENG-017E/F: per-collection schema mapping for Mongo/import.

## Risks and open questions

- `:import-all`'s payload shape may not map 1:1 onto per-operation schemas;
  if the backup payload uses physical collection names (see defect-inventory
  claimed item on logical-vs-physical naming), record the observed mismatch in
  the registry data as an explicit note rather than guessing — resolving it is
  ENG-017F scope.
- Whether `observation/registration-v1`, `git-ref-v1`, `commit-observed-v1`,
  `history-replacement-v1` are persisted through a port this slice must cover:
  inspect adapters during implementation; anything found writing them through
  an unlisted path is an anomaly to log on this card, not to absorb.

## Completion evidence

Required before review→done: test run output (command + counts), the registry
data pasted or linked in a card comment, `git diff --stat` showing law+test
files only, and any anomalies logged as comments. Acceptance at done requires
naming the reviewer/authority per `docs/process/review-and-acceptance.md`.

## Would have gated

Had this existed before Phase 1 implementation: US-000B's permissive adapter
could not have shipped silently (the registry's existence makes "which schema
does this write enforce?" a reviewable question), and ENG-002B / ENG-001G
could not have added persisted record kinds without registry entries.

---
REWORK 2026-07-12 (inbox-synthesis session): body rewritten to the story contract from docs/process/document-governance.md + the batch-authoring critique (inbox 10.21.21) that ruled the original 'administratively valid, not executable'. Original preserved as the anti-example at docs/standards/examples/story/bad-schema-operation-registry-original.md and in git history. Added: operational intent, decision context (design section + ADR-004 rules 1-2 + audit anchors), concrete op->schema table from law/ports.clj:62-66 and law/observation.clj, named verification tests, evidence-linked acceptance criteria, completion-evidence requirements. Triage authority: user instruction this session ('move the incomings up as far as you can'). Transitions: incoming->accepted (spec coherent, grounded, bounded) -> ready (3pts <= 5, acceptance criteria + verification present, sole dependency US-000B is done). --tasks-dir docs/kanban
---
