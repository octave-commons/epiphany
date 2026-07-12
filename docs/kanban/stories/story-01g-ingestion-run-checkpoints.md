---
id: "01900d7c-7f3a-7e8b-9c4d-000000001107"
title: "ENG-001G: Record ingestion runs and projection checkpoints"
status: "done"
type: "story"
priority: "P1"
phase: 1
epic: "01900d7c-7f3a-7e8b-9c4d-000000000001"
design: "docs/adrs/adr-001-git-backed-resource-identity.md"
points: 3
labels: ["phase-1", "archaeological-ledger", "engineering-breakdown"]
category: "stories"
dependency: ["01900d7c-7f3a-7e8b-9c4d-000000001104", "01900d7c-7f3a-7e8b-9c4d-000000001106"]
---

# ENG-001G: Record ingestion runs and projection checkpoints

Make graph/revision observation resumable without treating cursor state as repository identity.

## Acceptance criteria

- An ingestion run records input refs, configuration identity, tool version, start/end status, counters, and failures.
- Projection checkpoints are named/versioned independently and retain their own progress state.
- Interrupted runs can resume deterministically from persisted checkpoints.
- A replay into empty derived projections is possible from retained Mongo observations and available Git sources.

---
Completed. Added projection-checkpoint-v1 schema, extended observations port with record-ingestion-run!/record-checkpoint!, Mongo adapter with ingestion-run-v1 and projection-checkpoint-v1 collections, domain/ingestion.clj orchestration, 6 domain tests + 2 integration tests (53 assertions). Total: 154 tests, 460 assertions, 0 failures.
---
