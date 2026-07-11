---
id: "01900d7c-7f3a-7e8b-9c4d-000000001901"
title: "ENG-020A: Query cross-stage projection status (`ep status`)"
status: ready
type: "story"
priority: "P1"
phase: 1
parent: story-20-observe-ingestion-and-indexing
design: "docs/kanban/stories/story-20-observe-ingestion-and-indexing.md"
points: 2
labels: [phase-1, operations, observability, cli]
category: "stories"
dependency: ["01900d7c-7f3a-7e8b-9c4d-000000001107", "01900d7c-7f3a-7e8b-9c4d-000000001203", "01900d7c-7f3a-7e8b-9c4d-000000001301", "01900d7c-7f3a-7e8b-9c4d-000000001302"]
---
# ENG-020A: Query cross-stage projection status (`ep status`)

One status query across registration, discovery, extraction, indexing, and embedding.

## Acceptance criteria

- Reports per-stage counts, failures, retries, checkpoint state, and projection lag.
- A failure is inspectable with resource ID, commit/blob/path context, operation version, and error detail.
- Unavailable sources report UNAVAILABLE without inventing state.
