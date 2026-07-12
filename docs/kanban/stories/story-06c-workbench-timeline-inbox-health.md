---
id: "01900d7c-7f3a-7e8b-9c4d-000000001603"
title: "ENG-006C: Ship the workbench: timeline, inbox, corpus health"
status: "done"
type: "story"
priority: "P1"
phase: 1
epic: "01900d7c-7f3a-7e8b-9c4d-000000000006"
design: "docs/kanban/epics/epic-06-temporal-research-workbench.md"
points: 5
labels: ["phase-1", "workbench", "ui", "timeline", "review"]
category: "stories"
dependency: ["01900d7c-7f3a-7e8b-9c4d-000000001602", "01900d7c-7f3a-7e8b-9c4d-000000001404", "01900d7c-7f3a-7e8b-9c4d-000000001502"]
---

# ENG-006C: Ship the workbench: timeline, inbox, corpus health

Remaining phase-1 screens: lineage timeline, candidate review inbox, ingestion/projection status panel.

## Acceptance criteria

- Timeline edges are visually distinguished by status; every node opens the evidence drawer.
- Inbox triage is keyboard-efficient and records rationale with each decision.
- The health panel shows unparsed revisions, extraction errors, index/projection versions and lag, and failures — from the same status queries as `ep status`.
- Views stay usable on a large corpus via paging/progressive disclosure — never render everything.
