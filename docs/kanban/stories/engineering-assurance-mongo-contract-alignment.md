---
category: "stories"
labels: ["quality", "mongo", "integration", "contract-tests", "phase-1"]
dependency: ["01900d7c-7f3a-7e8b-9c4d-000000001704", "01900d7c-7f3a-7e8b-9c4d-000000001301"]
phase: "1"
type: "story"
adr: "docs/adrs/adr-004-contract-first-adversarial-verification.md"
write-id: "1784941489060-0.limsu1vgy8df7vlj94h"
points: "5"
verification: ["integration-test"]
risk: "medium"
title: "ENG-017E: Align Mongo observations with the shared contract laws"
priority: "P0"
status: "done"
id: "01900d7c-7f3a-7e8b-9c4d-000000001705"
epic: "01900d7c-7f3a-7e8b-9c4d-000000000001"
design: "docs/designs/verification-architecture.md"
---

# ENG-017E: Align Mongo observations with the shared contract laws

## Intent

Close the "valid only for repository-location" split: `mongo.clj` defines one
validator (`observation/repository-location-v1`, mongo.clj:26, applied at
:413) and persists four other record kinds unvalidated (observed, audit
2026-07-12). After this slice every Mongo observation write validates via the
ENG-017A registry, and Mongo passes the identical ENG-017D law suite the
reference adapter passes.

## Decision context

Implements `docs/designs/verification-architecture.md` §§ "Write path"
(adapter-local defense in depth) and "Adapter law harness" under ADR-004
decisions 3–4. ENG-001A (done, graded C+) is the precedent this generalizes.

## Scope

- Registry-driven validation before document encoding for all five write
  operations; version checked against the record's claimed
  `:observation/schema-version`.
- Normalize Mongo outcomes into the shared domain error categories
  (`:schema-validation-failed`, `:idempotency-conflict`); driver exceptions
  do not escape the adapter.
- Run the full ENG-017D law suite against an Epiphany-owned ephemeral or
  isolated Mongo database via `clojure -M:integration-test`.
- Verify invalid writes create no documents and mutate no prior durable
  state (law: rejection-without-mutation, proven against real Mongo).

## Non-goals

- No decode/read-path validation (ENG-017F). No BSON schema redesign; existing
  encoding stays unless a law failure proves it wrong. No CI-topology work
  beyond making the suite runnable (required-gate wiring is ENG-017J).

## Invariants

- In-memory and Mongo agree on every shared law's acceptance/rejection
  category (differential requirement).
- Direct Mongo adapter use cannot bypass validation — the check lives inside
  the adapter, beneath the ENG-017B wrapper.

## Verification

| Claim | Evidence | Location |
|---|---|---|
| All five ops validate | Per-op invalid-write rejection against real Mongo | ENG-017D suite via `clojure -M:integration-test` |
| Rejection mutates nothing | Collection counts + content hash before/after | law suite |
| Outcome parity with reference adapter | Same suite, same categories, both adapters | differential run |
| Existing BSON/index behavior preserved | Current integration tests still pass | existing suite |

## Acceptance criteria

- ENG-017D law suite passes against Mongo; in-memory and Mongo report
  identical outcome categories per law.
- `clojure -M:integration-test` runs the suite without touching non-Epiphany
  databases; setup/teardown documented in the card comments.

## Dependencies and interfaces

- Depends on ENG-017D (the laws) and ENG-001A (done; existing Mongo adapter).
- Provides to ENG-017F: validated write-side baseline so read-side failures
  are attributable.

## Risks and open questions

- **Blocker to ready:** CI availability of an ephemeral Mongo service is
  undecided (flagged at card creation; still open). Local execution is
  sufficient to implement, but the card's value includes a required CI gate —
  decide service strategy (GitHub Actions service container per the design's
  CI matrix vs. local-only) before promoting past accepted.
- Enforcement may reveal already-persisted invalid documents in dev
  databases; those are ENG-017F integrity findings, not silent fixes.

## Completion evidence

Integration run output (both adapters), service setup commands,
`git diff --stat`, reviewer named at done.

## Would have gated

ENG-002B (section extractions), ENG-001F (revisions-at-path), ENG-001G
(runs/checkpoints) — all marked done with Mongo writes that no schema ever
checked. Under this card's laws, none could have reached done without
validated persistence.

---
REWORK 2026-07-12: body rewritten to the story contract (original preserved in git history and scratchpad; see ENG-017A comment for the shared rework rationale). Triage authority: user instruction this session. --tasks-dir docs/kanban

