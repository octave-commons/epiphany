---
id: "01900d7c-7f3a-7e8b-9c4d-000000001205"
title: "ENG-002E: Propose path-repurpose boundaries from continuity signals"
status: ready
type: "story"
priority: "P1"
phase: 1
epic: "01900d7c-7f3a-7e8b-9c4d-000000000002"
design: "docs/kanban/epics/epic-02-markdown-evidence-extraction.md"
points: 3
labels: [phase-1, boundaries, epochs, review]
category: "stories"
dependency: ["01900d7c-7f3a-7e8b-9c4d-000000001204"]
---
# ENG-002E: Propose path-repurpose boundaries from continuity signals

Emit proposed epoch boundaries when the boundary model exceeds its threshold; keep gradual drift visible without hard boundaries.

## Acceptance criteria

- A proposal records the transition, raw signals, score, threshold, and model version.
- A long time gap alone never creates a boundary; low continuity alone displays as drift, not a proposal.
- Accepting/rejecting a boundary is a durable event; rejection hides it from the default view but keeps the audit record.
