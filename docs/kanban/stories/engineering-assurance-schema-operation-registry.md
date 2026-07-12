---
id: "01900d7c-7f3a-7e8b-9c4d-000000001701"
title: "ENG-017A: Define the schema operation registry"
status: "incoming"
type: "story"
priority: "P0"
phase: 1
epic: "01900d7c-7f3a-7e8b-9c4d-000000000001"
design: "docs/designs/verification-architecture.md"
points: 3
labels: [quality, schemas, contracts, verification, phase-1]
category: "stories"
dependency: ["01900d7c-7f3a-7e8b-9c4d-000000000000-b"]
---

# ENG-017A: Define the schema operation registry

Create the authoritative data registry mapping every public durable observation
write operation to its named, versioned Malli schema and persistence semantics.

## Scope

- Define registry entries for all existing public `:record-*` observation operations.
- Provide lookup and completeness checks against the law schema registry.
- Enforce the operation-selected schema version against the record's claimed version.
- Produce stable, safe schema-validation error data.

## Out of scope

- Port wrapping, adapter implementation changes, Mongo integration, or CLI/HTTP decoding.

## Acceptance criteria

- An unregistered public write operation fails explicitly; there is no permissive fallback.
- Every current durable observation write maps to one named schema and version.
- Tests fail for missing, orphaned, or version-inconsistent registry entries.
- Validation errors include a stable code and schema name without exposing raw repository content.
