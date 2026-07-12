---
id: "01900d7c-7f3a-7e8b-9c4d-000000001704"
title: "ENG-017D: Establish reusable observation adapter contract laws"
status: ready
type: "story"
priority: "P0"
phase: 1
epic: "01900d7c-7f3a-7e8b-9c4d-000000000001"
design: "docs/designs/verification-architecture.md"
adr: "docs/adrs/adr-004-contract-first-adversarial-verification.md"
points: 5
labels: ["quality", "contract-tests", "adapters", "differential-testing", "phase-1"]
category: "stories"
dependency: ["01900d7c-7f3a-7e8b-9c4d-000000001703"]
verification: ["unit-test"]
risk: "medium"
---
# ENG-017D: Establish reusable observation adapter contract laws

## Intent

One law suite, every adapter: encode the observation-port contract as a
parameterized test harness so in-memory and Mongo (and any future adapter)
are judged by identical laws with normalized data-level outcomes. Divergent
adapters become a test failure instead of a discovery in production.

## Decision context

Implements `docs/designs/verification-architecture.md` § "Adapter law
harness" under ADR-004 decision 4 (every adapter passes shared contract
tests). Directly targets the "valid only in Mongo" split observed in the
audit (ENG-001A comment: repository-location is the only validated type).

## Scope

- `observations-laws` harness taking `{:keys [make-port capabilities]}`;
  laws skipped for undeclared capabilities are reported as skipped, never
  silently passed.
- Laws: valid write accepted; invalid write rejected with
  `:schema-validation-failed`; rejection leaves state unchanged; idempotent
  replay; changed-content conflict; deterministic ordering where contracted;
  export/import round-trip equivalence.
- Comparisons over normalized domain outcomes only — injected `:now`/`:new-id`
  capabilities; never driver exceptions, raw documents, timestamps, or map
  order.
- Run the suite against the ENG-017C reference adapter in the unit alias.
- Prove the harness has teeth: a deliberately permissive fixture adapter
  (bare `swap!`, like the pre-ENG-017C double) must fail the suite.

## Non-goals

- Mongo lifecycle/encoding specifics (ENG-017E). Generative laws (ENG-017I).

## Invariants

- A law failure names the law and the expected/actual outcome category.
- The suite is data-parameterized: adding an adapter requires a factory and
  capability declaration, not copied tests.

## Verification

| Claim | Evidence | Location |
|---|---|---|
| Harness runs against reference adapter | Law suite green in unit alias | `epiphany.law-suite.observations-test` |
| Harness detects permissive adapters | Deliberately permissive fixture fails ≥ the rejection + state-unchanged laws | same (negative fixture) |
| Failures are diagnosable | Failure output includes law name + normalized categories | assertion on report shape |

## Acceptance criteria

- Law suite passes for the in-memory reference adapter under
  `clojure -M:unit-test`.
- The permissive fixture adapter demonstrably fails, and that negative test
  is itself part of the suite (the harness's own regression guard).

## Dependencies and interfaces

- Depends on ENG-017C (reference adapter enforcing the semantics the laws
  encode).
- Provides to ENG-017E: the exact suite Mongo must pass. Provides to
  ENG-017I: the law vocabulary generative tests extend.

## Risks and open questions

- Ordering laws: confirm which queries actually contract deterministic order
  before encoding it — encode observed contracts, don't invent them.

## Completion evidence

Test output including the negative-fixture failure demonstration,
`git diff --stat`, reviewer named at done.

## Would have gated

The in-memory/Mongo divergence at the heart of the false-green problem: with
this suite required, US-000B's double and ENG-001A's Mongo adapter could not
both have been "green" while disagreeing about what is persistable.

---
REWORK 2026-07-12: body rewritten to the story contract (original preserved in git history and scratchpad; see ENG-017A comment for the shared rework rationale). Triage authority: user instruction this session. --tasks-dir docs/kanban
---
