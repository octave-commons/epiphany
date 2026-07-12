---
id: "01900d7c-7f3a-7e8b-9c4d-000000001102"
title: "ENG-001B: Expose direct repository-registration command"
status: "done"
type: "story"
priority: "P0"
phase: 1
epic: "01900d7c-7f3a-7e8b-9c4d-000000000001"
design: "docs/adrs/adr-001-git-backed-resource-identity.md"
points: 9
labels: ["phase-1", "archaeological-ledger", "engineering-breakdown", "decomposed"]
category: "stories"
dependency: ["01900d7c-7f3a-7e8b-9c4d-000000001101"]
---

# ENG-001B: Expose direct repository-registration command

Connect the tested registration application service to the canonical direct-mode CLI.

## Acceptance criteria

- `epiphany register <path>` in explicit direct/services mode creates or reuses Git-local identity and records a MongoDB location observation.
- The command receives or generates a request ID, reports its selected target/profile, and returns the resource ID with exact path observations.
- Retry behavior is explicit: a supplied request ID is replay-safe; a generated request ID is printed so callers can safely retry.
- Normal, bare, and linked-worktree fixtures succeed.
- Invalid Git paths, unreadable Git metadata, and unavailable MongoDB return structured diagnostics and leave no partial Mongo observation.
- If a Git-local metadata write is unavailable, this card must not invent the external fallback: that behavior requires a separately specified card and test suite.
- Output clearly distinguishes Git-observed values from Epiphany-recorded observation values.

## Out of scope

No ingestion traversal, parsing, job queue, REST endpoint, or TUI action.

## Decomposed into

This card exceeded the 5-point cap (its subtask total was 16, with schema and CLI-scaffold work double-counted against ENG-001C and US-000A). Do not implement it directly. The work lives in:

- **ENG-001B1** `story-01b1-git-local-identity-resolution.md` — Git path validation, common-dir resolution, Git-local identity across normal/bare/worktree (4 pts)
- **ENG-001B2** `story-01b2-register-command-service-and-cli.md` — registration application service, `epiphany register` CLI, request-ID and error semantics (5 pts)

Acceptance criteria above remain the definition of done for the set.
