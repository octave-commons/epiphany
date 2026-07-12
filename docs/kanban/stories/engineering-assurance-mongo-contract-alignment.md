---
id: "01900d7c-7f3a-7e8b-9c4d-000000001705"
title: "ENG-017E: Align Mongo observations with the shared contract laws"
status: "accepted"
type: "story"
priority: "P0"
phase: 1
epic: "01900d7c-7f3a-7e8b-9c4d-000000000001"
design: "docs/designs/verification-architecture.md"
adr: "docs/adrs/adr-004-contract-first-adversarial-verification.md"
points: 5
labels: ["quality", "mongo", "integration", "contract-tests", "phase-1"]
category: "stories"
dependency: ["01900d7c-7f3a-7e8b-9c4d-000000001704", "01900d7c-7f3a-7e8b-9c4d-000000001301"]
verification: ["integration-test"]
risk: "medium"
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
---
