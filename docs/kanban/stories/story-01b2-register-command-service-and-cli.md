---
id: "01900d7c-7f3a-7e8b-9c4d-000000001102-2"
title: "ENG-001B2: Registration application service and `epiphany register` CLI"
status: "done"
type: "story"
priority: "P0"
phase: 1
epic: "01900d7c-7f3a-7e8b-9c4d-000000000001"
parent: "01900d7c-7f3a-7e8b-9c4d-000000001102"
design: "docs/adrs/adr-001-git-backed-resource-identity.md"
points: 5
labels: ["phase-1", "archaeological-ledger", "engineering-breakdown", "cli", "registration"]
category: "stories"
dependency: ["01900d7c-7f3a-7e8b-9c4d-000000001102-1", "01900d7c-7f3a-7e8b-9c4d-000000001101", "01900d7c-7f3a-7e8b-9c4d-000000001103", "01900d7c-7f3a-7e8b-9c4d-000000000000-b", "01900d7c-7f3a-7e8b-9c4d-000000000000-c"]
---

# ENG-001B2: Registration application service and `epiphany register` CLI

Second slice of ENG-001B. Composes ENG-001B1 identity resolution with the ENG-001A Mongo adapter behind the canonical direct-mode CLI.

## Scope

- Implement the registration application service over the ENG-001B1 identity functions and the ENG-001A location-observation adapter, validated by ENG-001C schemas.
- Wire `epiphany register <path>` in explicit direct/services mode.
- Request-ID handling: a supplied request ID is replay-safe; a generated request ID is printed so callers can safely retry.
- Structured diagnostics for invalid Git paths, unreadable Git metadata, and unavailable MongoDB, leaving no partial Mongo observation.

## Out of scope

No ingestion traversal, parsing, job queue, REST endpoint, or TUI action. No external fallback for unwritable Git directories.

## Acceptance criteria

- `epiphany register <path>` creates or reuses Git-local identity and records a MongoDB location observation.
- The command reports its selected target/profile and returns the resource ID with exact path observations.
- Retry with the same request ID replays the accepted result; a conflicting payload for that ID is an explicit `:idempotency/conflict`.
- Normal, bare, and linked-worktree integration fixtures succeed end to end.
- Failure paths return structured diagnostics and leave no partial Mongo observation.
- Output clearly distinguishes Git-observed values from Epiphany-recorded observation values.

---
Completed. CLI subcommand dispatch, register command with --profile/--request-id flags, :local and :services profile wiring, bin/ep alias, 6 new CLI tests (25 assertions). Total: 137 tests/407 assertions, 0 failures.
---
