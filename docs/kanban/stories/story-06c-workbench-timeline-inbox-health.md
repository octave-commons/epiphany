---
id: "01900d7c-7f3a-7e8b-9c4d-000000001603"
title: "ENG-006C: Ship the workbench: timeline, inbox, corpus health"
status: review
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

---
AUDIT 2026-07-12: status=done graded F. Observed: lineage timeline and candidates/decisions queries are placeholders returning empty results (workbench.clj:352 'For now return placeholder', :456 same). Acceptance criteria 'timeline edges visually distinguished by status' and 'inbox triage records rationale with each decision' cannot be true over placeholder data. workbench_test.clj asserts the placeholder itself ('HTMX timeline with empty path shows placeholder') — tests written to the stub, not the contract; empty placeholder results are indistinguishable from 'no data', violating the charter's unavailable-vs-empty rule. No completion evidence recorded. Would have been gated by: ENG-017I (epistemic laws: empty/unavailable/not-implemented distinct), ENG-017F (decode/import integrity), ADR-004 adversarial class 'placeholder query must report unavailable'. Demoting done->review. --tasks-dir docs/kanban
---
