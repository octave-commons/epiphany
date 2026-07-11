---
id: 01900d7c-7f3a-7e8b-9c4d-000000000001
title: "Epic 1: Archaeological Ledger"
status: accepted
type: epic
priority: high
phase: 1
design: docs/notes/design/phase-1-corpus-archaeology.md
size: 8
labels: [ingestion, git, markdown, provenance]
---

# Epic 1: Archaeological Ledger

Ingest Git repositories and Markdown revisions into an immutable, replayable artifact ledger.

## User outcome

“I can select a repository and see every Markdown artifact, its revisions, the commit that introduced each revision, and the exact source bytes that support it.”

## Scope

- Register a repository as a source.
- Discover tracked Markdown files (`**/docs/**/*.md` initially, configurable globs later).
- Walk the reachable Git commit graph.
- Persist commits, parents, author/committer timestamps, message, tree hash.
- Persist file-at-commit revisions, blob hash, path, add/modify/delete status.
- Read raw source bytes through Git object access; use operation-specific caches and explicit Git mirror/bundle preservation rather than default per-blob object storage.
- Record ingestion run, tool version, configuration, and failures.
- Create policy-versioned revision-level file-lineage candidates from explicit Git diff comparisons; candidates are not document identity.
- Never overwrite existing source observations; reruns add an ingestion run and deduplicate by content hash/provenance.

## Core events

- `:source/repository-registered`
- `:git/commit-observed`
- `:artifact/revision-observed`
- `:artifact/lineage-candidate`

## Non-goal

Do not attempt semantic idea lineage here. This epic establishes revision lineage: what Git can support with inspectable evidence.

## Acceptance criteria

- Given a Git repository, ingestion is deterministic for a fixed commit set and configuration.
- Every current Markdown file is linked to a current Git blob and at least one commit.
- Every historical revision has an immutable blob reference, source commit, observed time, and parser/ingestor version.
- A user can inspect raw Markdown for any revision without relying on the working tree.
- Re-running ingestion does not duplicate semantically identical source artifacts.
- A failure in one repository or revision is recorded and does not prevent other repositories from being processed.
- An ingestion can be replayed into empty projections from retained source artifacts.

## Delivery breakdown

See `docs/kanban/epics/epic-01-engineering-breakdown.md`. The ENG-001 cards supply dependency order and engineering acceptance boundaries; this epic remains the product outcome.

## Explicit constraints

- Git is canonical for Git-originated commits, trees, blobs, and exact paths-at-commit.
- MongoDB holds Epiphany-owned observations, idempotency records, run state, checkpoints, and review state.
- Raw Git bytes are read through Git object access; caches are operation-specific and rebuildable.
- A Git rename/copy comparison is comparison evidence. It never silently establishes document-purpose or idea identity.
- Default object storage is not part of ordinary Phase 1 Git ingestion.