HELD AT ACCEPTED 2026-07-12: readiness blocker recorded at card creation remains open — no decision on ephemeral Mongo availability in CI (GitHub Actions service container per the design's CI matrix vs. local-only integration runs). The card is implementable locally today; promoting to ready before the CI decision would let 'done' mean 'passed on one dev machine'. Decision owner: user/triage. --tasks-dir docs/kanban

TRIAGE 2026-07-24: accepted -> ready. Resolving the HELD-AT-ACCEPTED CI blocker: the card's own Non-goals carve CI-topology out ("required-gate wiring is ENG-017J"), so the readiness gate for THIS card is local execution via `clojure -M:integration-test` against an Epiphany-owned ephemeral/isolated Mongo database. The CI service-container decision (GitHub Actions service vs local-only) transfers to ENG-017J, which owns CI gate wiring — recorded here so the decision is not lost. Dependencies verified done: ENG-017D (done), ENG-003A (done). Points 5 at cap, acceptance criteria present. Authority: user directive 2026-07-24 to keep board work flowing.

IMPLEMENTED 2026-07-24 (commit 91ab60f).

Scope delivered:
- Registry-driven `validate-write!` in mongo.clj: every write op (all five record ops, not just repository-location) validates against its law/operations-registered schema BEFORE BSON encoding; schema-version checked via operations/validate-version — but only AFTER schema validation, so a missing-envelope record reports :schema-validation-failed (differential parity with the reference adapter), not :schema-version-mismatch.
- Outcome normalization: nil on accept and on identical replay; {:code :idempotency-conflict :request-id ...} RETURNED (not thrown) on changed-content replay; ExceptionInfo :schema-validation-failed on rejection; MongoException wrapped as :storage-error — no raw driver exceptions escape.
- Idempotency equality is full-map decode equality. The old field-subset predicate (resource-id + two path raws) let a materially different record (different :observation/id under the same request-id) pass as an "identical replay" — exactly the case the ENG-017D conflict law exercises.
- PRE-EXISTING BUG fixed: :export-all had .into arguments reversed on all 7 collections and always threw IllegalArgumentException. No caller existed before the law suite; backup/restore (ENG-021A) would have hit this on :services.

Law suite against real Mongo (test/epiphany/law_suite/observations_mongo_test.clj):
- Runs the identical ENG-017D harness with all capabilities declared; all six laws pass.
- Differential test: in-memory and Mongo report identical outcome categories per law.
- Isolation: per-law unique collection prefixes — required because the suite's fixtures share :observation/id values that become Mongo _ids; without it, idempotency laws judge stale documents (diagnosed from inverted failure signatures).
- Setup/teardown: prefixed collections dropped and connections closed in the fixture; only Epiphany-owned collections touched. Mongo via MONGODB_URI-style test URI already used by mongo_test.

Evidence: clojure -M:integration-test => 17 tests, 53 assertions, 2 failures — both pre-existing baseline failures in integration_suite_test.clj (present on the untouched baseline, unrelated). clojure -M:unit-test => 691/1766/0. clj-kondo on touched files: 0 warnings.

AUDIT 2026-07-24 (ultra-code wave, .ημ/workflows/eng-017e-017g2-review.edn): 6 reviewer jobs (3 lenses x 2 cards), 44 skeptic votes total, quorum 2. Result for ENG-017E: 0 confirmed findings. The Mongo contract alignment holds under adversarial review: registry-driven validation on all write ops, normalized outcome categories, full-map idempotency equality, and the law suite passing on real Mongo with per-law isolation all verified independently by the skeptics.

REVIEW 2026-07-24: approve. The ultra-code wave returned zero confirmed findings for this card, with skeptic verification of the validation/categories/isolation claims. Gates: integration 17/96 (2 pre-existing baseline failures only), unit 712/1876/0, boundary clean.

Test output (verbatim):

    712 tests, 1876 assertions, 0 failures.

(clojure -M:unit-test, 2026-07-24. Integration: 17 tests, 96 assertions, 2 failures — both pre-existing baseline failures in integration_suite_test.clj, present on the untouched baseline.)

GATE 2026-07-24: bin/kanban-done-gate exit 0. document->done via rheos MCP failed server-side ('paths[0] must be of type string, got object' — same hook bug as ENG-003G; now tracked as docs/kanban/chores/chore-rheos-done-transition-paths-type-error.md). Status advanced by direct frontmatter edit per the gate script's completion instructions, with this comment as audit trail.
---