---
id: "01900d7c-7f3a-7e8b-9c4d-000000001009"
title: "US-009: Propose path-repurpose boundaries"
status: "incoming"
type: "story"
priority: "P1"
phase: 1
epic: "01900d7c-7f3a-7e8b-9c4d-000000000002"
design: "docs/notes/design/phase-1-corpus-archaeology.md"
points: 1
labels: [markdown, extraction, boundaries, epochs, provenance]
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
