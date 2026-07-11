---
id: "01900d7c-7f3a-7e8b-9c4d-000000001012"
title: "US-012: Compare historical expressions"
status: "breakdown"
type: "story"
priority: "P1"
phase: 1
epic: "01900d7c-7f3a-7e8b-9c4d-000000000003"
design: "docs/notes/design/phase-1-corpus-archaeology.md"
points: 5
labels: [search, comparison, diff, evidence, provenance, decomposed]
category: "stories"
---

# US-012: Compare historical expressions

## Acceptance Criteria

- The user can select any two section expressions or revision-at-path observations.
- The view shows exact source metadata for both sides.
- It shows a textual diff or structured comparison appropriate to Markdown.
- It displays continuity signals without conflating them with the comparison itself.
- The user can create a candidate relation or a review decision from the comparison.
- The comparison remains reproducible from recorded source IDs and extractor/model versions.

## Notes

**As a corpus owner,** I want to compare two historical sections or revisions side by side so that I can see whether an apparent continuity, contradiction, or rewrite is real.

## Decomposed into

Product-outcome card; do not implement directly. The engineering slices:

- **ENG-004B** `story-04b-compare-expressions.md` — comparison + `ep diff` (3)
