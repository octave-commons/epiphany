---
id: "01900d7c-7f3a-7e8b-9c4d-000000001005"
title: "US-005: Resume and replay projections"
status: "incoming"
type: "story"
priority: "P1"
phase: 1
epic: "01900d7c-7f3a-7e8b-9c4d-000000000001"
design: "docs/notes/design/phase-1-corpus-archaeology.md"
points: 1
labels: [ingestion, projection, durability, checkpoint, replay]
category: "stories"
---

# US-005: Resume and replay projections

## Acceptance Criteria

- Every projection has its own durable MongoDB checkpoint.
- A failed worker can resume without duplicating immutable Git observations.
- An operator can replay one projection—for example extraction or embedding—without replaying source discovery.
- Replaying a projection produces new versioned derived records rather than overwriting prior evidence.
- The operator can inspect the source version, extractor/model version, checkpoint, status, last success time, and latest error for each projection.
- A projection can be marked stale after an extractor, parser, chunker, or index schema changes.

## Notes

**As an operator,** I want ingestion and derived projections to resume safely after interruption or be replayed deliberately.
