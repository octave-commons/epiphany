---
id: "01900d7c-7f3a-7e8b-9c4d-000000001702"
title: "ENG-017B: Enforce schemas through validating observation ports"
status: ready
type: "story"
priority: "P0"
phase: 1
epic: "01900d7c-7f3a-7e8b-9c4d-000000000001"
design: "docs/designs/verification-architecture.md"
adr: "docs/adrs/adr-004-contract-first-adversarial-verification.md"
points: 3
labels: ["quality", "schemas", "ports", "validation", "phase-1"]
category: "stories"
dependency: ["01900d7c-7f3a-7e8b-9c4d-000000001701"]
verification: ["unit-test"]
risk: "low"
---
# ENG-017B: Enforce schemas through validating observation ports

## Intent

Make validation unavoidable instead of remembered: wrap every public
observations-port write at the composition root so no application flow can
reach an adapter with an unvalidated record. After this slice, an invalid
write fails identically regardless of which adapter is composed.

## Decision context

Implements `docs/designs/verification-architecture.md` § "Validation boundary"
/ "Write path" under ADR-004 decision 2 ("no public persistence operation may
rely solely on a caller remembering to validate"). Consumes ENG-017A's
`schema-for-operation` and error data.

## Scope

- `epiphany.application.validation` (single gateway): resolves schemas via the
  ENG-017A registry, raises stable `:schema-validation-failed` domain errors,
  never embeds record content in thrown data.
- A `validating-observations-port` decorator applied at the composition root
  (`infra/profile.clj`) for **both** `:local` and `:services` profiles.
- Rejection-before-delegation proven: a rejected write must not invoke the
  wrapped function (spy/counter test), so no adapter mutation can occur.
- All registered write operations wrapped; wrapping is driven by the registry
  data, not a hand-maintained list.

## Non-goals

- No adapter-internal changes (`in_memory.clj` — ENG-017C; `mongo.clj` —
  ENG-017E). No read/decode validation (ENG-017F). No CLI/HTTP decoding
  (ENG-017G).

## Invariants

- Every registered write operation is wrapped exactly once at composition.
- An unregistered operation encountered at wrap time fails composition loudly
  (`:unregistered-write-operation`), not at first use.
- Validation errors carry `:code`, `:schema/name`, `:operation`, and explain
  data — never the raw record.

## Verification

| Claim | Evidence | Location |
|---|---|---|
| All registry ops wrapped | Test diffs wrapped keys against `law.operations/registered-operations` | `epiphany.application.validation-test` |
| Invalid write rejected before delegation | Spy adapter records zero calls on invalid input | same |
| Valid write delegates unchanged | Pass-through test | same |
| Both profiles compose the wrapper | Profile composition test | `epiphany.infra.profile-test` |
| Error data excludes record content | Key assertion on thrown ex-data | same |

## Acceptance criteria

- Each public `:record-*` call is validated exactly once at the wrapper
  boundary; invalid inputs return `:schema-validation-failed` and the wrapped
  function is provably not invoked.
- Composition with an unregistered operation fails at startup.
- All verification tests above pass under `clojure -M:unit-test`.

## Dependencies and interfaces

- Depends on ENG-017A (ready, same lane): registry + error contract.
- Provides to ENG-017C/E: the same gateway for adapter-local defense in depth.

## Risks and open questions

- `:import-all` wrapping semantics (bulk payload) may be deferred to ENG-017F;
  if deferred, mark it explicitly unwrapped in the registry data so the
  completeness test stays honest.

## Completion evidence

Test run output, `git diff --stat` (application + profile + tests only),
anomalies as comments, reviewer named at done per
`docs/process/review-and-acceptance.md`.

## Would have gated

US-000B composed raw adapters directly; with this wrapper the permissive
in-memory adapter could not have certified application flows (audit: US-000B
graded B+ — its criteria never required enforcement). Later cards whose unit
evidence relied on unvalidated writes (ENG-001G, ENG-002B) inherit that gap;
this closes it at one seam.

---
REWORK 2026-07-12: body rewritten to the story contract (original preserved in git history and scratchpad; see ENG-017A comment for the shared rework rationale). Triage authority: user instruction this session. --tasks-dir docs/kanban
---
