---
id: "01900d7c-7f3a-7e8b-9c4d-000000001015"
title: "US-015: Identify research gaps"
status: "breakdown"
type: "story"
priority: "P1"
phase: 1
epic: "01900d7c-7f3a-7e8b-9c4d-000000000004"
design: "docs/notes/design/phase-1-corpus-archaeology.md"
points: 5
labels: [lineage, research, gaps, contradiction, questions, decomposed]
category: "stories"
---

# US-015: Identify research gaps

## Acceptance Criteria

- A lineage/timeline view can identify:
  - unresolved contradiction candidates;
  - recurring open questions or TODO markers;
  - abrupt low-continuity transitions;
  - isolated later claims without earlier evidence;
  - repeated near-duplicate notes that may need synthesis.
- Each suggested gap links to exact evidence, not only an LLM-generated statement.
- The system can create a user-owned research-question record from selected evidence.
- The question record does not claim the gap is objectively unresolved; it records the user's interpretation and linked evidence.
- No external-data acquisition occurs in Phase 1.

## Notes

**As a corpus owner,** I want the system to show where a line of thought became unresolved, contradictory, or weakly supported so that I can decide what to research next.

## Decomposed into

Product-outcome card; do not implement directly. The engineering slices:

- **ENG-005E** `story-05e-research-gap-surfacing.md` — gap heuristics over trace/review (3)
