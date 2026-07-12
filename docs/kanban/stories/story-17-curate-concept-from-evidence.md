---
id: "01900d7c-7f3a-7e8b-9c4d-000000001017"
title: "US-017: Curate a concept from evidence"
status: "breakdown"
type: "story"
priority: "P1"
phase: 1
epic: "01900d7c-7f3a-7e8b-9c4d-000000000005"
design: "docs/designs/phase-1-corpus-archaeology.md"
points: 3
labels: [review, concepts, curation, evidence, provenance, decomposed]
category: "stories"
---

# US-017: Curate a concept from evidence

## Acceptance Criteria

- A concept is created explicitly by the user from one or more selected section expressions.
- The concept stores name, optional description, creation event, and evidence links.
- The concept may contain accepted and provisional evidence relations, but their status remains visible.
- Removing a section from a concept does not delete the source, candidate, or historical review records.
- A concept can be used as a search/timeline entry point.
- The system distinguishes a user-curated concept from an inferred cluster.

## Notes

**As a corpus owner,** I want to name and curate a concept after reviewing evidence so that an important idea can become a stable navigation object without pretending it was discovered automatically.

## Decomposed into

Product-outcome card; do not implement directly. The engineering slices:

- **ENG-005D** `story-05d-concepts-and-research-questions.md` — concepts + research questions (3)
