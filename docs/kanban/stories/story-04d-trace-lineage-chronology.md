---
id: "01900d7c-7f3a-7e8b-9c4d-000000001404"
title: "ENG-004D: Trace a lineage chronology (`ep trace`)"
status: review
type: "story"
priority: "P1"
phase: 1
epic: "01900d7c-7f3a-7e8b-9c4d-000000000004"
design: "docs/kanban/epics/epic-04-temporal-idea-lineage.md"
points: 4
labels: ["phase-1", "timeline", "lineage", "evidence"]
category: "stories"
dependency: ["01900d7c-7f3a-7e8b-9c4d-000000001403"]
---
# ENG-004D: Trace a lineage chronology (`ep trace`)

From a selected section, walk the dated chain of path history, relocations, accepted edges, and provisional candidates.

## Acceptance criteria

- Every edge carries a visible status: observed, accepted, provisional, or rejected/audit-only.
- The user can filter to observed facts only, and include/exclude provisional candidates.
- Every node resolves to the evidence reader.
- A candidate relation is never presented as established history without its status.

---
AUDIT 2026-07-12 (inbox-synthesis session): status=done graded F. Headline deliverable 'ep trace' does not exist — CLI dispatch (src/epiphany/infra/main.clj:539-545) handles only register/search/status/serve. Domain logic (lineage_trace.clj) exists; the declared scope (CLI command) does not. No completion-evidence comment was recorded. Would have been gated by: kanban review-state completion-evidence rule (docs/process/kanban.md), ENG-017G command-contract parity, ADR-004 rule 7 (CI evidence over agent claims). Demoting done->review. --tasks-dir docs/kanban
---
