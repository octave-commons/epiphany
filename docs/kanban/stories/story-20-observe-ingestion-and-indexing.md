---
id: "01900d7c-7f3a-7e8b-9c4d-000000001020"
title: "US-020: Observe ingestion and indexing"
status: "breakdown"
type: "story"
priority: "P1"
phase: 1
epic: "01900d7c-7f3a-7e8b-9c4d-000000000001"
design: "docs/notes/design/phase-1-corpus-archaeology.md"
points: 8
labels: [operations, observability, ingestion, indexing, monitoring, decomposed]
category: "stories"
---

# US-020: Observe ingestion and indexing

## Acceptance Criteria

- The system records structured events/logs for registration, discovery, extraction, cache use, indexing, candidate generation, and review actions.
- A status view shows repositories scanned, commits seen, blobs read, Markdown revisions extracted, sections indexed, failures, retries, and projection lag.
- Cache metrics distinguish LMDB hit/miss/eviction behavior from durable source availability.
- The user can inspect a failure with repository resource ID, commit/blob/path context, operation version, and error details.
- A failed derived operation can be retried or replayed without mutating Git observations.
- Weak nodes are not treated as mandatory holders of irreplaceable hot state.

## Notes

**As an operator,** I want to see health and throughput for each processing stage so that a large corpus does not become silently incomplete.

## Decomposed into

Product-outcome card; do not implement directly. The engineering slices:

- **ENG-001J** `story-01j-ledger-operational-diagnostics.md` — ledger outcomes + recovery evidence (3)
- **ENG-020A** `story-20a-cross-stage-status-query.md` — cross-stage `ep status` (2)
