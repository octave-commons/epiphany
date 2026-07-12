---
id: "01900d7c-7f3a-7e8b-9c4d-000000001110"
title: "ENG-001J: Report ledger ingestion outcomes and recovery evidence"
status: "done"
type: "story"
priority: "P1"
phase: 1
epic: "01900d7c-7f3a-7e8b-9c4d-000000000001"
design: "docs/adrs/adr-001-git-backed-resource-identity.md"
points: 3
labels: ["phase-1", "archaeological-ledger", "engineering-breakdown"]
category: "stories"
dependency: ["01900d7c-7f3a-7e8b-9c4d-000000001107", "01900d7c-7f3a-7e8b-9c4d-000000001108"]
---

# ENG-001J: Report ledger ingestion outcomes and recovery evidence

Expose ingestion-run state and recovery guidance through direct CLI queries.

## Acceptance criteria

- A user can list runs, per-stage counts, failures, checkpoint state, selected configuration, and source availability.
- Output identifies whether a record is Git-observed, platform-observed, derived, or provisional.
- Unavailable Git/Mongo sources are reported as `UNAVAILABLE` without inventing recovered state.
- Integration fixtures cover an interrupted run and a source becoming unavailable.

---
Completed. Added list-ingestion-runs/list-checkpoints to observations port, Mongo adapter query functions, ep status CLI command, 4 new CLI tests. Total: 155 unit tests + 11 integration tests, 491 assertions, 0 failures.
---
