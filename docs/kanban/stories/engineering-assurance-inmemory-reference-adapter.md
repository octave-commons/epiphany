---
id: "01900d7c-7f3a-7e8b-9c4d-000000001703"
title: "ENG-017C: Make the in-memory observations adapter contract-enforcing"
status: ready
type: "story"
priority: "P0"
phase: 1
epic: "01900d7c-7f3a-7e8b-9c4d-000000000001"
design: "docs/designs/verification-architecture.md"
adr: "docs/adrs/adr-004-contract-first-adversarial-verification.md"
points: 5
labels: ["quality", "adapters", "inmemory", "idempotency", "phase-1"]
category: "stories"
dependency: ["01900d7c-7f3a-7e8b-9c4d-000000001701", "01900d7c-7f3a-7e8b-9c4d-000000001702"]
verification: ["unit-test"]
risk: "medium"
---
# ENG-017C: Make the in-memory observations adapter contract-enforcing

## Intent

Retire the false-green oracle. `infra/adapters/in_memory.clj:55-105` currently
appends any map via bare `swap!` — the verified root cause of unit suites
certifying writes production would reject (audit 2026-07-12; defect inventory
item 3). This slice upgrades it to the reference implementation of the shared
observation-port semantics: it enforces what the contract says, while staying
atom-backed and fast.

## Decision context

Implements `docs/designs/verification-architecture.md` § "Reference adapter"
under ADR-004 decision 3 ("the in-memory adapter is a contract-enforcing
reference adapter... not allowed to accept states the durable adapter rejects
under the shared port contract").

## Scope

- Registry validation on every `:record-*` write inside the adapter (defense
  in depth beneath the ENG-017B wrapper).
- Enforce declared idempotency: same request-ID replay returns the recorded
  fact; same request-ID with materially different content yields
  `{:code :idempotency-conflict}`.
- Enforce append-only semantics and deterministic ordering where the port
  contract declares ordering.
- State inspection/export sufficient to prove rejected writes changed nothing.
- Migrate any existing unit test that depended on storing schema-invalid
  observations — each such test is itself audit evidence; list them in a card
  comment rather than silently rewriting them.

## Non-goals

- No Mongo emulation: no BSON, indexes, transactions, query planning, or
  driver exceptions (ADR-004 rejected alternative). No law-suite construction
  (ENG-017D). No Mongo changes (ENG-017E).

## Invariants

- A rejected write leaves all observable adapter state byte-identical.
- Invalid direct adapter use fails with the same domain error category as
  validated port use (`:schema-validation-failed`).
- The adapter never throws driver-style exceptions; failures are domain data.

## Verification

| Claim | Evidence | Location |
|---|---|---|
| Invalid write rejected + state unchanged | Write invalid record, export state before/after, compare | `epiphany.infra.adapters.in-memory-test` |
| Idempotent replay stable | Same request-ID twice → one fact | same |
| Changed-content replay conflicts | Same request-ID, mutated payload → `:idempotency-conflict` | same |
| No test depends on permissive storage | Suite green after enforcement lands | `clojure -M:unit-test` full run |

## Acceptance criteria

- All verification tests pass; the full unit suite passes with enforcement on
  (no test still requires permissive behavior).
- Tests that previously relied on invalid storage are listed in a card
  comment with their disposition (fixed fixture vs. deleted with reason).

## Dependencies and interfaces

- Depends on ENG-017A (registry) and ENG-017B (gateway + error vocabulary).
- Provides to ENG-017D: the reference adapter the law suite is written
  against first.

## Risks and open questions

- Enforcement may surface latent invalid fixtures across the suite (the
  false-green debt coming due). If the migration exceeds this card's points,
  stop, comment the inventory of failing fixtures, and split per the
  breakdown rule — do not weaken schemas to get green.

## Completion evidence

Test output, before/after count of fixtures corrected, `git diff --stat`,
anomalies as comments, reviewer named at done.

## Would have gated

US-000B graded B+ in the 2026-07-12 audit precisely because this behavior was
never required. Every one of the 41 done cards whose evidence was "unit tests
pass" inherits the permissive oracle; this card is the single highest-leverage
correction to the trustworthiness of future green claims.

---
REWORK 2026-07-12: body rewritten to the story contract (original preserved in git history and scratchpad; see ENG-017A comment for the shared rework rationale). Triage authority: user instruction this session. --tasks-dir docs/kanban
---
