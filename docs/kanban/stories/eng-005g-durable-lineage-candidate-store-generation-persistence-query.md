---
uuid: "c95a201b-57b7-47e3-ad04-2865ba1b95d9"
title: "ENG-005G: Durable lineage-candidate store (generation, persistence, query)"
status: "incoming"
priority: "P1"
labels: ["phase-1", "review", "candidates", "lineage", "provenance", "storage"]
created_at: "2026-07-21T19:09:06.893Z"
parent: "eng-005a-record-review-decisions-as-append-only-events"
points: "5"
phase: "1"
type: "story"
adr: "docs/adrs/adr-004-contract-first-adversarial-verification.md"
verification: ["unit-test"]
risk: "medium"
epic: "01900d7c-7f3a-7e8b-9c4d-000000000005"
---

# ENG-005G: Durable lineage-candidate store (generation, persistence, query)

## Intent

ENG-005A built the durable *review-decision* store; it deliberately did NOT
build a store for the *candidates* those decisions are about. That missing
half is the shared root blocker behind several stalled AC bullets: a lineage
candidate is generated (retrieval/heuristic/model output at `PROVISIONAL`
tier), persisted, and queryable — by candidate id, relation type, confidence
band, generator version, and time — with review decisions joining to it. Until
this exists, "query by relation type", "seed a candidate from a diff",
"cross-file candidate edges in trace", and a non-placeholder review inbox are
all unreachable.

## Decision context

Surfaced during the 2026-07-21 review lane. Independent reviews of ENG-005A,
ENG-004B, ENG-004D each traced their one remaining unmet AC to the absence of
a candidate store. Rather than hold those cards open indefinitely, the
candidate-dependent AC bullets are descoped to this card (see "Absorbed
scope"). Follows the epistemic-status ladder (ADR / CLAUDE.md): candidates are
`PROVISIONAL` and are promoted to `ACCEPTED`/`REJECTED` only through the
ENG-005A decision events — never silently.

## Scope

- A closed `observation/lineage-candidate-v1` (and/or redundancy-candidate)
  schema in `law`, registered in `law/operations.clj` + `law/ports.clj`, wrapped
  by the ENG-017B validation gateway — same pattern as review-decision-v1.
- Observations-port ops: `:record-lineage-candidate!` (append-only, idempotent
  by request-id) + query ops (by id, by relation type, by generator version,
  by confidence band, by time), in both in-memory and Mongo adapters, passing
  the ENG-017D law suite.
- The join to review decisions: a candidate resolves its current disposition
  (provisional / accepted / rejected / do-not-suggest) from the 005A decision
  events by candidate-id.
- Candidate *generation* wiring sufficient to populate the store from lineage
  projections (or an explicit, tested seam the lineage cards feed).

## Absorbed scope (descoped here from other cards, 2026-07-21)

- **ENG-005A** AC4 "queryable by relation type" — relation lives on the
  candidate; this card makes it queryable and the 005A AC is amended to
  "queryable by candidate, decision type, and time" for its own delivered scope.
- **ENG-004B** AC4 "a comparison can seed a candidate relation or review
  decision" — `ep diff` gains the ability to seed a candidate once this store
  exists.
- **ENG-004D** AC1/AC4 full breadth — cross-file accepted/provisional/rejected
  edges in `ep trace` require candidates + the decisions join
  (`find-cross-edges` is currently called with `{}`).
- **ENG-006C** inbox — a real, non-placeholder candidate queue to triage.

## Acceptance criteria

- Candidates persist append-only, idempotent by request-id, in both adapters;
  pass the ENG-017D law suite.
- Queryable by candidate id, relation type, generator version, confidence
  band, and time — each with a test.
- A candidate's disposition is derived by joining ENG-005A decision events;
  a rejected/do-not-suggest candidate is never surfaced as established.
- Full unit suite green under `clojure -M:unit-test`.

## Dependencies and interfaces

- Depends on ENG-005A (decision store) and ENG-017B (validation gateway).
- Unblocks the descoped AC bullets on ENG-004B, ENG-004D, ENG-005B, ENG-006C.

## Completion evidence

Test output, schema/registry diff, `git diff --stat`, reviewer named at done.

