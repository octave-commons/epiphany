---
id: "01900d7c-7f3a-7e8b-9c4d-000000001703"
title: "ENG-017C: Make the in-memory observations adapter contract-enforcing"
status: "incoming"
type: "story"
priority: "P0"
phase: 1
epic: "01900d7c-7f3a-7e8b-9c4d-000000000001"
design: "docs/designs/verification-architecture.md"
points: 5
labels: [quality, adapters, inmemory, idempotency, phase-1]
category: "stories"
dependency: ["01900d7c-7f3a-7e8b-9c4d-000000001701", "01900d7c-7f3a-7e8b-9c4d-000000001702"]
---

# ENG-017C: Make the in-memory observations adapter contract-enforcing

Upgrade the atom-backed observations adapter from a permissive test double to
a reference implementation of shared observation-port semantics.

## Scope

- Enforce registry validation for direct adapter use as defense in depth.
- Enforce declared idempotency, conflict, append-only, and deterministic-order rules.
- Add observable state inspection/export support required to prove rejected writes do not mutate state.
- Keep storage simple; do not imitate Mongo BSON, indexes, transactions, or driver behavior.

## Acceptance criteria

- Invalid direct writes fail with the same domain error category as validated port use.
- Rejected writes leave all observable adapter state unchanged.
- Equivalent retry behavior and changed-content request-ID conflicts are explicit and tested.
- No current unit test relies on permissive storage of schema-invalid observations.
