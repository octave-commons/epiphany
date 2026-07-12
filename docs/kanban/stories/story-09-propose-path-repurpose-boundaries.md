---
id: "01900d7c-7f3a-7e8b-9c4d-000000001009"
title: "US-009: Propose path-repurpose boundaries"
status: "breakdown"
type: "story"
priority: "P1"
phase: 1
epic: "01900d7c-7f3a-7e8b-9c4d-000000000002"
design: "docs/designs/phase-1-corpus-archaeology.md"
points: 5
labels: [markdown, extraction, boundaries, epochs, provenance, decomposed]
category: "stories"
---

# US-009: Propose path-repurpose boundaries

## Acceptance Criteria

- The system creates a proposed epoch boundary only when the artifact-specific boundary model exceeds its configured threshold.
- A long time gap alone cannot create a boundary.
- A low continuity score alone may be displayed as gradual drift without creating a hard-boundary proposal.
- Each proposal includes the transition, raw signal values, score, threshold, and model version.
- Accepting a boundary creates a durable interpretive decision while retaining all historical same-path observations.
- Rejecting a boundary prevents it from being presented as unresolved in the default interface while preserving its audit record.

## Notes

**As a corpus owner,** I want the system to flag likely purpose changes in a long-lived path without erasing its ordinary history.

## Decomposed into

Product-outcome card; do not implement directly. The engineering slices:

- **ENG-002E** `story-02e-path-repurpose-boundary-proposals.md` — threshold proposals + review events (3)
