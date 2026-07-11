---
id: "01900d7c-7f3a-7e8b-9c4d-000000001103"
title: "ENG-001C: Define append-only Git registration observation schemas"
status: "in_progress"
type: "story"
priority: "P0"
phase: 1
epic: "01900d7c-7f3a-7e8b-9c4d-000000000001"
design: "docs/adrs/adr-001-git-backed-resource-identity.md"
points: 4
labels: ["phase-1", "archaeological-ledger", "engineering-breakdown", "schemas", "malli"]
category: "stories"
dependency: ["01900d7c-7f3a-7e8b-9c4d-000000000000-a"]
---

# ENG-001C: Define append-only Git registration observation schemas

Define Malli contracts and Mongo record shapes for registration and Git-ref observations before walking history.

## Acceptance criteria

- Schemas distinguish Git-observed commit/ref values from platform observation metadata.
- A registration record retains repository instance, family assessment state, exact path, common Git directory, request ID, observed time, and adapter/schema version.
- Schema validation rejects normalized/re-written paths and missing provenance fields.
- Versioned fixtures document the EDN/JSON shapes used by direct-mode output and Mongo adapters.

## Breakdown (revised)

Re-scored during board triage. Dependency inverted: schemas are contracts and come **before** the ENG-001A adapter, not after it — this card also owns the repository-location observation schema that ENG-001A's original subtask #1 duplicated. Honest scope at 4 points:

| # | Task | Points | Dependencies |
|---|------|--------|--------------|
| 1 | Malli schemas: Git-observed commit/ref values, registration observation (incl. repository-location), platform observation metadata | 2 | None |
| 2 | Validation rules: reject normalized/rewritten paths, require provenance fields | 1 | #1 |
| 3 | Versioned EDN/JSON fixtures + schema validation tests | 1 | #1, #2 |

### Notes

- Schemas are contracts; changes require version bumps.
- Git-observed values are immutable facts; platform metadata is append-only.
- Fixtures document expected shapes for direct-mode output and Mongo adapters.
