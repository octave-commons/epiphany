---
id: "01900d7c-7f3a-7e8b-9c4d-000000001014"
title: "US-014: Trace an idea through time"
status: "breakdown"
type: "story"
priority: "P1"
phase: 1
epic: "01900d7c-7f3a-7e8b-9c4d-000000000004"
design: "docs/notes/design/phase-1-corpus-archaeology.md"
points: 8
labels: [lineage, timeline, temporal, evidence, graph, decomposed]
category: "stories"
---

# US-014: Trace an idea through time

## Acceptance Criteria

- Starting from a selected section, the system returns a chronology containing observed path history, exact relocation/copy observations, accepted relation edges, and provisional candidate edges.
- Every edge has a visual status: observed, accepted, provisional, or rejected/audit-only.
- The timeline can include gradual continuity strength between successive revisions of a persistent path.
- The user can filter the view to observed facts only.
- The user can choose whether provisional candidates are included.
- Every node can open the evidence reader.
- The system never presents a candidate relation as established history without its status and evidence.

## Notes

**As a corpus owner,** I want to start from a current section and traverse related earlier material as a dated evidence chain.

## Decomposed into

Product-outcome card; do not implement directly. The engineering slices:

- **ENG-004D** `story-04d-trace-lineage-chronology.md` — trace chronology + `ep trace` (4)
