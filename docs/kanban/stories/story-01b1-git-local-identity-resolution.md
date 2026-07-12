---
id: "01900d7c-7f3a-7e8b-9c4d-000000001102-1"
title: "ENG-001B1: Resolve Git-local repository identity across repo shapes"
status: "done"
type: "story"
priority: "P0"
phase: 1
epic: "01900d7c-7f3a-7e8b-9c4d-000000000001"
parent: "01900d7c-7f3a-7e8b-9c4d-000000001102"
design: "docs/adrs/adr-001-git-backed-resource-identity.md"
points: 4
labels: ["phase-1", "archaeological-ledger", "engineering-breakdown", "git", "identity"]
category: "stories"
dependency: ["01900d7c-7f3a-7e8b-9c4d-000000000000-a"]
---

# ENG-001B1: Resolve Git-local repository identity across repo shapes

First slice of ENG-001B. Pure Git-side work: given a path, validate it, resolve the common Git directory, and create or reuse the Git-local resource identity. No MongoDB, no CLI surface.

## Scope

- Validate a requested path as a normal worktree, bare repository, or linked worktree; reject non-Git paths with structured errors.
- Resolve the common Git directory before any metadata write.
- Create or reuse `.git/corpus-archaeology/repository.edn` containing only a generated `:resource-id`.
- Provide fixtures for all three repository shapes.

## Out of scope

Mongo observations, the `epiphany register` command, request-ID semantics, and the external-fallback behavior for unwritable Git directories (separately specified card).

## Acceptance criteria

- Normal, bare, and linked-worktree fixtures succeed and resolve to the correct common Git directory.
- An existing `repository.edn` is reused, never regenerated or rewritten.
- Invalid or non-Git paths fail with a specific structured error and create no partial state.
- Unreadable Git metadata produces a structured diagnostic distinct from "not a repository".
- Exact path strings, including Unicode, are preserved without normalization.

---
Implementation complete. 9 tests, 37 assertions, 0 failures. Ready for review. --tasks-dir docs/kanban

Review complete. All acceptance criteria met. Minor TOCTOU observation noted but out of scope for single-process CLI story. --tasks-dir docs/kanban
---
