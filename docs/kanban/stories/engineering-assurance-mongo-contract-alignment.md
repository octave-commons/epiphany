---
id: "01900d7c-7f3a-7e8b-9c4d-000000001705"
title: "ENG-017E: Align Mongo observations with the shared contract laws"
status: "incoming"
type: "story"
priority: "P0"
phase: 1
epic: "01900d7c-7f3a-7e8b-9c4d-000000000001"
design: "docs/designs/verification-architecture.md"
points: 5
labels: [quality, mongodb, contract-tests, integration, phase-1]
category: "stories"
dependency: ["01900d7c-7f3a-7e8b-9c4d-000000001704", "01900d7c-7f3a-7e8b-9c4d-000000001101"]
---

# ENG-017E: Align Mongo observations with the shared contract laws

Run the shared observation adapter laws against an isolated Mongo integration
database and add local validation before document encoding/writing.

## Scope

- Apply schema/version validation to every Mongo observation write, not only
  repository-location writes.
- Normalize Mongo outcomes into shared domain error categories.
- Run ENG-017D laws against an Epiphany-owned ephemeral/isolated Mongo database.
- Verify invalid writes do not create documents or mutate prior durable state.

## Acceptance criteria

- In-memory and Mongo agree on each shared law's acceptance/rejection category.
- Direct Mongo adapter use cannot bypass schema/version validation.
- Existing BSON-specific integration tests continue to cover encoding/index behavior.
- CI can run this suite separately from no-service unit tests.
