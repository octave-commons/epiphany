---
id: "01900d7c-7f3a-7e8b-9c4d-000000001706"
title: "ENG-017F: Validate decoded and imported observation data"
status: "incoming"
type: "story"
priority: "P1"
phase: 1
epic: "01900d7c-7f3a-7e8b-9c4d-000000000001"
design: "docs/designs/verification-architecture.md"
points: 5
labels: [quality, backups, decoding, integrity, phase-1]
category: "stories"
dependency: ["01900d7c-7f3a-7e8b-9c4d-000000001701", "01900d7c-7f3a-7e8b-9c4d-000000001705"]
---

# ENG-017F: Validate decoded and imported observation data

Validate all Mongo-decoded records and backup-import payloads against their
claimed contracts before admitting them to application/domain behavior.

## Scope

- Add schema/version validation after Mongo decode.
- Define backup manifest validation, supported-version behavior, and
  pre-mutation failure semantics.
- Distinguish corrupt data, unsupported schema version, and unavailable dependency.
- Add canonical export/import/export round-trip tests.

## Acceptance criteria

- Corrupt, truncated, unknown-version, and schema-invalid backups fail before mutation.
- A malformed stored document is surfaced as integrity failure, not omitted or returned as empty data.
- Backup round trips preserve equivalent canonical observation data.
- Tests preserve the difference between unavailable, corrupt, and empty outcomes.
