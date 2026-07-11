---
id: "01900d7c-7f3a-7e8b-9c4d-000000000000-c"
title: "US-000C: Local service manifest and readiness diagnostics"
status: ready
type: "story"
priority: P0
phase: 0
parent: "01900d7c-7f3a-7e8b-9c4d-000000000000"
points: 4
labels: [bootstrap, services, mongodb, object-storage, diagnostics, phase-0]
category: "stories"
dependency: ["01900d7c-7f3a-7e8b-9c4d-000000000000-b"]
---
# US-000C: Local service manifest and readiness diagnostics

Third slice of US-000. Provisions the reproducible local service environment that `:services` mode and every integration test depend on.

## Scope

- Add a checked-in local service manifest for MongoDB and S3-compatible object storage, with documented lifecycle commands and non-secret defaults.
- Add health/readiness diagnostics that name unavailable required services without creating partial durable state.
- Wire `clojure -M:integration-test` to either run against explicitly started local services or exit with clear availability diagnostics.

## Out of scope

Selecting a production deployment platform, orchestrator, hosted MongoDB provider, or cloud object store. Implementing any Epiphany schema or adapter.

## Acceptance criteria

- The checked-in service manifest can start MongoDB and S3-compatible object storage using documented commands, with data directories ignored by Git.
- `clojure -M:integration-test` runs against explicitly started local services or exits with clear availability diagnostics — never hangs, never fabricates results.
- Diagnostics name each unavailable required service and its selected profile.
- The service manifest is a reproducible local adapter environment, not a canonical source of Git facts: Git remains canonical for Git-originated objects; MongoDB is durable for Epiphany observations/events/decisions/jobs/checkpoints; object storage retains immutable source blobs.
