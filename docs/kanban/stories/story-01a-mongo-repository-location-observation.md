---
id: "01900d7c-7f3a-7e8b-9c4d-000000001101"
title: "ENG-001A: Persist idempotent repository-location observations"
status: "done"
type: "story"
priority: "P0"
phase: 1
epic: "01900d7c-7f3a-7e8b-9c4d-000000000001"
design: "docs/adrs/adr-001-git-backed-resource-identity.md"
points: 5
labels: ["phase-1", "archaeological-ledger", "engineering-breakdown", "mongodb"]
category: "stories"
dependency: ["01900d7c-7f3a-7e8b-9c4d-000000001103", "01900d7c-7f3a-7e8b-9c4d-000000000000-b", "01900d7c-7f3a-7e8b-9c4d-000000000000-c"]
---

# ENG-001A: Persist idempotent repository-location observations

Implement the MongoDB adapter required by the registration application service.

## Contract

- The command envelope contains `:request-id` and exact requested `:repository-path`; the application adds `:resource-id`, resolved `:common-git-dir`, `:observed-at`, and an explicit schema/adapter version.
- It records an Epiphany observation, not a claim that a filesystem location is permanent identity or that a resource has joined a repository family.
- Lookup by `:request-id` returns the complete previously accepted registration result, including the selected target/profile identity.
- Recording the same request ID with equivalent canonical command content is atomic and idempotent; a differing command payload for that request ID is an explicit `:idempotency/conflict` failure.
- The unique constraint is scoped by target/database identity plus request ID; request IDs must not accidentally deduplicate commands sent to different configured targets.
- The adapter uses a named Epiphany-owned, isolated integration-test database and cleans only collections it owns deterministically.

## Acceptance criteria

- Integration tests against configured MongoDB prove insert, retry, lookup, and conflict behavior.
- Mongo uniqueness constraints enforce target-scoped request-ID idempotency and surface duplicate-key races as the accepted prior result or an explicit conflict.
- Tests cover concurrent/retried writes, equivalent retry, conflicting retry, and lookup after process restart.
- Tests preserve exact path strings, including Unicode, without normalization.
- Mongo unavailability and invalid configuration produce explicit adapter failures; selected `:services` mode never substitutes an in-memory store.
- Index creation is explicit, versioned, and verified by integration tests.

## Breakdown (revised)

Re-scored during board triage. The original subtask table totaled 14 but double-counted schema work (owned by ENG-001C) and the observation port (owned by US-000B), and inflated test subtasks. Honest scope at 5 points:

| # | Task | Points | Dependencies |
|---|------|--------|--------------|
| 1 | MongoDB adapter infra (connection, writes, lookup) against ENG-001C schemas | 2 | ENG-001C, US-000B |
| 2 | Target-scoped request-ID idempotency and `:idempotency/conflict` detection | 1 | #1 |
| 3 | Explicit versioned index creation | 1 | #1 |
| 4 | Integration tests: insert/retry/lookup/conflict, concurrency, restart, Unicode paths, unavailability, isolated test-db cleanup | 1 | #1–#3, US-000C |

### Notes

- Schemas come from ENG-001C; the observation port comes from US-000B. This card implements the adapter, not the contracts.
- This is the first MongoDB adapter; establish patterns for future adapters.
- Use the Epiphany-owned integration-test database; clean only owned collections.
- Preserve exact path strings (no normalization); design for concurrent/retried writes.

---
Completed. MongoDB adapter with idempotent inserts, versioned indexes, Unicode path preservation, and 9 integration tests (16 assertions). Fixed: MongoWriteException API (.getCode not .getWriteError), Document.getInteger→getLong for MongoDB 5.x, keyword→string encoding, collection-prefix for test isolation.

AUDIT 2026-07-12: status=done graded C+. Deliverable verified: mongo.clj defines and applies validate-location-observation for observation/repository-location-v1 (mongo.clj:26,413) with idempotency handling. No completion evidence comment recorded. Systemic finding: this card's validation pattern was never generalized — it is the ONLY validated observation type in the Mongo adapter; later persistence cards (e.g. ENG-002B section extractions, ENG-001G runs/checkpoints) wrote unvalidated record types. This upgrades the defect-inventory claim 'Mongo validates only repository-location' from provisional to observed. Gate: ENG-017E (align all Mongo observations with shared contract laws) cites this card as both precedent and boundary. --tasks-dir docs/kanban
---
