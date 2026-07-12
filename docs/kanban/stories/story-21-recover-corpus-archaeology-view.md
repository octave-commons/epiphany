---
id: "01900d7c-7f3a-7e8b-9c4d-000000001021"
title: "US-021: Recover the corpus archaeology view"
status: "breakdown"
type: "story"
priority: "P1"
phase: 1
epic: "01900d7c-7f3a-7e8b-9c4d-000000000001"
design: "docs/designs/phase-1-corpus-archaeology.md"
points: 5
labels: [operations, recovery, backup, restore, durability, decomposed]
category: "stories"
---

# US-021: Recover the corpus archaeology view

## Acceptance Criteria

- MongoDB backup/restore procedures recover resource registrations, observed Git metadata, events, review decisions, and projection state.
- Git repositories remain the source of canonical Git blobs during ordinary reindexing.
- LMDB caches are treated as disposable; their loss does not lose canonical source facts or user review decisions.
- Search/vector indices can be rebuilt from Git plus MongoDB metadata/events.
- A restore drill can reproduce a selected evidence packet from restored data and available repository sources.
- The system records which sources were inaccessible during restore/rebuild.

## Notes

**As an operator,** I want to restore core metadata and rebuild projections so that a machine failure does not permanently destroy evidence navigation.

## Decomposed into

Product-outcome card; do not implement directly. The engineering slices:

- **ENG-021A** `story-21a-backup-restore-rebuild-drill.md` — backup/restore + rebuild drill (3)
