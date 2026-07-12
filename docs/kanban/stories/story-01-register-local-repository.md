---
id: "01900d7c-7f3a-7e8b-9c4d-000000001001"
title: "US-001: Register a local repository"
status: "done"
type: "story"
priority: "P1"
phase: 1
epic: "01900d7c-7f3a-7e8b-9c4d-000000000001"
design: "docs/designs/phase-1-corpus-archaeology.md"
points: 8
labels: ["git", "ingestion", "provenance", "registration", "decomposed"]
category: "stories"
---

# US-001: Register a local repository

## Acceptance Criteria

- Given a path to a valid local Git worktree or bare repository, registration succeeds.
- The system resolves the common Git directory before writing metadata.
- The system creates `.git/corpus-archaeology/repository.edn` with only a generated `:resource-id`.
- The system records the initial local location as an observation in MongoDB.
- The UI/CLI reports the resource ID, current HEAD, default branch/ref if available, and registration time.
- Registration does not scan, parse, or embed all history synchronously; it creates an ingest request/job.
- If the Git directory is not writable, registration succeeds with an external MongoDB identity fallback and clearly reports that fallback state.
- Invalid or non-Git paths fail with a specific error and do not create partial source records.

## Notes

**As a corpus owner,** I want to register a local Git repository as a source so that its Markdown history becomes available to the system.

---
Decomposed into 4 stories (ENG-001C, ENG-001B1, ENG-001A, ENG-001B2). Do not implement directly. --tasks-dir docs/kanban

AUDIT ADDENDUM 2026-07-12 (after clj -M:test): status=done but a latent return-contract bug surfaced. epiphany.application.registration/register! returns two different shapes: the idempotent path (registration.clj:15-16) returns the full stored observation record; the fresh path (:33-36) returns a thin {:resource-id :repository-path :common-git-dir :request-id}. So register! twice with the same request-id yields first-result != second-result (profile_test.clj:73 red). Idempotency 'works' as dedup but not as a stable contract. Not demoting (deliverable exists and largely functions); flagging as a real defect for ENG-017G (command-result contract) / ENG-017D (adapter laws) to gate. Also: registration_test fake keys by :request-id while code emits :observation/request-id — test-fake bug; the real in_memory adapter keys correctly (in_memory.clj:54). --tasks-dir docs/kanban
---
