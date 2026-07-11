---
id: "01900d7c-7f3a-7e8b-9c4d-000000001001"
title: "US-001: Register a local repository"
status: "incoming"
type: "story"
priority: "P1"
phase: 1
epic: "01900d7c-7f3a-7e8b-9c4d-000000000001"
design: "docs/notes/design/phase-1-corpus-archaeology.md"
points: 1
labels: [git, ingestion, provenance, registration]
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
